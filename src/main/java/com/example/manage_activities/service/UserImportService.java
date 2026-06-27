package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.BulkUserImportResponse;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.entity.Profile;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.enums.UserAccountStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.ProfileRepository;
import com.example.manage_activities.repository.UserRepository;
import com.example.manage_activities.security.RoleAssignmentPolicy;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Imports a batch of users from a CSV file (multipart upload).
 *
 * Expected CSV header (case-insensitive, order-independent):
 *   username,password,email,role,fullName,studentCode,className,department,phone,status
 *
 * Each row is processed in its own transaction so a single bad row does not abort the
 * whole import. Failures are collected and returned together with the success summary.
 *
 * Only ADMIN may call this service — the role check lives in {@link RoleAssignmentPolicy}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportService {

    private static final int MAX_ROWS = 1000;

    private static final List<String> REQUIRED_HEADERS = List.of(
            "username", "password", "role");

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleAssignmentPolicy roleAssignmentPolicy;
    private final SystemLogService systemLogService;

    public BulkUserImportResponse importUsers(MultipartFile file) {
        roleAssignmentPolicy.assertCanManageUsers();
        Roles caller = roleAssignmentPolicy.getCallerRole();
        if (caller != Roles.ADMIN) {
            // QTHT only — Manager is not allowed to bulk-import.
            throw new AppException(ErrorCode.ROLE_ASSIGNMENT_FORBIDDEN);
        }

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<BulkUserImportResponse.RowError> errors = new ArrayList<>();
        List<String> createdUsernames = new ArrayList<>();
        int total = 0;
        int success = 0;

        try (CSVReader reader = new CSVReader(
                new Utf8InputStreamReader(file.getInputStream()))) {

            String[] header = reader.readNext();
            if (header == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            validateHeader(header);

            String[] row;
            int rowNumber = 1; // header is row 1
            while ((row = reader.readNext()) != null) {
                rowNumber++;
                total++;
                if (isBlankRow(row)) {
                    continue;
                }
                if (total > MAX_ROWS) {
                    errors.add(BulkUserImportResponse.RowError.builder()
                            .rowNumber(rowNumber)
                            .username("")
                            .reason("Vượt quá giới hạn " + MAX_ROWS + " dòng / lần import.")
                            .build());
                    break;
                }

                ImportedRow parsed;
                try {
                    parsed = parseRow(header, row, rowNumber);
                } catch (AppException ex) {
                    errors.add(BulkUserImportResponse.RowError.builder()
                            .rowNumber(rowNumber)
                            .username("")
                            .reason(ex.getErrorCode().getMessage())
                            .build());
                    continue;
                }

                try {
                    createSingleUser(parsed);
                    success++;
                    createdUsernames.add(parsed.username);
                } catch (AppException ex) {
                    errors.add(BulkUserImportResponse.RowError.builder()
                            .rowNumber(rowNumber)
                            .username(parsed.username)
                            .reason(ex.getErrorCode().getMessage())
                            .build());
                } catch (RuntimeException ex) {
                    log.error("Unexpected error importing row {} (username={})", rowNumber, parsed.username, ex);
                    errors.add(BulkUserImportResponse.RowError.builder()
                            .rowNumber(rowNumber)
                            .username(parsed.username)
                            .reason("Lỗi không xác định: " + ex.getMessage())
                            .build());
                }
            }
        } catch (IOException | CsvValidationException ex) {
            log.error("CSV import failed", ex);
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        systemLogService.logAction(getCurrentUserId(), "BULK_IMPORT_USERS", "users",
                null,
                "total=" + total + ", success=" + success + ", failed=" + errors.size());

        return BulkUserImportResponse.builder()
                .totalRows(total)
                .successCount(success)
                .failureCount(errors.size())
                .errors(errors)
                .createdUsernames(createdUsernames)
                .build();
    }

    /**
     * Creates a single user + profile in its own transaction so the surrounding batch
     * is not poisoned by a runtime failure on this row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSingleUser(ImportedRow row) {
        if (userRepository.existsByUsername(row.username)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (StringUtils.hasText(row.email) && userRepository.existsByEmail(row.email)) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User user = User.builder()
                .id(generateUserId())
                .username(row.username)
                .email(StringUtils.hasText(row.email) ? row.email : null)
                .passwordHash(passwordEncoder.encode(row.password))
                .roleId(row.roleId)
                .status(StringUtils.hasText(row.status) ? row.status : UserAccountStatus.ACTIVE.getValue())
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);

        Roles role = Roles.fromId(row.roleId);
        if ((role == Roles.STUDENT || role == Roles.ORGANIZER)
                && (StringUtils.hasText(row.fullName)
                    || StringUtils.hasText(row.studentCode)
                    || StringUtils.hasText(row.className)
                    || StringUtils.hasText(row.department)
                    || StringUtils.hasText(row.phone))) {
            createImportedProfile(saved.getId(), role, row);
        }
    }

    private void createImportedProfile(String userId, Roles role, ImportedRow row) {
        if (profileRepository.findByUserId(userId) != null) {
            throw new AppException(ErrorCode.EXIST_PROFILE);
        }
        if (role == Roles.STUDENT && StringUtils.hasText(row.studentCode)
                && profileRepository.existsByStudentCode(row.studentCode)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Profile profile = Profile.builder()
                .id(UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .userId(userId)
                .fullName(StringUtils.hasText(row.fullName) ? row.fullName : row.username)
                .studentCode(role == Roles.STUDENT ? row.studentCode : null)
                .className(role == Roles.STUDENT ? row.className : null)
                .department(StringUtils.hasText(row.department) ? row.department : null)
                .phone(StringUtils.hasText(row.phone) ? row.phone : null)
                .build();
        profileRepository.save(profile);
    }

    public void writeTemplate(OutputStream out) throws IOException {
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVWriter csv = new CSVWriter(writer)) {
            csv.writeNext(new String[]{
                    "username", "password", "email", "role",
                    "fullName", "studentCode", "className", "department", "phone", "status"
            });
            csv.writeNext(new String[]{
                    "sv001", "Matkhau123", "sv001@ptit.edu.vn", "STUDENT",
                    "Nguyen Van A", "SV001", "D22CNTT01", "CNTT", "0912345678", "active"
            });
            csv.writeNext(new String[]{
                    "org001", "Matkhau123", "org001@ptit.edu.vn", "ORGANIZER",
                    "CLB Tin Hoc", "", "", "CLB", "", "active"
            });
            csv.writeNext(new String[]{
                    "gv001", "Matkhau123", "gv001@ptit.edu.vn", "MANAGER",
                    "Nguyen Thi B", "", "", "", "", "active"
            });
        }
    }

    private void validateHeader(String[] header) {
        List<String> normalized = Arrays.stream(header)
                .map(s -> s == null ? "" : s.trim().toLowerCase())
                .toList();
        for (String required : REQUIRED_HEADERS) {
            if (!normalized.contains(required)) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }
    }

    private ImportedRow parseRow(String[] header, String[] row, int rowNumber) {
        ImportedRow out = new ImportedRow();
        for (int i = 0; i < header.length && i < row.length; i++) {
            String key = header[i] == null ? "" : header[i].trim().toLowerCase();
            String value = row[i] == null ? "" : row[i].trim();
            switch (key) {
                case "username" -> out.username = value;
                case "password" -> out.password = value;
                case "email" -> out.email = value;
                case "role" -> out.roleId = resolveRoleId(value);
                case "fullname" -> out.fullName = value;
                case "studentcode" -> out.studentCode = value;
                case "classname" -> out.className = value;
                case "department" -> out.department = value;
                case "phone" -> out.phone = value;
                case "status" -> out.status = normalizeStatus(value);
                default -> { /* ignore unknown columns */ }
            }
        }

        if (!StringUtils.hasText(out.username)) {
            throw new AppException(ErrorCode.USERNAME_INVALID);
        }
        if (out.username.length() < 3) {
            throw new AppException(ErrorCode.USERNAME_INVALID);
        }
        if (!StringUtils.hasText(out.password) || out.password.length() < 8) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        if (out.roleId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return out;
    }

    private Integer resolveRoleId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        try {
            Roles r = Roles.valueOf(normalized);
            return r.getId();
        } catch (IllegalArgumentException ignored) {
            // allow numeric role id 1..4
            try {
                int n = Integer.parseInt(normalized);
                if (n < 1 || n > Roles.values().length) {
                    return null;
                }
                return n;
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

    private String normalizeStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String n = raw.trim().toLowerCase();
        if ("active".equals(n) || "inactive".equals(n)) {
            return n;
        }
        return null;
    }

    private boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String generateUserId() {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } while (userRepository.existsById(id));
        return id;
    }

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    /** Internal mutable holder for the parsed fields of one CSV row. */
    private static class ImportedRow {
        String username;
        String password;
        String email;
        Integer roleId;
        String fullName;
        String studentCode;
        String className;
        String department;
        String phone;
        String status;
    }

    /**
     * Reader that strips an optional UTF-8 BOM from the start of the stream so the first
     * header cell is not polluted with \uFEFF. Many editors (Notepad, Excel) prepend a BOM
     * when saving UTF-8 CSV files.
     */
    private static class Utf8InputStreamReader extends Reader {
        private final InputStream in;
        private boolean firstByte = true;
        private int pending = -1;

        Utf8InputStreamReader(InputStream in) {
            this.in = in;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = 0;
            while (read < len) {
                int c;
                if (pending != -1) {
                    c = pending;
                    pending = -1;
                } else {
                    int b = in.read();
                    if (b == -1) {
                        break;
                    }
                    if (firstByte) {
                        firstByte = false;
                        if (b == 0xEF) {
                            int b2 = in.read();
                            int b3 = in.read();
                            if (b2 == 0xBB && b3 == 0xBF) {
                                continue;
                            }
                            pending = b3;
                            c = (b2 << 8) | b;
                        } else {
                            c = b;
                        }
                    } else {
                        c = b;
                    }
                }
                cbuf[off + read++] = (char) c;
            }
            return read == 0 ? -1 : read;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
