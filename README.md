# be_sme

Hướng dẫn này dành cho người mới để tải và chạy dự án nhanh nhất.

## Yêu cầu hệ thống

- **Java JDK 17** (bắt buộc)
- **Docker** + Docker Compose (khuyến nghị để chạy PostgreSQL nhanh)
- (Tuỳ chọn) PostgreSQL cài trực tiếp nếu không dùng Docker

> Dự án sử dụng Spring Boot, Maven Wrapper (`./mvnw`) nên không cần cài Maven riêng.

## 1) Clone dự án

```bash
git clone <URL_REPO>
cd be_sme
```

## 2) Khởi chạy PostgreSQL

### Cách A — Dùng Docker (khuyến nghị)

```bash
docker compose up -d
```

`docker-compose.yml` đã cấu hình sẵn:
- DB: `be_sme`
- User: `postgres`
- Password: `123456`
- Port: `5432`

### Cách B — Cài PostgreSQL thủ công

Tạo database và user giống cấu hình trong `src/main/resources/application.yml`:
- DB: `be_sme`
- User: `postgres`
- Password: `123456`
- Host: `127.0.0.1`
- Port: `5432`

Bạn có thể sửa lại trong `application.yml` nếu muốn dùng thông tin khác.

## 3) Chạy ứng dụng

```bash
./mvnw spring-boot:run
```

Ứng dụng sẽ chạy với profile mặc định và tự chạy migration bằng Flyway.

## 4) Cấu hình JWT (khuyến nghị)

Trong `src/main/resources/application.yml` có cấu hình:

```yaml
spring:
  app:
    jwt:
      hmacSecret: "CHANGE_ME_TO_A_LONG_RANDOM_SECRET_32+"
```

Khi chạy thật, hãy đổi `hmacSecret` thành chuỗi ngẫu nhiên đủ dài.

## 5) Một số lỗi thường gặp

- **Sai phiên bản Java**: đảm bảo `java -version` là 17.
- **Không kết nối được DB**: kiểm tra Docker/PostgreSQL có đang chạy và đúng port.
- **Sai thông tin DB**: kiểm tra `application.yml` và `docker-compose.yml`.

## Lệnh nhanh

```bash
# Chạy DB
docker compose up -d

# Chạy ứng dụng
./mvnw spring-boot:run
```
