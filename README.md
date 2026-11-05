<a id="readme-top"></a>

<div align="center">
  <img src="frontend/public/favicon.svg" alt="AlgoViz" width="96" height="96" />

  <h1>AlgoViz</h1>

  <p>
    An algorithm learning and coding practice platform — run code, submit solutions,<br>
    and <strong>step through every line of your Java program</strong> with a live runtime trace.
  </p>

  <p>
    <a href="https://github.com/hienpham0344/AlgoViz/stargazers"><img src="https://img.shields.io/github/stars/hienpham0344/AlgoViz?style=for-the-badge&color=FFD700" alt="Stars"></a>
    <a href="https://github.com/hienpham0344/AlgoViz/network/members"><img src="https://img.shields.io/github/forks/hienpham0344/AlgoViz?style=for-the-badge&color=4B8BF5" alt="Forks"></a>
    <a href="https://github.com/hienpham0344/AlgoViz/issues"><img src="https://img.shields.io/github/issues/hienpham0344/AlgoViz?style=for-the-badge&color=FF6B6B" alt="Issues"></a>
    <a href="https://github.com/hienpham0344/AlgoViz/blob/main/LICENSE"><img src="https://img.shields.io/github/license/hienpham0344/AlgoViz?style=for-the-badge&color=2ECC71" alt="License"></a>
  </p>

  <p>
    <a href="docs/AST_INTEGRATION.md"><strong>Documentation</strong></a>
    &nbsp;·&nbsp;
    <a href="https://github.com/hienpham0344/AlgoViz/issues/new">Report a Bug</a>
    &nbsp;·&nbsp;
    <a href="https://github.com/hienpham0344/AlgoViz/issues/new">Request a Feature</a>
  </p>
</div>

---

## ✨ Features

- 🔍 **Live Java Trace** — step through every line with array state, local variables and algorithm hints, powered by [AST-tool](docs/AST_INTEGRATION.md) (JavaParser + JDI)
- 🏃 **Multi-language Runner** — execute Python, JavaScript, Java and C++ submissions against test cases with auto-judging
- 📚 **Problem Library** — curated problems (LeetCode-compatible IDs), difficulty badges, hints and editorial solutions
- 🎓 **Courses & Progress** — structured lessons with per-account progress tracking
- 👤 **Accounts** — register, login, JWT + refresh token, profile, password change and submission history
- 🐳 **One-command start** — `docker compose up --build` serves the full app on port 8080

---

## 🛠 Tech Stack

<div align="center">
  <img src="https://img.shields.io/badge/React_19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" alt="React">
  <img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite">
  <img src="https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind CSS">
  <br/>
  <img src="https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <br/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/Azure_App_Service-0078D4?style=for-the-badge&logo=microsoftazure&logoColor=white" alt="Azure">
  <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions">
</div>

---

## 📋 Table of Contents

<details>
  <summary>Expand</summary>
  <ol>
    <li><a href="#-quick-start-docker">Quick Start (Docker)</a></li>
    <li><a href="#-local-setup-without-docker">Local Setup</a></li>
    <li><a href="#-development">Development</a></li>
    <li><a href="#-configuration">Configuration</a></li>
    <li><a href="#-testing">Testing</a></li>
    <li><a href="#-project-structure">Project Structure</a></li>
    <li><a href="#-deploy-to-azure">Deploy to Azure</a></li>
    <li><a href="#-roadmap">Roadmap</a></li>
    <li><a href="#-contributing">Contributing</a></li>
    <li><a href="#-security">Security</a></li>
    <li><a href="#-license">License</a></li>
  </ol>
</details>

---

## 🐳 Quick Start (Docker)

The all-in-one image builds the frontend, packages it into the Spring Boot JAR and runs everything on port 8080. The container runs as a non-root user.

```bash
# 1. Copy the env template
cp .env.example .env

# 2. Set the required secret (paste the output as ALGOVIZ_JWT_SECRET in .env)
openssl rand -hex 32

# 3. Build and start
docker compose up --build
```

