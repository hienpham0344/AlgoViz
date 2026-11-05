<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/hienpham0344/AlgoViz">
    <img src="frontend/public/favicon.svg" alt="AlgoViz logo" width="80" height="80">
  </a>

  <h3 align="center">AlgoViz</h3>

  <p align="center">
    Nền tảng học và luyện thuật toán: chạy code, nộp bài và xem từng bước thực thi của chương trình Java.
    <br />
    <a href="docs/"><strong>Xem tài liệu »</strong></a>
    <br />
    <br />
    <a href="https://github.com/hienpham0344/AlgoViz/issues/new">Báo lỗi</a>
    &middot;
    <a href="https://github.com/hienpham0344/AlgoViz/issues/new">Đề xuất tính năng</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Mục lục</summary>
  <ol>
    <li>
      <a href="#về-dự-án">Về dự án</a>
      <ul>
        <li><a href="#công-nghệ">Công nghệ</a></li>
      </ul>
    </li>
    <li>
      <a href="#bắt-đầu">Bắt đầu</a>
      <ul>
        <li><a href="#yêu-cầu">Yêu cầu</a></li>
        <li><a href="#cài-đặt-và-chạy">Cài đặt và chạy</a></li>
      </ul>
    </li>
    <li><a href="#docker">Docker</a></li>
    <li><a href="#triển-khai-trên-azure">Triển khai trên Azure</a></li>
    <li>
      <a href="#sử-dụng">Sử dụng</a>
      <ul>
        <li><a href="#phát-triển">Phát triển</a></li>
        <li><a href="#cấu-hình">Cấu hình</a></li>
        <li><a href="#kiểm-tra">Kiểm tra</a></li>
        <li><a href="#cấu-trúc-dự-án">Cấu trúc dự án</a></li>
      </ul>
    </li>
    <li><a href="#lộ-trình">Lộ trình</a></li>
    <li><a href="#bảo-mật">Bảo mật</a></li>
    <li><a href="#đóng-góp">Đóng góp</a></li>
    <li><a href="#giấy-phép">Giấy phép</a></li>
    <li><a href="#liên-hệ">Liên hệ</a></li>
    <li><a href="#lời-cảm-ơn">Lời cảm ơn</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## Về dự án

AlgoViz là ứng dụng web để học thuật toán và luyện bài tập. Backend Spring Boot phục vụ cả API và giao diện React đã build sẵn, nên chạy một tiến trình duy nhất.

Các chức năng chính:

* **Bài tập và chấm bài**: danh sách, chi tiết, chạy Python/JavaScript/Java/C++, nộp bài, verdict và lịch sử lấy từ server.
* **Trực quan hóa thực thi**: Run Java để xem trace thật theo từng dòng, mảng và biến cục bộ. Engine [AST-tool](docs/AST_INTEGRATION.md) dùng JavaParser để phân tích và JDI để quan sát JVM; kết quả còn có gợi ý thuật toán nhận diện bằng phân tích tĩnh.
* **Tài khoản**: đăng ký, đăng nhập, refresh token, hồ sơ, đổi mật khẩu. Khách vẫn duyệt bài, học nội dung và dùng workspace trực quan hóa.
* **Học tập**: khóa học và bài học; tiến độ lưu theo tài khoản.

Python/JavaScript/C++ chạy thật nhưng chưa có trace AST. Nhận diện thuật toán là phân tích tĩnh, không phải chứng minh tính đúng đắn.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

### Công nghệ

* [![React][React.js]][React-url]
* [![Vite][Vite.js]][Vite-url]
* [![Tailwind CSS][Tailwind.css]][Tailwind-url]
* [![Spring Boot][Spring.io]][Spring-url]
* [![PostgreSQL][Postgres.org]][Postgres-url]
* [![Docker][Docker.com]][Docker-url]

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- GETTING STARTED -->
## Bắt đầu

