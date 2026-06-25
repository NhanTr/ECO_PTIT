# Hệ thống Phân Quyền (Authorization) - Manage Activities Backend

## Tổng Quan

Dự án sử dụng **Role-Based Access Control (RBAC)** với JWT tokens để phân quyền. Roles được lưu trong JWT claims và được kiểm tra tại 2 cấp độ:
1. **URL-level**: SecurityFilterChain (SecurityConfig.java)
2. **Method-level**: @PreAuthorize annotations (Controllers)

---

## Các Roles Hiện Có

```java
public enum Roles {
    ADMIN,        // ID: 1 - Quản trị hệ thống
    MANAGER,      // ID: 2 - Giảng viên / Quản lý hoạt động
    ORGANIZER,    // ID: 3 - BTC/CLB
    STUDENT       // ID: 4 - Sinh viên
}
```

**Access token TTL:** mặc định **900 giây (15 phút)** (`jwt.valid-duration`) để giảm cửa sổ JWT mang role cũ sau khi admin đổi quyền. Refresh token vẫn dài hơn (7 ngày).

---

## Luồng Phân Quyền

### 1. Tạo Token (generateToken)
```
User đăng nhập
    ↓
AuthenticationService.authenticate()
    ↓
JwtUtil.generateToken(user)
    ├─ Lấy user.getRoleId()
    ├─ Convert RoleId → RoleName (buildScopes)
    └─ Lưu vào JWT claim: "scopes": "ADMIN"
    ↓
Gửi token về client
```

### 2. Xác Thực Token (verifyToken)
```
Client gửi request kèm: Authorization: Bearer <token>
    ↓
SecurityFilterChain - oauth2ResourceServer filter
    ├─ Decode JWT token
    ├─ Extract "scopes" claim = "ADMIN"
    ├─ JwtAuthenticationConverter
    │  └─ Thêm prefix: "ADMIN" → "ROLE_ADMIN"
    └─ Lưu vào SecurityContext
    ↓
SecurityContextHolder.getContext().getAuthentication()
    ├─ getName() = userId
    └─ getAuthorities() = [ROLE_ADMIN]
```

### 3. Kiểm Tra Quyền (Authorization)
```
@PreAuthorize("hasRole('ADMIN')")
    ↓
Spring kiểm tra: "ROLE_ADMIN" có trong authorities không?
    ├─ Có → Allow ✓
    └─ Không → Deny (403 Forbidden) ✗
```

---

## Quy Tắc Phân Quyền Cho Từng Endpoint

### **Authentication Endpoints** (`/auth/**`)
- **Public** - Không yêu cầu authentication
- **Endpoints:**
  - `POST /auth/token` - Đăng nhập
  - `POST /auth/introspect` - Kiểm tra token
  - `POST /auth/refresh` - Làm mới token
  - `POST /auth/logout` - Đăng xuất

---

### **Activity Endpoints** (`/api/v1/activities/**`)

| Endpoint | Method | Quyền | Mô Tả |
|----------|--------|-------|-------|
| `/api/v1/activities` | GET | `Public` | Xem tất cả hoạt động |
| `/api/v1/activities/{id}` | GET | `Public` | Xem chi tiết hoạt động |
| `/api/v1/activities/organizer/{id}` | GET | `Public` | Xem hoạt động theo tổ chức |
| `/api/v1/activities/status/{status}` | GET | `Public` | Xem hoạt động theo trạng thái |
| `/api/v1/activities` | POST | `ORGANIZER, ADMIN` | Tạo hoạt động mới |
| `/api/v1/activities/{id}` | PUT | `ORGANIZER, ADMIN` | Chỉnh sửa hoạt động |
| `/api/v1/activities/{id}` | DELETE | `ORGANIZER, ADMIN` | Xóa hoạt động |

**Ghi chú:**
- GET endpoints public để cho phép mọi người xem
- POST/PUT/DELETE yêu cầu `ORGANIZER` hoặc `ADMIN`
- ORGANIZER có thể chỉnh sửa hoạt động của mình (logic: kiểm tra organizerId)

---

### **User Endpoints** (`/api/v1/users/**`)

