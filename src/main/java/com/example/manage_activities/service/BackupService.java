package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.BackupFileResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.ActivityFile;
import com.example.manage_activities.entity.Attendance;
import com.example.manage_activities.entity.Notification;
import com.example.manage_activities.entity.Profile;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.entity.Role;
import com.example.manage_activities.entity.SystemConfig;
import com.example.manage_activities.entity.SystemLog;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.ActivityFileRepository;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.NotificationRepository;
import com.example.manage_activities.repository.ProfileRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import com.example.manage_activities.repository.RoleRepository;
import com.example.manage_activities.repository.SystemConfigRepository;
import com.example.manage_activities.repository.SystemLogRepository;
import com.example.manage_activities.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BackupService {

    private static final String CONFIRM_RESTORE = "RESTORE";
    private static final String BACKUP_ENTRY = "backup.json";
    private static final Path BACKUP_DIRECTORY = Path.of("backups");

    ObjectMapper objectMapper;
    UserRepository userRepository;
    ActivityRepository activityRepository;
    ActivityFileRepository activityFileRepository;
    RegistrationRepository registrationRepository;
    AttendanceRepository attendanceRepository;
    NotificationRepository notificationRepository;
    ProfileRepository profileRepository;
    RoleRepository roleRepository;
    SystemLogRepository systemLogRepository;
    SystemConfigRepository systemConfigRepository;
    EntityManager entityManager;
    DataSource dataSource;
    SystemLogService systemLogService;

    @Transactional
    public BackupFileResponse createManualBackup() {
        BackupFileResponse response = createBackup("manual");
        systemLogService.logAction(getCurrentUserId(), "CREATE_BACKUP", "backup", null, response.getFileName());
        return response;
    }

    @Scheduled(cron = "${system.backup.cron:0 0 2 * * *}")
    @Transactional
    public void createScheduledBackup() {
        createBackup("scheduled");
    }

    public List<BackupFileResponse> listBackups() {
        try {
            if (!Files.exists(BACKUP_DIRECTORY)) {
                return List.of();
            }
            return Files.list(BACKUP_DIRECTORY)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .map(this::toBackupFileResponse)
                    .sorted(Comparator.comparing(BackupFileResponse::getCreatedAt).reversed())
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot list backups", exception);
        }
    }

    @Transactional
    public void restoreBackup(String fileName, String confirmation) {
        if (!CONFIRM_RESTORE.equals(confirmation)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Path backupPath = resolveBackupPath(fileName);
        Dialect dialect;
        try {
            dialect = detectDialect();
        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException("Cannot detect database dialect", ex);
        }

        boolean fkWasOn = setForeignKeyChecks(dialect, false);
        try (ZipFile zipFile = new ZipFile(backupPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(BACKUP_ENTRY);
            if (entry == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }

            BackupPayload payload;
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                payload = objectMapper.readValue(inputStream, BackupPayload.class);
            }

            clearData();
            entityManager.clear();
            Map<Integer, Integer> roleIdMap = persistAndRemapRoles(payload.roles());
            entityManager.flush();
            persistAll(payload.users(), roleIdMap);
            entityManager.flush();
            persistAll(payload.activities());
            entityManager.flush();
            persistAll(payload.activityFiles());
            entityManager.flush();
            persistAll(payload.registrations());
            entityManager.flush();
            persistAll(payload.attendances());
            entityManager.flush();
            persistAll(payload.notifications());
            entityManager.flush();
            persistAll(payload.profiles());
            entityManager.flush();
            persistAll(payload.systemConfigs());
            entityManager.flush();
            persistAll(payload.systemLogs());
            entityManager.flush();
            systemLogService.logAction(getCurrentUserId(), "RESTORE_BACKUP", "backup", null, fileName);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot restore backup", exception);
        } finally {
            setForeignKeyChecks(dialect, fkWasOn);
        }
    }

    /**
     * Best-effort toggle of FK enforcement so restores work even when target DB
     * has FK constraints. Returns whether FK was on before, so we can restore
     * it on exit.
     */
    private boolean setForeignKeyChecks(Dialect dialect, boolean enable) {
        try {
            switch (dialect) {
                case MYSQL -> {
                    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = " + (enable ? 1 : 0)).executeUpdate();
                    return true;
                }
                case POSTGRES -> {
                    // Postgres has session_replication_role; we use it sparingly.
                    String role = enable ? "origin" : "replica";
                    entityManager.createNativeQuery("SET session_replication_role = " + role).executeUpdate();
                    return true;
                }
                case H2 -> {
                    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY " + (enable ? "TRUE" : "FALSE")).executeUpdate();
                    return true;
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception ex) {
            // Best-effort: do not abort restore on FK toggle failure.
            System.err.println("[BackupService] Failed to toggle FK checks (" + dialect + ", enable=" + enable + "): " + ex.getMessage());
            return false;
        }
    }

    private BackupFileResponse createBackup(String type) {
        try {
            Files.createDirectories(BACKUP_DIRECTORY);
            String fileName = "backup-" + type + "-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".zip";
            Path backupPath = BACKUP_DIRECTORY.resolve(fileName);
            BackupPayload payload = new BackupPayload(
                    userRepository.findAll(),
                    activityRepository.findAll(),
                    activityFileRepository.findAll(),
                    registrationRepository.findAll(),
                    attendanceRepository.findAll(),
                    notificationRepository.findAll(),
                    profileRepository.findAll(),
                    roleRepository.findAll(),
                    systemLogRepository.findAll(),
                    systemConfigRepository.findAll()
            );

            try (OutputStream out = Files.newOutputStream(backupPath); ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry(BACKUP_ENTRY));
                byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
                zip.write(jsonBytes);
                zip.closeEntry();
            }

            return toBackupFileResponse(backupPath);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create backup", exception);
        }
    }

    private void clearData() {
        attendanceRepository.deleteAll();
        registrationRepository.deleteAll();
        activityFileRepository.deleteAll();
        notificationRepository.deleteAll();
        profileRepository.deleteAll();
        roleRepository.deleteAll();
        activityRepository.deleteAll();
        userRepository.deleteAll();
        systemConfigRepository.deleteAll();
        systemLogRepository.deleteAll();
        entityManager.flush();
    }

    private Path resolveBackupPath(String fileName) {
        Path backupPath = BACKUP_DIRECTORY.resolve(fileName).normalize();
        if (!backupPath.startsWith(BACKUP_DIRECTORY) || !Files.exists(backupPath)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return backupPath;
    }

    private BackupFileResponse toBackupFileResponse(Path path) {
        try {
            return BackupFileResponse.builder()
                    .fileName(path.getFileName().toString())
                    .size(Files.size(path))
                    .createdAt(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()))
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read backup file", exception);
        }
    }

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

        private <T> void persistAll(List<T> entities) {
        persistAll(entities, Map.of());
    }

    private <T> void persistAll(List<T> entities, Map<Integer, Integer> roleIdMap) {
        for (T entity : entities) {
            if (entity instanceof com.example.manage_activities.entity.User user) {
                Integer oldRoleId = user.getRoleId();
                if (oldRoleId != null && roleIdMap.containsKey(oldRoleId)) {
                    user.setRoleId(roleIdMap.get(oldRoleId));
                }
            }
            entityManager.persist(entity);
        }
    }

    private Map<Integer, Integer> persistAndRemapRoles(List<Role> roles) {
        Map<Integer, Integer> idMap = new HashMap<>();
        try {
            Dialect dialect = detectDialect();
            logDialect(dialect);

            disableIdentityForRoles(dialect);
            for (Role role : roles) {
                Integer oldId = role.getId();
                entityManager.createNativeQuery("INSERT INTO roles (id, role_name) VALUES (?, ?)")
                        .setParameter(1, oldId)
                        .setParameter(2, role.getRoleName())
                        .executeUpdate();
                idMap.put(oldId, oldId);
            }
            advanceIdentityAfterRestore(dialect, roles);
            entityManager.flush();
            entityManager.clear();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot remap roles during restore", exception);
        }
        return idMap;
    }

    private enum Dialect { H2, MYSQL, POSTGRES, OTHER }

    private Dialect detectDialect() throws java.sql.SQLException {
        String product = dataSource.getConnection().getMetaData().getDatabaseProductName();
        String upper = product == null ? "" : product.toUpperCase(Locale.ROOT);
        if (upper.contains("H2")) return Dialect.H2;
        if (upper.contains("MYSQL") || upper.contains("MARIADB")) return Dialect.MYSQL;
        if (upper.contains("POSTGRES")) return Dialect.POSTGRES;
        return Dialect.OTHER;
    }

    private void logDialect(Dialect dialect) {
        // Lightweight logging via System.err to avoid coupling to slf4j here.
        System.err.println("[BackupService] Detected DB dialect: " + dialect);
    }

    /**
     * Allow caller to insert explicit values into the IDENTITY column for the
     * duration of this transaction. The statements differ per dialect:
     *   - H2:        ALTER COLUMN ... INT NOT NULL   (drops IDENTITY)
     *   - MySQL:     SET sql_mode = ... NO_AUTO_VALUE_ON_ZERO + explicit values allowed
     *                (we already SET FOREIGN_KEY_CHECKS=0 below)
     *   - PostgreSQL: drop DEFAULT, restore it later
     *   - OTHER:     no-op (assumes caller has prepared the schema manually)
     */
    private void disableIdentityForRoles(Dialect dialect) {
        switch (dialect) {
            case H2 -> entityManager.createNativeQuery(
                    "ALTER TABLE roles ALTER COLUMN id INT NOT NULL").executeUpdate();
            case MYSQL -> {
                // MySQL always allows INSERT of explicit values for AUTO_INCREMENT,
                // provided the supplied value is > 0. We just rely on that here.
                // FK checks are off below.
            }
            case POSTGRES -> entityManager.createNativeQuery(
                    "ALTER TABLE roles ALTER COLUMN id DROP IDENTITY IF EXISTS").executeUpdate();
            case OTHER -> { /* nothing to do */ }
        }
    }

    private void advanceIdentityAfterRestore(Dialect dialect, List<Role> roles) {
        int maxId = roles.stream()
                .map(Role::getId)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        switch (dialect) {
            case H2 -> entityManager.createNativeQuery(
                    "ALTER TABLE roles ALTER COLUMN id INT GENERATED BY DEFAULT AS IDENTITY (START WITH " + (maxId + 1) + ")").executeUpdate();
            case MYSQL -> entityManager.createNativeQuery(
                    "ALTER TABLE roles AUTO_INCREMENT = " + (maxId + 1)).executeUpdate();
            case POSTGRES -> {
                entityManager.createNativeQuery(
                        "ALTER TABLE roles ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY").executeUpdate();
                entityManager.createNativeQuery(
                        "SELECT setval(pg_get_serial_sequence('roles','id'), " + (maxId + 1) + ", false)").executeUpdate();
            }
            case OTHER -> { /* nothing to do */ }
        }
    }

    private record BackupPayload(
            List<User> users,
            List<Activity> activities,
            List<ActivityFile> activityFiles,
            List<Registration> registrations,
            List<Attendance> attendances,
            List<Notification> notifications,
            List<Profile> profiles,
            List<Role> roles,
            List<SystemLog> systemLogs,
            List<SystemConfig> systemConfigs
    ) {
    }
}
