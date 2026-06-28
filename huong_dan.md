# Hướng dẫn khởi động Backend (ECO_PTIT)

## ⚠️ Lưu ý quan trọng

**KHÔNG dùng lệnh `mvnw spring-boot:run`** với project này. Lệnh này gây lỗi
`ClassNotFoundException: com.example.manage_activities.ManageActivitiesApplication`
do Maven plugin không đưa `target/classes` vào classpath khi đường dẫn dự án chứa
ký tự tiếng Việt (`Bài Tập`).

**Cách chạy đúng là dùng `java -jar`** với file jar đã build sẵn.

## Yêu cầu trước khi chạy

Đảm bảo các dịch vụ sau đã chạy:
- **MySQL** trên `localhost:3306`, user `root` / password `123456`, đã có database `my_app_db`.
- **Redis** trên `localhost:6379` (không cần password).
- **Java 21** đã cài và có trong PATH.

## Cách 1 — Dùng file `start-backend.bat` (khuyến nghị)

Double-click vào file `start-backend.bat` ở thư mục gốc dự án.

## Cách 2 — Chạy thủ công bằng PowerShell

Mỗi lần khởi động lại máy hoặc muốn chạy lại, mở PowerShell rồi chạy khối lệnh
sau (copy nguyên khối rồi paste vào):

```powershell
cd "E:\Bài Tập\CNPM\ECO_PTIT"
$env:DB_PASSWORD="123456"
$env:DB_URL="jdbc:mysql://localhost:3306/my_app_db"
$env:DB_USERNAME="root"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:JWT_SECRET_KEY="MySecretKeyForJWTAuthenticationChangeThisInProduction123456"
java -jar target/db-0.0.1-SNAPSHOT.jar
```

Khi nào terminal in ra dòng:

```
Tomcat started on port 8080 (http) with context path ''
```

là backend đã sẵn sàng. Mở trình duyệt vào `http://localhost:8080` để kiểm tra.

## Cách 3 — Build lại jar rồi chạy

Nếu sửa code backend và cần build lại, chạy lệnh:

```powershell
cd "E:\Bài Tập\CNPM\ECO_PTIT"
.\mvnw.cmd clean package -DskipTests
```

Sau đó chạy lại `java -jar target/db-0.0.1-SNAPSHOT.jar` như Cách 2.

## Dừng backend

Mở Task Manager → tìm tiến trình `java.exe` → End task.

Hoặc trong PowerShell đang chạy backend, nhấn `Ctrl + C`.

## Biến môi trường

| Tên | Giá trị |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/my_app_db` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `123456` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `JWT_SECRET_KEY` | `MySecretKeyForJWTAuthenticationChangeThisInProduction123456` |

Các giá trị này khớp với file `src/main/resources/application-dev.properties`
nên có thể điều chỉnh nếu cấu hình DB/Redis khác.
