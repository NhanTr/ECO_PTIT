# ECO_PTIT — Hoạt động sinh viên Backend

## Mục lục

- [Tổng quan](#tổng-quan)
- [Kiến trúc](#kiến-trúc)
- [Vai trò người dùng](#vai-trò-người-dùng)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cài đặt & Chạy](#cài-đặt--chạy)
- [Cấu hình](#cấu-hình)
- [Cơ sở dữ liệu](#cơ-sở-dữ-liệu)
- [API Endpoints](#api-endpoints)
- [Bảo mật](#bảo-mật)
- [Kiểm thử](#kiểm-thử)
- [Backup & Restore](#backup--restore)

---

## Tổng quan

**ECO_PTIT** là backend REST API của hệ thống **quản lý hoạt động sinh viên** trường PTIT. Hệ thống cho phép tổ chức, đăng ký, điểm danh và thống kê các hoạt động ngoại khóa cho sinh viên. Backend được viết bằng **Java Spring Boot**, hỗ trợ đa vai trò, JWT authentication và giao tiếp với frontend thông qua REST API.

---

## Kiến trúc

```
Frontend (Next.js / APTIT)
       │
       ▼
  REST API (Spring Boot)
  Port: 8080 (mặc định)
       │
       ▼
  MySQL Database
  Port: 3306
       │
       ▼
  Redis (Refresh Token Cache)
  Port: 6379
```

Hệ thống sử dụng kiến trúc **Layered Architecture**:
- **Controller Layer** — Nhận request từ client, điều hướng đến service tương ứng.
- **Service Layer** — Xử lý logic nghiệp vụ.
- **Repository Layer** — Tương tác với cơ sở dữ liệu qua JPA.
- **Entity Layer** — Định nghĩa các thực thể trong hệ thống.

---

## Vai trò người dùng

| Role ID | Tên vai trò | Mô tả |
|---------|-------------|--------|
| 1 | `ADMIN` | Quản trị hệ thống toàn phần: quản lý người dùng, cấu hình, backup |
| 2 | `MANAGER` | Quản lý cấp khoa: duyệt/từ chối hoạt động, duyệt báo cáo, thống kê |
| 3 | `ORGANIZER` | Người tổ chức: tạo, gửi duyệt, quản lý hoạt động của câu lạc bộ |
| 4 | `STUDENT` | Sinh viên: đăng ký tham gia, xem điểm rèn luyện, điểm danh |

---

## Cấu trúc dự án

```
src/
└── main/
    └── java/
        └── com/example/manage_activities/
            ├── configuration/        # Cấu hình Spring (Security, CORS, Redis, JWT...)
            ├── Controller/            # REST Controllers
            │   ├── AuthenticationController.java
            │   ├── ActivityController.java
            │   ├── AdminUserController.java
            │   ├── AdminActivityController.java
            │   ├── ManagerActivityController.java
            │   ├── RegistrationController.java
            │   ├── AttendanceController.java
            │   ├── NotificationController.java
            │   ├── BackupController.java
            │   └── SystemStatisticsController.java
            ├── dto/
            │   ├── request/           # DTO cho request từ client
            │   └── response/          # DTO cho response về client
            ├── entity/                # JPA Entities (User, Activity, Registration...)
            ├── enums/                 # Enum (ActivityStatus, RegistrationStatus, Roles...)
            ├── exception/             # Xử lý ngoại lệ và mã lỗi
            ├── mapper/                # MapStruct mappers (Entity <-> DTO)
            ├── repository/            # JPA Repositories
            │   └── projection/        # Projection interfaces cho truy vấn
            ├── security/              # Security policies (PermissionCatalog, RoleAssignmentPolicy)
            └── service/                # Business logic services
```

**Các file cấu hình chính:**

| File | Mục đích |
|------|----------|
| `SecurityConfig.java` | Cấu hình Spring Security, phân quyền endpoints |
| `CorsConfig.java` | Cấu hình CORS cho frontend |
| `JwtAuthenticationEntryPoint.java` | Xử lý khi chưa xác thực JWT |
| `RedisConfig.java` | Cấu hình kết nối Redis cho refresh token |
| `DotenvConfig.java` | Load biến môi trường từ `.env` |
| `RoleDataInitializer.java` | Khởi tạo dữ liệu vai trò khi chạy lần đầu |
| `ApplicationInitConfig.java` | Tạo tài khoản admin mặc định |

---

## Công nghệ sử dụng

| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|-----------|
| **Java** | 21 | Ngôn ngữ lập trình chính |
| **Spring Boot** | 3.x | Framework backend |
| **Spring Security** | 6.x | Xác thực & phân quyền |
| **Spring Data JPA** | — | Tương tác cơ sở dữ liệu |
| **MySQL** | 8.0 | Cơ sở dữ liệu chính |
| **Redis** | — | Lưu trữ refresh tokens |
| **MapStruct** | 1.6.3 | Ánh xạ Entity <-> DTO |
| **Lombok** | 1.18.x | Giảm boilerplate code |
| **jjwt** | — | Tạo & xác thực JWT tokens |
| **dotenv-java** | 3.0.0 | Đọc biến môi trường |
| **Maven** | 3.9.12 | Build tool |
| **Docker** | — | Container hóa (MySQL) |

---

## Cài đặt & Chạy

### Yêu cầu

- **Java 21** trở lên
- **Maven 3.8+**
- **MySQL 8.0** (hoặc dùng Docker)
- **Redis** (optional, cho refresh token)

### Cách 1: Dùng Docker cho MySQL

```bash
# Chạy MySQL bằng Docker Compose
docker-compose up -d
```

### Cách 2: MySQL cục bộ

Tạo database `my_app_db`:

```sql
CREATE DATABASE my_app_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Chạy ứng dụng

```bash
# Cài đặt dependencies và build
./mvnw clean install

# Chạy ứng dụng
./mvnw spring-boot:run
```

Hoặc chạy trực tiếp:

```bash
java -jar target/manage_activities-*.jar
```

### Tài khoản mặc định

- **Username:** `admin`
- **Password:** `admin123`

> **Lưu ý:** Tài khoản admin mặc định chỉ được tạo khi database chưa có user nào. Trong production, **bắt buộc** đổi mật khẩu sau lần đăng nhập đầu tiên.

---

## Cấu hình

Tạo file `.env` trong thư mục gốc của dự án:

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/my_app_db
DB_USERNAME=root
DB_PASSWORD=123456

# JWT
JWT_SECRET_KEY=your-secret-key-min-32-characters
JWT_VALID_DURATION=900
JWT_REFRESH_DURATION=604800

# Server
SERVER_PORT=8080

# Admin mặc định
ADMIN_DEFAULT_USERNAME=admin
ADMIN_DEFAULT_PASSWORD=admin123

# Upload
UPLOAD_DIR=uploads
MAX_FILE_SIZE=10485760

# Redis (optional)
REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## Cơ sở dữ liệu

### Các bảng chính

| Bảng | Mô tả |
|------|-------|
| `user` | Tài khoản người dùng (admin, manager, organizer, student) |
| `profile` | Hồ sơ chi tiết của người dùng (mssv, lớp, khoa...) |
| `activity` | Hoạt động sinh viên |
| `activity_file` | File đính kèm hoạt động |
| `registration` | Đăng ký tham gia hoạt động |
| `attendance` | Điểm danh sinh viên |
| `notification` | Thông báo hệ thống |
| `role` | Vai trò người dùng |
| `permission` | Quyền hạn |
| `role_permission` | Map quyền - vai trò |
| `room` | Phòng học |
| `academic_period` | Học kỳ / năm học |
| `category` | Loại hoạt động |
| `system_config` | Cấu hình hệ thống |
| `system_log` | Nhật ký hệ thống |

### Chu kỳ trạng thái hoạt động

```
Draft → Pending → Reviewing → Approved → Ongoing → Closed
                                    ↘ (Rejected)
```

---

## API Endpoints

Base URL: `http://localhost:8080/api/v1`

### Xác thực

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/auth/login` | Đăng nhập |
| `POST` | `/auth/register` | Đăng ký |
| `POST` | `/auth/refresh` | Làm mới token |
| `POST` | `/auth/logout` | Đăng xuất |
| `POST` | `/auth/introspect` | Kiểm tra token |
| `GET` | `/auth/me` | Lấy thông tin user hiện tại |

### Hoạt động

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/activities` | Danh sách hoạt động |
| `POST` | `/activities` | Tạo hoạt động mới |
| `GET` | `/activities/{id}` | Chi tiết hoạt động |
| `PATCH` | `/activities/{id}` | Cập nhật hoạt động |
| `DELETE` | `/activities/{id}` | Xóa hoạt động |
| `PATCH` | `/activities/{id}/submit` | Gửi duyệt hoạt động |
| `PATCH` | `/activities/{id}/cancel-request` | Gửi yêu cầu hủy |
| `POST` | `/activities/{id}/reports` | Nộp báo cáo |
| `POST` | `/activities/{id}/reports/upload` | Upload file báo cáo |
| `GET` | `/activities/schedule-conflicts` | Kiểm tra xung đột lịch |

### Quản lý (Manager)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/manager/activities` | Danh sách hoạt động chờ duyệt |
| `PATCH` | `/manager/activities/{id}/approve` | Duyệt hoạt động |
| `PATCH` | `/manager/activities/{id}/reject` | Từ chối hoạt động |
| `PATCH` | `/manager/activities/{id}/review` | Bắt đầu review |
| `PATCH` | `/manager/activities/{id}/cancel` | Hủy hoạt động |
| `PATCH` | `/manager/activities/{id}/cancel-requests/approve` | Duyệt yêu cầu hủy |
| `PATCH` | `/manager/activities/{id}/cancel-requests/reject` | Từ chối yêu cầu hủy |
| `PATCH` | `/manager/activities/{id}/reports/{reportId}/approve` | Duyệt báo cáo |
| `PATCH` | `/manager/activities/{id}/reports/{reportId}/reject` | Từ chối báo cáo |
| `GET` | `/manager/activities/{id}/reports/{reportId}/download` | Tải báo cáo |
| `PATCH` | `/manager/registrations/{activityId}/students/{studentId}/approve` | Duyệt đăng ký SV |
| `PATCH` | `/manager/registrations/{activityId}/students/{studentId}/reject` | Từ chối đăng ký |
| `GET` | `/manager/statistics` | Thống kê hoạt động |
| `GET` | `/manager/student-statistics` | Thống kê sinh viên |

### Đăng ký & Điểm danh

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST` | `/registrations` | Đăng ký tham gia |
| `DELETE` | `/registrations/{id}` | Hủy đăng ký |
| `PATCH` | `/attendance/check-in` | Điểm danh sinh viên |
| `POST` | `/organizer/attendance/check-in` | Organizer điểm danh |
| `POST` | `/organizer/attendance/points` | Chấm điểm sinh viên |

### Quản trị (Admin)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/admin/users` | Danh sách người dùng |
| `POST` | `/admin/users` | Tạo người dùng |
| `PATCH` | `/admin/users/{id}` | Cập nhật người dùng |
| `DELETE` | `/admin/users/{id}` | Vô hiệu hóa người dùng |
| `GET` | `/admin/roles` | Danh sách vai trò |
| `PATCH` | `/admin/users/{id}/roles` | Gán vai trò |
| `PATCH` | `/admin/roles/permissions` | Cập nhật quyền vai trò |
| `GET` | `/admin/activities` | Danh sách hoạt động (admin) |
| `PATCH` | `/admin/activities/{id}/cancel` | Hủy hoạt động |
| `POST` | `/admin/notifications/broadcast` | Gửi thông báo broadcast |
| `GET` | `/admin/system-configs` | Lấy cấu hình hệ thống |
| `PUT` | `/admin/system-configs/{key}` | Cập nhật cấu hình |
| `GET` | `/admin/system-logs` | Xem nhật ký hệ thống |
| `GET` | `/admin/statistics` | Thống kê tổng quan |
| `POST` | `/admin/backups/export` | Tạo backup |
| `POST` | `/admin/backups/restore` | Khôi phục từ backup |
| `GET` | `/admin/backups` | Danh sách file backup |

### Các endpoint khác

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `GET` | `/categories` | Danh sách danh mục hoạt động |
| `GET` | `/rooms` | Danh sách phòng |
| `GET` | `/academic-periods` | Danh sách học kỳ |
| `GET` | `/notifications` | Thông báo của tôi |
| `PATCH` | `/notifications/{id}/read` | Đánh dấu đã đọc |
| `GET` | `/profile` | Hồ sơ cá nhân |
| `PUT` | `/profile` | Cập nhật hồ sơ |
| `POST` | `/auth/change-password` | Đổi mật khẩu |
| `GET` | `/student/enrolled` | Hoạt động đã đăng ký |
| `GET` | `/student/points` | Điểm rèn luyện |

---

## Bảo mật

- **JWT Authentication**: Token truy cập có thời hạn (mặc định 15 phút), refresh token được lưu trong Redis.
- **BCrypt Password Encoding**: Mật khẩu được băm với độ mạnh 10 rounds.
- **Role-based Access Control (RBAC)**: Phân quyền theo vai trò tại `SecurityConfig.java` và `RoleAssignmentPolicy.java`.
- **Permission Catalog**: Kiểm tra chi tiết từng quyền trong `PermissionCatalog.java`.
- **CORS Configuration**: Chỉ cho phép origin được cấu hình (mặc định `http://127.0.0.1:5500`).
- **System Logging**: Ghi nhật ký hành động người dùng qua `SystemLogService`.

---

## Kiểm thử

Dự án có bộ unit tests cho các thành phần quan trọng:

```bash
./mvnw test
```

Các test hiện có:
- `AdminActivityControllerSecurityTest` — Kiểm thử bảo mật controller hoạt động
- `AdminUserControllerSecurityTest` — Kiểm thử bảo mật controller người dùng
- `AdminSystemManagementSecurityTest` — Kiểm thử bảo mật quản trị hệ thống
- `RoleAssignmentPolicyTest` — Kiểm thử chính sách phân vai trò
- `ActivityServiceTest` — Kiểm thử service hoạt động
- `AttendanceServiceTest` — Kiểm thử service điểm danh
- `NotificationServiceTest` — Kiểm thử service thông báo
- `RegistrationServiceTest` — Kiểm thử service đăng ký
- `SystemConfigServiceTest` — Kiểm thử service cấu hình

---

## Backup & Restore

Hệ thống hỗ trợ backup thủ công và tự động qua `BackupService`.

### Tạo backup

```
POST /api/v1/admin/backups/export
```

File backup được lưu trong thư mục `backups/` với định dạng ZIP.

### Khôi phục

```
POST /api/v1/admin/backups/restore
Body: { "filename": "backup-manual-20260627003533.zip" }
```

> **Lưu ý:** Khôi phục sẽ ghi đè dữ liệu hiện tại. Hãy sao lưu trước khi thực hiện.

---

## Chu kỳ phát triển hoạt động đầy đủ

```
1. Organizer tạo hoạt động          (status: Draft)
2. Organizer gửi duyệt              (status: Pending)
3. Manager bắt đầu review            (status: Reviewing)
4. Manager duyệt                    (status: Approved)
5. Sinh viên đăng ký                 (registration: Pending/Approved)
6. Organizer/Manager duyệt SV        (registration: Approved)
7. Hoạt động diễn ra                (status: Ongoing)
8. Điểm danh                        (Attendance)
9. Hoạt động kết thúc               (status: Closed)
10. Organizer nộp báo cáo            (report: Pending)
11. Manager duyệt báo cáo            (report: Approved)
```
