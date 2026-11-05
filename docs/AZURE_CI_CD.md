# AlgoViz Azure CI/CD setup

The workflow `.github/workflows/algoviz-ci-cd.yml` runs checks on Pull Requests and pushes to `main`:

- Frontend (Node 22): `npm ci`, `npm run lint`, `npm run build`
- Backend (Java 17): `mvn -B -ntp verify`
- Backend image: build `backend/Dockerfile` and probe `/api/ast/health` in a container

## Deployment is disabled by default

Opt in via GitHub repository **Settings > Secrets and variables > Actions > Variables**:

| Variable | Value |
|---|---|
| `ALGOVIZ_ENABLE_FRONTEND_DEPLOY` | `true` only when frontend deploy is ready |
| `ALGOVIZ_ENABLE_BACKEND_DEPLOY` | **keep false until the code execution sandbox is secure** |
| `VITE_API_BASE_URL` | `https://<backend-host>/api` |
| `VITE_AST_API_BASE_URL` | `https://<backend-host>` |
| `AZURE_ACR_NAME` | Existing Azure Container Registry resource name |

Create GitHub environment named `production`. Recommended secrets for the environment:

- `AZURE_STATIC_WEB_APPS_API_TOKEN`: Azure Static Web App deployment token.
- `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`: Azure OIDC workload identity for the backend deployment. The Azure federated credential must trust subject `repo:hienpham0344/AlgoViz:environment:production` with audience `api://AzureADTokenExchange`.

Grant the Azure identity narrowly scoped rights for `az acr build` and Azure Container App updates. Before enabling backend CD, set up the Container App `algoviz-backend` in `rg-algoviz` and configure its identity to pull images from ACR.

**Important:** Current AlgoViz backend runs user code via `NativeSandboxStrategy`, and Docker sandbox is disabled by default. Packaging Spring Boot in a Docker image **does not isolate user programs from the API process**. Keep public ingress and backend CD disabled until this is remediated, including checking AST/JDI endpoints.

Make sure the Azure Static Web App is created and avoid leaving a second automatically generated deployment workflow active for the same app. Configure secrets for PostgreSQL, CORS and JWT inside Azure Container Apps, not GitHub.

After completing configuration, merging a PR runs CI automatically; enabling the corresponding toggle will allow CD on main pushes. Azure resources consume credits while used.

References: https://learn.microsoft.com/en-us/azure/container-apps/github-actions and https://learn.microsoft.com/en-us/azure/static-web-apps/build-configuration
