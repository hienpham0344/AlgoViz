# AlgoViz

Nền tảng học và luyện thuật toán: frontend React 19/Vite, backend Spring Boot 3/Java 21 (Java 17+), chạy và nộp code, trực quan hóa từng bước thực thi, khóa học và hồ sơ cá nhân.

## Mục lục

- [Yêu cầu](#yêu-cầu)
- [Chạy nhanh bằng Docker](#chạy-nhanh-bằng-docker)
- [Chạy cục bộ (không Docker)](#chạy-cục-bộ-không-docker)
- [Triển khai trên Azure](#triển-khai-trên-azure)
- [Phát triển](#phát-triển)
- [Cấu hình](#cấu-hình)
- [Kiểm tra](#kiểm-tra)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Các luồng đã tích hợp](#các-luồng-đã-tích-hợp)
- [Ghi chú bảo mật](#ghi-chú-bảo-mật)

## Yêu cầu

| Chạy bằng | Cần |
|---|---|
| Docker | Docker Engine 24+ với Docker Compose v2 |
| Cục bộ | Node.js 22.12+, JDK 17+ (đầy đủ `javac` và module `jdk.jdi`), Python 3, g++ |

Maven Wrapper có sẵn trong `backend/`. Scripts tự tìm JDK qua `JAVA_HOME`, `PATH` hoặc `~/.jdks/`; Node qua `PATH` hoặc nvm.

## Chạy nhanh bằng Docker

Image all-in-one: frontend được build vào JAR, backend phục vụ cả API và UI. Container chạy bằng user không phải root.

```bash
cp .env.example .env
# Điền ALGOVIZ_JWT_SECRET, ví dụ: openssl rand -hex 32
docker compose up --build
```

Mở [localhost:8080](http://localhost:8080). Mặc định dùng **H2 trong bộ nhớ**: dữ liệu mất khi container khởi động lại; tài khoản `demo` / `password123` được tạo khi database trống.

Dùng PostgreSQL, dữ liệu được giữ trong volume:

```bash
# Trong .env: đặt POSTGRES_PASSWORD và bỏ comment nhóm SPRING_DATASOURCE_* (xem .env.example)
docker compose --profile postgres up --build
```

Lệnh khác:

```bash
docker compose logs -f app        # theo dõi log
docker compose down               # dừng (giữ volume)
docker compose down -v            # dừng và xoá dữ liệu PostgreSQL
docker build -t algoviz:local .   # chỉ build image
docker run --rm -p 8080:8080 -e ALGOVIZ_JWT_SECRET="$(openssl rand -hex 32)" algoviz:local
```

Áp dụng: `Dockerfile` (multi-stage: Node build → Maven package → JRE runtime), `docker-compose.yml` (service `app`, `postgres` tùy chọn), `.env.example`, `.dockerignore`. Workflow `.github/workflows/docker.yml` build và smoke test image trên pull request; `.github/workflows/deploy-azure.yml` deploy khi push `main`.

## Triển khai trên Azure

Pipeline `.github/workflows/deploy-azure.yml` chạy khi push `main`: build image → smoke test `/api/ast/health` → push lên Azure Container Registry → deploy lên Azure App Service (Linux container) qua OIDC, không lưu mật khẩu Azure.

Cần cấu hình một lần: resource group, ACR, App Service, PostgreSQL, App registration với federated credential, và các variable/secret trong GitHub (Settings → Secrets and variables → Actions):

| Loại | Tên |
|---|---|
| Variable | `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, `AZURE_WEBAPP_NAME`, `AZURE_CONTAINER_REGISTRY` |
| Secret | `APP_ALGOVIZ_JWT_SECRET`, `APP_SPRING_DATASOURCE_URL`, `APP_SPRING_DATASOURCE_USERNAME`, `APP_SPRING_DATASOURCE_PASSWORD` |

Lệnh `az`, subject của federated credential và lưu ý production nằm trong [docs/DEPLOY_AZURE.md](docs/DEPLOY_AZURE.md). Trước khi mở công khai, đọc mục chạy code và sandbox trong tài liệu đó.

## Chạy cục bộ (không Docker)

```bash
npm --prefix frontend ci
npm run build
npm run start:offline
```

Mở [localhost:8080](http://localhost:8080). `start:offline` chạy backend thật với H2 trong bộ nhớ. Dùng database riêng thì chạy `npm start` với biến môi trường trong `backend/.env` (xem `backend/.env.example`); không commit file `.env`. Chạy lại `npm run build` sau khi sửa code. Nếu thư viện Maven đã có trong cache, build offline bằng `bash scripts/build-unified.sh -o`.

## Phát triển

```bash
npm run dev:backend    # Spring Boot tại http://localhost:8080
npm run dev:frontend   # Vite tại http://localhost:5173, proxy /api
```

Vite chuyển `/api`, `/h2-console`, `/swagger-ui` và `/v3/api-docs` tới `localhost:8080`; đổi đích bằng `VITE_BACKEND_URL`. `VITE_API_BASE_URL` cho phép gọi API ở origin khác.

Bản JAR chứa frontend trong `frontend/dist`; Maven sao chép asset khi package. Các route trực tiếp như `/problems`, `/profile`, `/learn` do SPA fallback phục vụ; API và tài nguyên tĩnh không tồn tại trả lỗi đúng loại.

## Cấu hình

Biến môi trường quan trọng (xem đầy đủ trong `backend/src/main/resources/application.yaml`):

| Biến | Ý nghĩa |
|---|---|
| `ALGOVIZ_JWT_SECRET` | Bắt buộc khi dùng Compose; tối thiểu 32 ký tự |
| `ALGOVIZ_PORT` | Cổng host của Compose (mặc định 8080) |
| `SPRING_DATASOURCE_URL`, `_DRIVER`, `_USERNAME`, `_PASSWORD` | Database; trống = H2 trong bộ nhớ |
| `ALGOVIZ_CORS_ALLOWED_ORIGINS` | Danh sách origin cách nhau bởi dấu phẩy |
| `ALGOVIZ_EXECUTION_TIMEOUT_SECONDS` | Giới hạn chạy code (mặc định 5) |
| `ALGOVIZ_PYTHON_COMMAND`, `ALGOVIZ_CPP_COMMAND` | Lệnh chạy Python/C++ |
| `ALGOVIZ_IMPORT_ENABLED` | Import dữ liệu bài tập lúc khởi động (mặc định tắt) |

Docker sandbox cho Java (`ALGOVIZ_DOCKER_SANDBOX_ENABLED`) mặc định tắt và chỉ dùng được khi app gọi được Docker daemon của host, nên không bật trong container.

## Kiểm tra

```bash
npm run test:frontend    # Node test runner
npm run lint
npm run test:backend     # Maven test với H2 riêng
npm run build
```

`bash scripts/test-backend.sh -o` chạy test bằng cache Maven. Test backend dùng H2 riêng, không kết nối database trong `.env`. Một số test phụ thuộc môi trường được khai báo bỏ qua sẵn. Test AST/JDI cần mở socket loopback với JVM target; nếu môi trường chặn socket (sandbox, host hạn chế) các test đó sẽ lỗi.

## Cấu trúc dự án

```
backend/src/main/java/com/aisc/algoviz/
  ast/            engine AST-tool (JavaParser + JDI) và controller trace
  auth/           đăng nhập, JWT, refresh token, rate limit
  problem/        bài tập, import dữ liệu
  submission/     chạy code, chấm bài, sandbox (native/Docker)
  user/           hồ sơ, tiến độ học
backend/src/main/resources/ application.yaml, schema, openapi
frontend/src/features/  analyze, auth, history, learn, problems, profile, visualizer
frontend/src/services/  api, astApi, runtimeTrace, submissions, solutionTrace
scripts/          build/run/test cục bộ
docs/             AST_INTEGRATION.md, SANDBOX_CODE_EDITOR.md, DEPLOY_AZURE.md, OpenAPI
.github/workflows/   docker.yml, deploy-azure.yml
Dockerfile, docker-compose.yml
```

## Các luồng đã tích hợp

- Đăng ký/đăng nhập, refresh token và logout; phiên hết hạn trả HTTP 401. Khách có thể duyệt bài, học nội dung và dùng workspace trực quan hóa.
- Danh sách/chi tiết bài, chạy Python/JavaScript/Java/C++, nộp bài, verdict và lịch sử lấy từ server. LeetCode ID được dùng nhất quán khi có; ID nội bộ là phương án dự phòng.
- Hồ sơ, giới thiệu, mục tiêu và đổi mật khẩu lưu vào backend; đổi mật khẩu thu hồi refresh token và yêu cầu đăng nhập lại.
- Tiến độ bài học lưu theo tài khoản qua `/api/users/learning-progress`.
- Xóa submission chỉ áp dụng cho tài khoản sở hữu; xóa lần Accepted cuối sẽ cập nhật lại điểm và số bài đã giải.
- AST-tool/JavaParser/JDI tích hợp trong backend: Run Java để xem trace thực với dòng source, mảng và biến cục bộ. Python/JavaScript/C++ chạy thật nhưng chưa có runtime trace AST. Xem [docs/AST_INTEGRATION.md](docs/AST_INTEGRATION.md).

Swagger: [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

## Ghi chú bảo mật

- Đổi `ALGOVIZ_JWT_SECRET` trước khi triển khai; giá trị mặc định trong `application.yaml` chỉ dành cho phát triển.
- Chế độ chạy code native (Scripts/container dùng native) chạy tiến trình trong cùng hệ điều hành, không cách ly filesystem/network như sandbox cách ly thật. Môi trường chia sẻ cần bật cách ly (Docker worker riêng, VM hoặc dịch vụ judge) hoặc tắt các endpoint chạy code; xem [docs/SANDBOX_CODE_EDITOR.md](docs/SANDBOX_CODE_EDITOR.md).
- Container đã chạy non-root, drop toàn bộ capabilities và có healthcheck; đây chưa phải sandbox đa tenant.
