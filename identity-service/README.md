# identity-service

Identity Service ("Phòng quản lý nhân khẩu") — lưu thông tin đăng nhập người dùng. Spring Boot 3.5, Java 21, Gradle, PostgreSQL.

## Chạy nhanh

```bash
# 1. Khởi động PostgreSQL (tự tạo database identity_db)
docker compose up -d

# Hoặc nếu đã có PostgreSQL sẵn:
# psql -U postgres -c "CREATE DATABASE identity_db;"

# 2. Chạy service (port 8080)
./gradlew bootRun
```

Cấu hình DB trong `src/main/resources/application.yml` (override bằng biến môi trường `DB_USERNAME`, `DB_PASSWORD`).

## API

### POST /api/auth/register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bao","password":"123456"}'
```

- `201 Created` → `{"id":"<uuid>","username":"bao","role":"USER"}`
- `409 Conflict` → username đã tồn tại
- `400 Bad Request` → dữ liệu không hợp lệ

Kiểm tra mật khẩu đã được băm:

```sql
SELECT id, username, password, role FROM users;
-- password dạng $2a$10$...
```

### GET /api/auth/test-token?username={name}  (Bài 2)

```bash
curl "http://localhost:8080/api/auth/test-token?username=bao"
```

- `200 OK` → `{"token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiYW8i...","tokenType":"Bearer","expiresIn":3600}`
- `404 Not Found` → user không tồn tại

Dán token vào https://jwt.io để xem 3 phần:

| Phần | Nội dung |
|------|----------|
| Header | `{"alg":"HS256"}` |
| Payload | `{"sub":"bao","role":"USER","iat":...,"exp":...}` |
| Signature | `HMACSHA256(base64url(header) + "." + base64url(payload), jwt-secret)` |

Cấu hình trong `application.yml`: `jwt-secret` (Base64 của 32 byte = 256-bit, override bằng `JWT_SECRET`), `jwt-expiration: 3600000`.
Tạo secret mới: `openssl rand -base64 32`.

### POST /api/auth/login  (Bài 3)

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bao","password":"123456"}'
```

- `200 OK` → `{"token":"eyJ...","tokenType":"Bearer","expiresIn":3600}`
- `401 Unauthorized` → `{"status":401,"message":"Bad credentials"}` (cả khi sai mật khẩu lẫn khi không có user)

Luồng xử lý (`AuthService.login`):
1. `userRepository.findByUsername(username)`
2. `passwordEncoder.matches(rawPassword, encodedPassword)` — không dùng `==`/`equals` vì BCrypt có salt ngẫu nhiên, mỗi lần `encode` cho ra chuỗi khác nhau.
3. Sai → `BadCredentialsException` → `GlobalExceptionHandler` trả 401 với thông báo chung.
   Nếu user không tồn tại vẫn chạy `matches` với một hash giả để thời gian phản hồi giống nhau (chống dò username).
4. Đúng → `JwtUtil.generateToken(user)`.

Stateless (`SecurityConfig`): `SessionCreationPolicy.STATELESS`, `NullSecurityContextRepository`, tắt `requestCache`/`formLogin`/`httpBasic`/`logout`.
Kiểm chứng: response đăng nhập (`curl -i`) **không có** header `Set-Cookie: JSESSIONID`.

### Postman

Import `postman/identity-service.postman_collection.json` → Run collection. Các request có sẵn test: 200 + JWT 3 phần + không có JSESSIONID, 401 khi sai mật khẩu, 401 khi user không tồn tại. Token đăng nhập được lưu vào biến `{{accessToken}}`.

## Cấu trúc

```
src/main/java/com/example/identityservice
├── IdentityServiceApplication.java
├── config/SecurityConfig.java        # SecurityFilterChain, csrf off, permitAll register, BCrypt bean
├── controller/AuthController.java    # register, login, test-token
├── dto/RegisterRequest, LoginRequest, UserResponse, TokenResponse
├── entity/User.java, Role.java       # id UUID, username, password, role
├── exception/...                     # 409 / 400 handlers
├── repository/UserRepository.java
├── security/JwtUtil.java             # generateToken(User), parseClaims(token)
└── service/AuthService.java          # register (BCrypt encode), login (BCrypt matches + JWT)
```