| Endpoint | Method | Quyền | Mô Tả |
|----------|--------|-------|-------|
| `/api/v1/users` | GET | `ADMIN` | Xem tất cả users |
| `/api/v1/users/{id}` | GET | `ADMIN` | Xem chi tiết user |
| `/api/v1/users` | POST | `ADMIN` | Tạo user mới |
| `/api/v1/users/{id}` | PUT | `ADMIN` | Chỉnh sửa user |
| `/api/v1/users/{id}` | DELETE | `ADMIN` | Xóa user |
| `/api/v1/users/change-password` | POST | `Authenticated` | Đổi mật khẩu (user của mình) |

**Ghi chú:**
- Chỉ `ADMIN` có thể quản lý users
- Bất kỳ user nào đã xác thực cũng có thể đổi mật khẩu của mình

---

### **Registration Endpoints** (`/api/v1/registrations/**`)

| Endpoint | Method | Quyền | Mô Tả |
|----------|--------|-------|-------|
| `/api/v1/registrations` | POST | `Authenticated` | Đăng ký tham gia hoạt động |
| `/api/v1/registrations/{id}` | DELETE | `Authenticated` | Hủy đăng ký |
| `/api/v1/registrations/**` | GET | `Authenticated` | Xem đăng ký |

**Ghi chú:**
- Tất cả users đã xác thực có thể đăng ký tham gia
- Tại service layer, kiểm tra user chỉ có thể xem/xóa đăng ký của mình

---

## Cấu Hình Hiện Tại

### SecurityConfig.java
```java
httpSecurity.authorizeHttpRequests(request -> request
    // Public
    .requestMatchers("/auth/**").permitAll()
    .requestMatchers("GET", "/api/v1/activities/**").permitAll()
    
    // User management - ADMIN only
    .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
    
    // Activity management
    .requestMatchers("POST", "/api/v1/activities").hasAnyRole("ORGANIZER", "ADMIN")
    .requestMatchers(PUT", "/api/v1/activities/**").hasAnyRole("ORGANIZER", "ADMIN")
    .requestMatchers("DELETE", "/api/v1/activities/**").hasAnyRole("ORGANIZER", "ADMIN")
    
    // Registration - authenticated users
    .requestMatchers("/api/v1/registrations/**").authenticated()
    
    // All other requests require authentication
    .anyRequest().authenticated()
);
```

### Controller Annotations
```java
// UserController
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public APIResponse<List<UserResponse>> getAllUsers() { ... }

// ActivityController
@PostMapping
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public ResponseEntity<ActivityResponse> createActivity(...) { ... }
```

---

## Các SpEL Expressions Phổ Biến

```java
// Kiểm tra role (option 1)
@PreAuthorize("hasRole('ADMIN')")  // Tìm ROLE_ADMIN

// Kiểm tra role (option 2)
@PreAuthorize("hasAuthority('ROLE_ADMIN')")

// Kiểm tra nhiều roles
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")

// Kiểm tra permission
@PreAuthorize("hasAuthority('READ')")

// Kiểm tra authentication
@PreAuthorize("isAuthenticated()")
@PreAuthorize("permitAll()")
@PreAuthorize("denyAll()")

// Custom conditions
@PreAuthorize("#userId == authentication.name")  // Chỉ user của mình
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
```

---

## Testing Authorization

### 1. Get Token (ADMIN)
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_user","password":"password"}'

# Response:
{
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "authenticated": true
}
```

### 2. Access Protected Endpoint
```bash
# Access as ADMIN - OK ✓
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <token>"

# Response: 200 OK with user list

# Access as STUDENT - FAIL ✗
# Response: 403 Forbidden
```

### 3. Test Different Roles
```bash
# Get ORGANIZER token
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"organizer_user","password":"password"}'

# Create activity - OK ✓
curl -X POST http://localhost:8080/api/v1/activities \
  -H "Authorization: Bearer <organizer_token>" \
  -H "Content-Type: application/json" \
  -d '{...activity data...}'
```

---

## Best Practices

1. **URL-level**: Cho quy tắc đơn giản, toàn cục
2. **Method-level (@PreAuthorize)**: Cho logic phức tạp, endpoint cụ thể
3. **Service-level**: Kiểm tra ownership (user chỉ xem được dữ liệu của mình)
4. **Logging**: Ghi log các quyết định authorize

---

## Future Enhancements

- [ ] Custom permission system (e.g., "CAN_EDIT_ACTIVITY")
- [ ] Role hierarchy (ADMIN > ORGANIZER > STUDENT)
- [ ] Row-level security (user chỉ xem dữ liệu liên quan)
- [ ] Audit logging cho authorization decisions
