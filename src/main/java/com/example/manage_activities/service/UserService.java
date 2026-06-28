package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ChangePasswordRequest;
import com.example.manage_activities.dto.request.UserCreateRequest;
import com.example.manage_activities.dto.request.UserIdentityRequest;
import com.example.manage_activities.dto.request.UserUpdateRequest;
import com.example.manage_activities.dto.response.BulkImportResponse;
import com.example.manage_activities.dto.response.ProfileResponse;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.entity.Profile;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.enums.UserAccountStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.UserMapper;
import com.example.manage_activities.repository.ProfileRepository;
import com.example.manage_activities.repository.UserRepository;
import com.example.manage_activities.security.RoleAssignmentPolicy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService;
    private final RoleAssignmentPolicy roleAssignmentPolicy;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        roleAssignmentPolicy.assertCanManageUsers();

        Integer roleId = request.getRoleId() != null ? request.getRoleId() : Roles.STUDENT.getId();
        roleAssignmentPolicy.assertCanAssignRole(roleId);
        validateRoleId(roleId);
        validateIdentity(roleId, request.getIdentity());

        log.info("Creating new user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (StringUtils.hasText(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User user = userMapper.toEntity(request);
        user.setId(generateUserId());
        user.setRoleId(roleId);
        user.setStatus(resolveStatus(request.getStatus()));
        user.setCreatedAt(LocalDateTime.now());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        Profile profile = createProfileIfRequired(savedUser.getId(), Roles.fromId(roleId), request.getIdentity());

        systemLogService.logAction(getCurrentUserId(), "CREATE_USER", "users", null,
                "userId=" + savedUser.getId() + ", roleId=" + savedUser.getRoleId());
        log.info("User created successfully with ID: {}", savedUser.getId());

        return toUserResponse(savedUser, profile);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        roleAssignmentPolicy.assertCanManageUsers();
        roleAssignmentPolicy.assertCanManageUser(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return toUserResponse(user, profileRepository.findByUserId(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(Integer roleId, String status, String q) {
        roleAssignmentPolicy.assertCanManageUsers();
        log.info("Searching users roleId={}, status={}, q={}", roleId, status, q);

        return userRepository.searchUsers(roleId, normalizeFilter(status), normalizeFilter(q))
                .stream()
                .map(user -> toUserResponse(user, profileRepository.findByUserId(user.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return searchUsers(null, null, null);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> toUserResponse(user, profileRepository.findByUserId(user.getId())));
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> toUserResponse(user, profileRepository.findByUserId(user.getId())));
    }

    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        roleAssignmentPolicy.assertCanManageUser(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new AppException(ErrorCode.EMAIL_EXISTED);
            }
        }

        if (request.getRoleId() != null && !request.getRoleId().equals(user.getRoleId())) {
            roleAssignmentPolicy.assertCanAssignRole(request.getRoleId());
            validateRoleId(request.getRoleId());
        }

        userMapper.updateEntity(user, request);
        if (request.getStatus() != null) {
            user.setStatus(UserAccountStatus.fromValue(request.getStatus()).getValue());
        }

        User updatedUser = userRepository.save(user);
        systemLogService.logAction(getCurrentUserId(), "UPDATE_USER", "users",
                "userId=" + id,
                "email=" + updatedUser.getEmail() + ", status=" + updatedUser.getStatus()
                        + ", roleId=" + updatedUser.getRoleId());
        log.info("User updated successfully with ID: {}", updatedUser.getId());

        return toUserResponse(updatedUser, profileRepository.findByUserId(id));
    }

    @Transactional
    public UserResponse assignPrimaryRole(String userId, Integer roleId) {
        validateRoleId(roleId);
        roleAssignmentPolicy.assertCanManageUser(userId);
        roleAssignmentPolicy.assertCanAssignRole(roleId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Integer oldRoleId = user.getRoleId();

        user.setRoleId(roleId);
        User savedUser = userRepository.save(user);
        systemLogService.logAction(getCurrentUserId(), "ASSIGN_PRIMARY_ROLE", "users",
                "userId=" + userId + ", roleId=" + oldRoleId,
                "userId=" + userId + ", roleId=" + roleId);
        return toUserResponse(savedUser, profileRepository.findByUserId(userId));
    }

    @Transactional
    public UserResponse revokePrimaryRole(String userId) {
        roleAssignmentPolicy.assertCanManageUser(userId);
        roleAssignmentPolicy.assertCanAssignRole(Roles.STUDENT.getId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Integer oldRoleId = user.getRoleId();

        user.setRoleId(Roles.STUDENT.getId());
        User savedUser = userRepository.save(user);
        systemLogService.logAction(getCurrentUserId(), "REVOKE_PRIMARY_ROLE", "users",
                "userId=" + userId + ", roleId=" + oldRoleId,
                "userId=" + userId + ", roleId=" + Roles.STUDENT.getId());
        return toUserResponse(savedUser, profileRepository.findByUserId(userId));
    }

    @Transactional
    public void deactivateUser(String id) {
        roleAssignmentPolicy.assertCanManageUser(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (UserAccountStatus.INACTIVE.getValue().equalsIgnoreCase(user.getStatus())) {
            log.info("User {} is already inactive", id);
            return;
        }

        String oldStatus = user.getStatus();
        user.setStatus(UserAccountStatus.INACTIVE.getValue());
        userRepository.save(user);

        systemLogService.logAction(getCurrentUserId(), "DEACTIVATE_USER", "users",
                "userId=" + id + ", status=" + oldStatus,
                "userId=" + id + ", status=" + UserAccountStatus.INACTIVE.getValue());
        log.info("User deactivated successfully with ID: {}", id);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        systemLogService.logAction(userId, "CHANGE_PASSWORD", "users", "userId=" + userId, "passwordChanged=true");
        log.info("Password changed successfully for user: {}", userId);
    }

    public BulkImportResponse bulkImportFromCsv(InputStream csvStream) {
        roleAssignmentPolicy.assertCanManageUsers();

        List<BulkImportResponse.FailedRow> failures = new ArrayList<>();
        List<String> createdUserIds = new ArrayList<>();
        int total = 0;
        int success = 0;

        try (com.opencsv.CSVReader reader = new com.opencsv.CSVReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();
            if (header == null || header.length == 0) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                colIndex.put(header[i].trim(), i);
            }
            Integer usernameIdx = requireColumn(colIndex, "username", failures);
            Integer passwordIdx = requireColumn(colIndex, "password", failures);
            Integer emailIdx = colIndex.get("email");
            Integer fullNameIdx = colIndex.get("fullName");
            Integer studentCodeIdx = colIndex.get("studentCode");
            Integer classNameIdx = colIndex.get("className");
            Integer departmentIdx = colIndex.get("department");
            Integer phoneIdx = colIndex.get("phone");
            Integer roleIdx = requireColumn(colIndex, "role", failures);
            if (usernameIdx == null || passwordIdx == null || roleIdx == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }

            String[] rawRow;
            int lineNo = 1;
            while ((rawRow = reader.readNext()) != null) {
                lineNo++;
                boolean empty = true;
                for (String s : rawRow) { if (s != null && !s.isBlank()) { empty = false; break; } }
                if (empty) continue;
                String[] row = new String[header.length];
                System.arraycopy(rawRow, 0, row, 0, Math.min(rawRow.length, header.length));
                total++;
                String username = safeGet(row, usernameIdx);
                String password = safeGet(row, passwordIdx);
                String email = emailIdx != null ? safeGet(row, emailIdx) : null;
                String fullName = fullNameIdx != null ? safeGet(row, fullNameIdx) : null;
                String studentCode = studentCodeIdx != null ? safeGet(row, studentCodeIdx) : null;
                String className = classNameIdx != null ? safeGet(row, classNameIdx) : null;
                String department = departmentIdx != null ? safeGet(row, departmentIdx) : null;
                String phone = phoneIdx != null ? safeGet(row, phoneIdx) : null;
                String roleRaw = safeGet(row, roleIdx);
                try {
                    UserIdentityRequest.UserIdentityRequestBuilder idBuilder = UserIdentityRequest.builder()
                            .fullName(fullName)
                            .studentCode(studentCode)
                            .className(className)
                            .department(department)
                            .phone(phone);
                    UserCreateRequest req = UserCreateRequest.builder()
                            .username(username)
                            .password(password)
                            .email(email)
                            .roleId(resolveRoleId(roleRaw))
                            .identity(idBuilder.build())
                            .build();
                    UserResponse resp = createUserInNewTx(req);
                    createdUserIds.add(resp.getId());
                    success++;
                } catch (Exception ex) {
                    log.warn("Bulk import row {} failed: {}", lineNo, ex.getMessage());
                    failures.add(BulkImportResponse.FailedRow.builder()
                            .rowNumber(lineNo)
                            .username(username != null ? username : "")
                            .reason(extractReason(ex))
                            .build());
                }
            }
        } catch (java.io.IOException | com.opencsv.exceptions.CsvValidationException e) {
            log.error("Failed to read CSV stream", e);
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        systemLogService.logAction(getCurrentUserId(), "BULK_IMPORT_USERS", "users",
                "total=" + total,
                "success=" + success + ", failed=" + failures.size());

        return BulkImportResponse.builder()
                .totalRows(total)
                .successCount(success)
                .failureCount(failures.size())
                .createdUserIds(createdUserIds)
                .failures(failures)
                .build();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public UserResponse createUserInNewTx(UserCreateRequest request) {
        return createUser(request);
    }

    private Integer requireColumn(Map<String, Integer> cols, String name, List<BulkImportResponse.FailedRow> failures) {
        if (!cols.containsKey(name)) {
            log.error("CSV missing required column: {}", name);
            return null;
        }
        return cols.get(name);
    }

    private Map<String, Integer> parseHeader(String headerLine) {
        Map<String, Integer> map = new HashMap<>();
        String[] tokens = headerLine.split(",");
        for (int i = 0; i < tokens.length; i++) {
            map.put(tokens[i].trim(), i);
        }
        return map;
    }

    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }

    private String safeGet(String[] cols, int idx) {
        return (idx >= 0 && idx < cols.length) ? (cols[idx] == null ? null : cols[idx].trim()) : null;
    }

    private Integer resolveRoleId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Roles.STUDENT.getId();
        }
        String trimmed = raw.trim().toUpperCase();
        try {
            return Roles.valueOf(trimmed).getId();
        } catch (IllegalArgumentException ex) {
            try {
                int parsed = Integer.parseInt(trimmed);
                if (parsed >= 1 && parsed <= Roles.values().length) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) { }
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String extractReason(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) return ex.getClass().getSimpleName();
        if (msg.length() > 200) msg = msg.substring(0, 200);
        return msg;
    }

    private void validateIdentity(Integer roleId, UserIdentityRequest identity) {
        Roles role = Roles.fromId(roleId);
        if (role != Roles.STUDENT && role != Roles.ORGANIZER) {
            return;
        }
        if (identity == null) {
            throw new AppException(ErrorCode.USER_IDENTITY_REQUIRED);
        }
        if (role == Roles.STUDENT) {
            if (!StringUtils.hasText(identity.getStudentCode())
                    || !StringUtils.hasText(identity.getClassName())
                    || !StringUtils.hasText(identity.getDepartment())) {
                throw new AppException(ErrorCode.USER_IDENTITY_REQUIRED);
            }
        }
        if (role == Roles.ORGANIZER) {
            if (!StringUtils.hasText(identity.getFullName())
                    || !StringUtils.hasText(identity.getDepartment())) {
                throw new AppException(ErrorCode.USER_IDENTITY_REQUIRED);
            }
        }
    }

    private Profile createProfileIfRequired(String userId, Roles role, UserIdentityRequest identity) {
        if (role != Roles.STUDENT && role != Roles.ORGANIZER) {
            return null;
        }
        if (profileRepository.findByUserId(userId) != null) {
            throw new AppException(ErrorCode.EXIST_PROFILE);
        }
        if (role == Roles.STUDENT && profileRepository.existsByStudentCode(identity.getStudentCode())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Profile profile = Profile.builder()
                .id(generateProfileId())
                .userId(userId)
                .fullName(StringUtils.hasText(identity.getFullName())
                        ? identity.getFullName()
                        : identity.getStudentCode())
                .studentCode(role == Roles.STUDENT ? identity.getStudentCode() : null)
                .className(role == Roles.STUDENT ? identity.getClassName() : null)
                .department(identity.getDepartment())
                .phone(identity.getPhone())
                .build();
        return profileRepository.save(profile);
    }

    private UserResponse toUserResponse(User user, Profile profile) {
        UserResponse response = userMapper.toDTO(user);
        Roles role = Roles.fromId(user.getRoleId());
        response.setRoleName(role.name());
        response.setRoleDisplayName(role.getDisplayNameVi());
        if (profile != null) {
            response.setProfile(toProfileResponse(profile));
        }
        return response;
    }

    private ProfileResponse toProfileResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .studentCode(profile.getStudentCode())
                .className(profile.getClassName())
                .department(profile.getDepartment())
                .phone(profile.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }

    private String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return UserAccountStatus.ACTIVE.getValue();
        }
        return UserAccountStatus.fromValue(status).getValue();
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateRoleId(Integer roleId) {
        if (roleId == null || roleId < 1 || roleId > Roles.values().length) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String generateUserId() {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } while (userRepository.existsById(id));
        return id;
    }

    private String generateProfileId() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        while (profileRepository.existsById(id)) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        }
        return id;
    }

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