Open **http://localhost:8080**. A demo account `demo` / `password123` is created when the database is empty.

By default, data is stored in an in-memory H2 database and **lost on restart**.  
To persist data with PostgreSQL:

```bash
# In .env – uncomment the SPRING_DATASOURCE_* block and set POSTGRES_PASSWORD
docker compose --profile postgres up --build
```

<details>
  <summary>More Docker commands</summary>

```bash
docker compose logs -f app       # stream logs
docker compose down              # stop (keep volumes)
docker compose down -v           # stop + wipe PostgreSQL data
docker build -t algoviz:local .  # build image only
```

</details>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 💻 Local Setup (without Docker)

**Prerequisites:** Node.js 22.12+, JDK 17+ (full JDK with `javac` and `jdk.jdi`), Python 3, g++

```bash
# Clone
git clone https://github.com/hienpham0344/AlgoViz.git
cd AlgoViz

# Install frontend dependencies
npm --prefix frontend ci

# Build frontend + package backend JAR
npm run build

# Run with in-memory H2 (no database setup required)
npm run start:offline
```

Open **http://localhost:8080**.

To use your own database, copy `backend/.env.example` to `backend/.env`, fill in `SPRING_DATASOURCE_*`, then run `npm start`.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 🔧 Development

```bash
npm run dev:backend    # Spring Boot at http://localhost:8080
npm run dev:frontend   # Vite dev server at http://localhost:5173
```

Vite proxies `/api`, `/swagger-ui`, `/h2-console` and `/v3/api-docs` to `localhost:8080`.  
Override the target with `VITE_BACKEND_URL`.

| Useful URL | Description |
|---|---|
| http://localhost:8080 | Full app (built frontend) |
| http://localhost:5173 | Hot-reload frontend |
| http://localhost:8080/swagger-ui.html | API reference |
| http://localhost:8080/h2-console | In-memory DB console |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## ⚙️ Configuration

All settings have safe defaults; only `ALGOVIZ_JWT_SECRET` is required in production.

| Variable | Default | Description |
|---|---|---|
| `ALGOVIZ_JWT_SECRET` | dev-only value | **Required in production.** Min 32 chars |
| `SPRING_DATASOURCE_URL` | H2 in-memory | JDBC URL for PostgreSQL or other DB |
| `SPRING_DATASOURCE_DRIVER` | `org.h2.Driver` | Driver class name |
| `SPRING_DATASOURCE_USERNAME` | `sa` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | _(empty)_ | Database password |
| `ALGOVIZ_CORS_ALLOWED_ORIGINS` | `localhost:8080` | Comma-separated allowed origins |
| `ALGOVIZ_EXECUTION_TIMEOUT_SECONDS` | `5` | Code execution time limit |
| `ALGOVIZ_PYTHON_COMMAND` | `python3` | Python binary |
| `ALGOVIZ_CPP_COMMAND` | `g++` | C++ compiler binary |
| `ALGOVIZ_IMPORT_ENABLED` | `false` | Import problem data on startup |

Full list: `backend/src/main/resources/application.yaml`.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 🧪 Testing

```bash
npm run test:frontend   # Node test runner (services + components)
npm run lint            # ESLint
npm run test:backend    # Maven tests (H2 in-memory, isolated)
npm run build           # Full build verification
```

Backend tests use a separate in-memory H2 database and do not touch your `.env`. AST/JDI tests require loopback sockets; they will fail in heavily sandboxed environments.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 📁 Project Structure