Chạy bản sao cục bộ theo các bước dưới đây. Nếu chỉ muốn thử nhanh, dùng [Docker](#docker).

### Yêu cầu

| Chạy bằng | Cần |
|---|---|
| Docker | Docker Engine 24+ với Docker Compose v2 |
| Cục bộ | Node.js 22.12+, JDK 17+ đầy đủ (có `javac` và module `jdk.jdi`), Python 3, g++ |

Maven Wrapper có sẵn trong `backend/`. Các script tự tìm JDK qua `JAVA_HOME`, `PATH` hoặc `~/.jdks/`, và Node qua `PATH` hoặc nvm.

### Cài đặt và chạy

1. Clone repo
   ```sh
   git clone https://github.com/hienpham0344/AlgoViz.git
   cd AlgoViz
   ```
2. Cài thư viện frontend
   ```sh
   npm --prefix frontend ci
   ```
3. Build frontend và backend
   ```sh
   npm run build
   ```
4. Chạy với H2 trong bộ nhớ
   ```sh
   npm run start:offline
   ```
5. Mở [localhost:8080](http://localhost:8080). Dữ liệu mất khi tắt server. Khi database trống, tài khoản thử nghiệm `demo` / `password123` được tạo.

Muốn dùng database riêng, đặt các biến `SPRING_DATASOURCE_*` trong `backend/.env` (mẫu ở `backend/.env.example`; không commit `.env`) rồi chạy `npm start`. Chạy lại `npm run build` sau khi sửa code. Nếu thư viện Maven đã có trong cache, build offline bằng `bash scripts/build-unified.sh -o`.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- DOCKER -->
## Docker

Image gồm frontend đã build, backend và công cụ chạy code. Container chạy bằng user không phải root, bỏ toàn bộ capabilities.

```sh
cp .env.example .env
# điền ALGOVIZ_JWT_SECRET, ví dụ: openssl rand -hex 32
docker compose up --build
```

Mặc định dùng H2 trong bộ nhớ nên dữ liệu mất khi container khởi động lại. Dùng PostgreSQL với dữ liệu giữ trong volume:

```sh
# Trong .env: đặt POSTGRES_PASSWORD và bỏ comment nhóm SPRING_DATASOURCE_* (xem .env.example)
docker compose --profile postgres up --build
```

Lệnh thường dùng:

```sh
docker compose logs -f app        # theo dõi log
docker compose down               # dừng, giữ volume
docker compose down -v            # dừng và xoá dữ liệu PostgreSQL
docker build -t algoviz:local .   # chỉ build image
```

Các file liên quan: `Dockerfile` (Node build, Maven package, JDK 21 runtime), `docker-compose.yml`, `.env.example`, `.dockerignore`. Workflow `.github/workflows/docker.yml` build và kiểm tra health của image trên mỗi pull request.

> Máy phát triển cần Docker để dùng mục này. Cấu hình Docker và Compose trong repo chưa được chạy thử trên máy có Docker, nên nếu gặp lỗi build, hãy mở issue kèm log.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- AZURE -->
## Triển khai trên Azure

Workflow `.github/workflows/deploy-azure.yml` chạy khi push `main`: build image, kiểm tra `/api/ast/health`, đẩy lên Azure Container Registry rồi deploy lên Azure App Service (Linux container) qua OIDC. Repo không lưu mật khẩu Azure. Job bị bỏ qua cho đến khi đặt variable `AZURE_WEBAPP_NAME`.

Cần cấu hình một lần: resource group, ACR, App Service, PostgreSQL, App registration có federated credential, và các giá trị sau trong GitHub (Settings → Secrets and variables → Actions):

| Loại | Tên |
|---|---|
| Variable | `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, `AZURE_WEBAPP_NAME`, `AZURE_CONTAINER_REGISTRY` |
| Secret | `APP_ALGOVIZ_JWT_SECRET`, `APP_SPRING_DATASOURCE_URL`, `APP_SPRING_DATASOURCE_USERNAME`, `APP_SPRING_DATASOURCE_PASSWORD` |

Lệnh `az`, subject của federated credential và các lưu ý production nằm trong [docs/DEPLOY_AZURE.md](docs/DEPLOY_AZURE.md). Đọc phần chạy code và sandbox trong tài liệu đó trước khi mở công khai.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- USAGE -->
## Sử dụng

### Phát triển

```sh
npm run dev:backend    # Spring Boot tại http://localhost:8080
npm run dev:frontend   # Vite tại http://localhost:5173, proxy /api
```

Vite chuyển `/api`, `/h2-console`, `/swagger-ui` và `/v3/api-docs` tới `localhost:8080`; đổi đích bằng `VITE_BACKEND_URL`. `VITE_API_BASE_URL` cho phép gọi API ở origin khác. Swagger UI nằm tại [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

JAR chứa frontend trong `frontend/dist`; Maven sao chép asset khi package. Các route như `/problems`, `/profile`, `/learn` do SPA fallback phục vụ, còn API và tài nguyên tĩnh không tồn tại trả lỗi đúng loại.

### Cấu hình

Các biến môi trường quan trọng (đầy đủ trong `backend/src/main/resources/application.yaml`):

| Biến | Ý nghĩa |
|---|---|
| `ALGOVIZ_JWT_SECRET` | Bắt buộc khi dùng Compose; tối thiểu 32 ký tự |
| `ALGOVIZ_PORT` | Cổng host của Compose (mặc định 8080) |
| `SPRING_DATASOURCE_URL`, `_DRIVER`, `_USERNAME`, `_PASSWORD` | Database; trống là H2 trong bộ nhớ |
| `ALGOVIZ_CORS_ALLOWED_ORIGINS` | Danh sách origin, cách nhau bằng dấu phẩy |
| `ALGOVIZ_EXECUTION_TIMEOUT_SECONDS` | Giới hạn thời gian chạy code (mặc định 5) |
| `ALGOVIZ_PYTHON_COMMAND`, `ALGOVIZ_CPP_COMMAND` | Lệnh chạy Python và C++ |
| `ALGOVIZ_IMPORT_ENABLED` | Import dữ liệu bài tập lúc khởi động (mặc định tắt) |

Docker sandbox cho Java (`ALGOVIZ_DOCKER_SANDBOX_ENABLED`) mặc định tắt và cần quyền gọi Docker daemon của host, nên không bật bên trong container.

### Kiểm tra

```sh
npm run test:frontend    # Node test runner
npm run lint
npm run test:backend     # Maven test với H2 riêng
npm run build
```

`bash scripts/test-backend.sh -o` chạy test bằng cache Maven. Test backend dùng H2 riêng, không kết nối database trong `.env`. Test AST/JDI cần mở socket loopback; nếu môi trường chặn socket thì các test đó lỗi.

### Cấu trúc dự án

```
backend/src/main/java/com/aisc/algoviz/
  ast/          engine AST-tool (JavaParser + JDI) và controller trace
  auth/         đăng nhập, JWT, refresh token, rate limit
  problem/      bài tập, import dữ liệu
  submission/   chạy code, chấm bài, sandbox (native/Docker)
  user/         hồ sơ, tiến độ học
frontend/src/features/   analyze, auth, history, learn, problems, profile, visualizer
frontend/src/services/   api, astApi, runtimeTrace, submissions, solutionTrace
scripts/                 build, run, test cục bộ
docs/                    AST_INTEGRATION, SANDBOX_CODE_EDITOR, DEPLOY_AZURE, OpenAPI
.github/workflows/       docker.yml, deploy-azure.yml
Dockerfile, docker-compose.yml
```

Xem thêm [tích hợp AST-tool](docs/AST_INTEGRATION.md) và [kiến trúc sandbox](docs/SANDBOX_CODE_EDITOR.md).

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- ROADMAP -->
## Lộ trình

- [x] Backend Spring Boot phục vụ cả giao diện React
- [x] Trace thực thi Java bằng AST-tool (JavaParser + JDI)
- [x] Đóng gói Docker và pipeline deploy Azure App Service
- [ ] Gọi `/api/algorithms/recognize` từ giao diện
- [ ] Test cho `/api/algorithms/catalog` và `/api/algorithms/recognize`
- [ ] Trace thực thi cho Python, JavaScript và C++
- [ ] Sandbox chạy code cách ly cho môi trường dùng chung
- [ ] Migration database có kiểm soát (Flyway hoặc Liquibase)

Xem [issues đang mở](https://github.com/hienpham0344/AlgoViz/issues) để biết đề xuất và lỗi đã biết.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- SECURITY -->
## Bảo mật

* Đổi `ALGOVIZ_JWT_SECRET` trước khi triển khai. Giá trị mặc định trong `application.yaml` chỉ dành cho phát triển.
* Chế độ chạy code native (cũng là chế độ trong container) chạy tiến trình trên cùng hệ điều hành, không cách ly filesystem và network. Môi trường dùng chung cần worker cách ly riêng (Docker, VM hoặc dịch vụ judge) hoặc tắt các endpoint chạy code; xem [docs/SANDBOX_CODE_EDITOR.md](docs/SANDBOX_CODE_EDITOR.md).
* Container chạy non-root, bỏ toàn bộ capabilities và có healthcheck. Cấu hình này chưa đủ cho môi trường nhiều người dùng không tin cậy.
* Tài khoản `demo` được tạo khi database trống; hãy tắt seeder này ở production.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- CONTRIBUTING -->
## Đóng góp

Mọi đóng góp đều được hoan nghênh.

1. Fork dự án
2. Tạo nhánh tính năng (`git checkout -b feature/TinhNangMoi`)
3. Commit thay đổi (`git commit -m 'Thêm tính năng mới'`)
4. Push lên nhánh (`git push origin feature/TinhNangMoi`)
5. Mở Pull Request

Chạy `npm run lint` và `npm test` trước khi gửi.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- LICENSE -->
## Giấy phép

Phát hành theo giấy phép MIT. Xem `LICENSE` để biết thêm.

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- CONTACT -->
## Liên hệ

hienpham0344 - [github.com/hienpham0344](https://github.com/hienpham0344)

Dự án: [https://github.com/hienpham0344/AlgoViz](https://github.com/hienpham0344/AlgoViz)

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## Lời cảm ơn

* [Best-README-Template](https://github.com/othneildrew/Best-README-Template)
* [JavaParser](https://javaparser.org/)
* [Spring Boot](https://spring.io/projects/spring-boot)
* [Img Shields](https://shields.io)

<p align="right">(<a href="#readme-top">về đầu trang</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/hienpham0344/AlgoViz.svg?style=for-the-badge
[contributors-url]: https://github.com/hienpham0344/AlgoViz/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/hienpham0344/AlgoViz.svg?style=for-the-badge
[forks-url]: https://github.com/hienpham0344/AlgoViz/network/members
[stars-shield]: https://img.shields.io/github/stars/hienpham0344/AlgoViz.svg?style=for-the-badge
[stars-url]: https://github.com/hienpham0344/AlgoViz/stargazers
[issues-shield]: https://img.shields.io/github/issues/hienpham0344/AlgoViz.svg?style=for-the-badge
[issues-url]: https://github.com/hienpham0344/AlgoViz/issues
[license-shield]: https://img.shields.io/github/license/hienpham0344/AlgoViz.svg?style=for-the-badge
[license-url]: https://github.com/hienpham0344/AlgoViz/blob/main/LICENSE
[React.js]: https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB
[React-url]: https://react.dev/
[Vite.js]: https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white
[Vite-url]: https://vite.dev/
[Tailwind.css]: https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white
[Tailwind-url]: https://tailwindcss.com/
[Spring.io]: https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[Spring-url]: https://spring.io/projects/spring-boot
[Postgres.org]: https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white
[Postgres-url]: https://www.postgresql.org/
[Docker.com]: https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white
[Docker-url]: https://www.docker.com/
