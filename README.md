# Microservices demo

| Service | Port | Công nghệ | Vai trò |
|---------|------|-----------|---------|
| gateway-service | **8888** | Spring Cloud Gateway (WebFlux/Netty) | Cổng vào duy nhất, định tuyến request |
| identity-service | 8080 | Spring Boot MVC + Security + JPA + PostgreSQL | Đăng ký / đăng nhập / cấp JWT (Bài 1–3) |
| product-service | 8082 | Spring Boot MVC | Service giả lập `/api/products` |

## Định tuyến (gateway-service/src/main/resources/application.yml)

| Client gọi Gateway | Predicate | Filter | Chuyển tiếp tới |
|--------------------|-----------|--------|-----------------|
| `:8888/identity/api/auth/login` | `Path=/identity/**` | `StripPrefix=1` | `:8080/api/auth/login` |
| `:8888/product/api/products` | `Path=/product/**` | `StripPrefix=1` | `:8082/api/products` |
| path khác | — | — | 404 tại Gateway |

`StripPrefix=1` bỏ đoạn path đầu tiên (`/identity`, `/product`) trước khi chuyển tiếp, nên service bên dưới không cần biết về tiền tố của Gateway.

## Chạy

```bash
cd identity-service && docker compose up -d && ./gradlew bootRun   # terminal 1
cd product-service  && ./gradlew bootRun                           # terminal 2
cd gateway-service  && ./gradlew bootRun                           # terminal 3
```

## Kiểm tra

```bash
curl -X POST http://localhost:8888/identity/api/auth/register -H "Content-Type: application/json" -d '{"username":"bao","password":"123456"}'
curl -X POST http://localhost:8888/identity/api/auth/login    -H "Content-Type: application/json" -d '{"username":"bao","password":"123456"}'
curl http://localhost:8888/product/api/products
curl http://localhost:8888/product/api/products/1
```

Hoặc import `postman/gateway.postman_collection.json` → Run collection.
Log DEBUG của Gateway sẽ in route được khớp, ví dụ `Route matched: identity-service`.

## Ghi chú

- gateway-service **không** có `spring-boot-starter-web`: Gateway là ứng dụng reactive chạy trên Netty; nếu có Spring MVC trong classpath, ứng dụng sẽ báo lỗi và không khởi động được.
- gateway-service dùng Spring Boot 3.4.10 + Spring Cloud 2024.0.2 để giữ đúng tên `spring-cloud-starter-gateway` và prefix cấu hình `spring.cloud.gateway.routes`.
  (Từ Spring Cloud 2025.0 artifact được đổi thành `spring-cloud-starter-gateway-server-webflux` và prefix thành `spring.cloud.gateway.server.webflux.*`.)