```
AlgoViz/
├── frontend/
│   └── src/
│       ├── features/        # analyze, auth, history, learn, problems, profile, visualizer
│       └── services/        # api, astApi, runtimeTrace, solutionTrace, submissions
├── backend/
│   └── src/main/java/com/aisc/algoviz/
│       ├── ast/             # AST-tool engine (JavaParser + JDI) + trace API
│       ├── auth/            # JWT, refresh tokens, rate limiting
│       ├── problem/         # Problem CRUD, importer
│       ├── submission/      # Code runner, judge, sandbox (native / Docker)
│       └── user/            # Profiles, learning progress
├── docs/
│   ├── AST_INTEGRATION.md   # How the Java trace engine works
│   ├── SANDBOX_CODE_EDITOR.md
│   └── DEPLOY_AZURE.md
├── .github/workflows/
│   ├── docker.yml           # Build + health-check on PRs
│   └── deploy-azure.yml     # Build + push ACR + deploy App Service on main
├── Dockerfile               # Multi-stage: Node → Maven → JDK 21 runtime
├── docker-compose.yml       # App + optional PostgreSQL profile
└── scripts/                 # Local build / run / test helpers
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## ☁️ Deploy to Azure

The workflow `.github/workflows/deploy-azure.yml` runs on every push to `main`: builds the image, smoke-tests `/api/ast/health`, pushes to Azure Container Registry and deploys to App Service via OIDC — no Azure credentials stored in the repo. The job is **skipped** until `AZURE_WEBAPP_NAME` is set.

**Required GitHub configuration** (Settings → Secrets and variables → Actions):

| Kind | Name |
|---|---|
| Variable | `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID` |
| Variable | `AZURE_RESOURCE_GROUP`, `AZURE_WEBAPP_NAME`, `AZURE_CONTAINER_REGISTRY` |
| Secret | `APP_ALGOVIZ_JWT_SECRET`, `APP_SPRING_DATASOURCE_URL` |
| Secret | `APP_SPRING_DATASOURCE_USERNAME`, `APP_SPRING_DATASOURCE_PASSWORD` |

See [docs/DEPLOY_AZURE.md](docs/DEPLOY_AZURE.md) for the `az` commands, federated credential setup and production caveats.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 🗺 Roadmap

- [x] Live Java execution trace (AST-tool / JavaParser + JDI)
- [x] Spring Boot backend serving the bundled React frontend
- [x] Docker Compose stack with optional PostgreSQL
- [x] Azure App Service CI/CD via GitHub Actions + OIDC
- [ ] Call `/api/algorithms/recognize` from the visualizer UI
- [ ] Runtime trace support for Python, JavaScript and C++
- [ ] Isolated sandbox for shared/production environments
- [ ] Controlled database migrations (Flyway or Liquibase)
- [ ] Remove hard-coded demo account seeder for production

See the [open issues](https://github.com/hienpham0344/AlgoViz/issues) for full proposals.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 🤝 Contributing

1. Fork the project
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'feat: add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

Please run `npm run lint` and `npm test` before submitting.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 🔒 Security

> **Important:** Change `ALGOVIZ_JWT_SECRET` before deploying. The default value in `application.yaml` is for local development only.

- The native code runner executes processes on the same OS — it is **not** a sandboxed environment. For shared deployments, use isolated workers or disable the code execution endpoints. See [docs/SANDBOX_CODE_EDITOR.md](docs/SANDBOX_CODE_EDITOR.md).
- The Docker container runs as a non-root user with all capabilities dropped, but this is not sufficient for untrusted multi-tenant use.
- The `demo` account is seeded when the database is empty — disable the seeder in production.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 📄 License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<div align="center">
  <p>
    Built with ❤️ by <a href="https://github.com/hienpham0344">hienpham0344</a>
    &nbsp;·&nbsp;
    <a href="https://github.com/hienpham0344/AlgoViz">github.com/hienpham0344/AlgoViz</a>
  </p>
  <p>
    <a href="https://github.com/othneildrew/Best-README-Template">README template by othneildrew</a>
    &nbsp;·&nbsp;
    <a href="https://shields.io">Shields.io</a>
    &nbsp;·&nbsp;
    <a href="https://javaparser.org">JavaParser</a>
  </p>
</div>
