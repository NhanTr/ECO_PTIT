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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
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
            userRepository.saveAll(payload.users());
            activityRepository.saveAll(payload.activities());
            activityFileRepository.saveAll(payload.activityFiles());
            registrationRepository.saveAll(payload.registrations());
            attendanceRepository.saveAll(payload.attendances());
            notificationRepository.saveAll(payload.notifications());
            profileRepository.saveAll(payload.profiles());
            roleRepository.saveAll(payload.roles());
            systemConfigRepository.saveAll(payload.systemConfigs());
            systemLogRepository.saveAll(payload.systemLogs());
            systemLogService.logAction(getCurrentUserId(), "RESTORE_BACKUP", "backup", null, fileName);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot restore backup", exception);
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

            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(backupPath))) {
                zip.putNextEntry(new ZipEntry(BACKUP_ENTRY));
                objectMapper.writeValue(zip, payload);
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
