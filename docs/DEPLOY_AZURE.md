# Triển khai AlgoViz trên Azure

Mô hình: GitHub Actions build image → đẩy lên Azure Container Registry (ACR) → Azure App Service (Linux, container) chạy image. Xác thực bằng OIDC, không lưu mật khẩu Azure trong repo. Database: Azure Database for PostgreSQL.

Tài liệu này là hướng dẫn cấu hình; các bước `az` chỉ cần chạy **một lần** trên máy có Azure CLI và đã `az login`, không chạy trong CI. Chưa được kiểm chứng trên subscription Azure thực tế.

## 1. Chuẩn bị tài nguyên

```bash
az group create --name rg-algoviz --location southeastasia
az acr create --resource-group rg-algoviz --name algovizacr --sku Basic --admin-enabled false
az postgres flexible-server create \
  --resource-group rg-algoviz --name algoviz-pg --location southeastasia \
  --admin-user algoviz --admin-password '<mật khẩu mạnh>' \
  --database-name algoviz --sku-name Standard_B1ms --tier Burstable --public-access 0.0.0.0
az appservice plan create --resource-group rg-algoviz --name plan-algoviz \
  --is-linux --sku B1
az webapp create --resource-group rg-algoviz --plan plan-algoviz \
  --name <tên-app-duy-nhất> --deployment-container-image-name mcr.microsoft.com/azuredocs/aci-helloworld
```

> `--public-access 0.0.0.0` chỉ để thử nghiệm. Nên cấu hình Private Endpoint / firewall cho production.

App Service cần quyền kéo image từ ACR:

```bash
PRINCIPAL_ID=$(az webapp identity assign --resource-group rg-algoviz --name <tên-app> --query principalId -o tsv)
ACR_ID=$(az acr show --name algovizacr --query id -o tsv)
az role assignment create --assignee "$PRINCIPAL_ID" --scope "$ACR_ID" --role AcrPull
az webapp config set --resource-group rg-algoviz --name <tên-app> \
  --generic-configurations '{"acrUseManagedIdentityCreds": true}'
```

## 2. App registration cho GitHub OIDC

```bash
APP_ID=$(az ad app create --display-name github-algoviz --query appId -o tsv)
az ad sp create --id "$APP_ID"

# Federated credential cho nhánh main của repo
az ad app federated-credential create --id "$APP_ID" --parameters '{
  "name": "github-main",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:hienpham0344/AlgoViz:ref:refs/heads/main",
  "audiences": ["api://AzureADTokenExchange"]
}'
```

Cấp quyền tối thiểu cho service principal (thay ID bằng giá trị của subscription):

```bash
SCOPE=/subscriptions/<SUBSCRIPTION_ID>/resourceGroups/rg-algoviz
az role assignment create --assignee "$APP_ID" --scope "$SCOPE" --role Contributor
```

Quyền cần thiết cho workflow: `AcrPush` (qua Contributor trên RG), `Website Contributor` (deploy và ghi app settings). Nếu dùng chặt hơn, gán riêng `AcrPush` trên ACR và `Website Contributor` trên App Service.

## 3. Cấu hình GitHub

Repository → **Settings → Secrets and variables → Actions**:

| Loại | Tên | Giá trị |
|---|---|---|
| Variable | `AZURE_CLIENT_ID` | `$APP_ID` |
| Variable | `AZURE_TENANT_ID` | `az account show --query tenantId -o tsv` |
| Variable | `AZURE_SUBSCRIPTION_ID` | `az account show --query id -o tsv` |
| Variable | `AZURE_RESOURCE_GROUP` | `rg-algoviz` |
| Variable | `AZURE_WEBAPP_NAME` | tên App Service |
| Variable | `AZURE_CONTAINER_REGISTRY` | `<tên-acr>.azurecr.io` |
| Secret | `APP_ALGOVIZ_JWT_SECRET` | `openssl rand -hex 32` (≥ 32 ký tự) |
| Secret | `APP_SPRING_DATASOURCE_URL` | `jdbc:postgresql://algoviz-pg.postgres.database.azure.com:5432/algoviz?sslmode=require` |
| Secret | `APP_SPRING_DATASOURCE_USERNAME` | `algoviz` |
| Secret | `APP_SPRING_DATASOURCE_PASSWORD` | mật khẩu PostgreSQL ở bước 1 |

Tạo environment `production` (Settings → Environments) để có thể thêm required reviewers.

## 4. Workflow

`.github/workflows/deploy-azure.yml` chạy khi push `main` hoặc `workflow_dispatch`. Job bị bỏ qua cho đến khi variable `AZURE_WEBAPP_NAME` được đặt, nên push khi chưa cấu hình không làm pipeline báo lỗi. Các bước:

1. Kiểm tra đủ biến/bí mật.
2. Build image bằng `Dockerfile` (Node build → Maven package → JRE runtime).
3. Chạy container tạm và chờ `/api/ast/health` trả 200 (smoke test).
4. `az login` bằng OIDC, `az acr login` và push image tag `github.sha`.
5. `azure/webapps-deploy` gắn image vào App Service.
6. Ghi app settings (`WEBSITES_PORT=8080`, datasource, JWT, CORS) rồi restart.

Truy cập `https://<tên-app>.azurewebsites.net`; Swagger ở `/swagger-ui.html`.

## 5. Lưu ý khi chạy trên Azure

- **Chạy code**: mặc định native, không cách ly filesystem/network. App Service là môi trường chia sẻ với người dùng không tin cậy; trước khi mở công khai cần worker sandbox riêng (container/VM cách ly) hoặc tắt endpoint chạy code (`/api/submissions/run`, `/api/ast/**`, `/api/debug*`). Xem [SANDBOX_CODE_EDITOR.md](SANDBOX_CODE_EDITOR.md).
- **Rate limit**: filter đọc `X-Forwarded-For`; sau proxy của App Service cần bật `server.forward-headers-strategy=framework` nếu muốn giới hạn theo IP client thay vì IP proxy.
- **Bộ nhớ/CPU**: AST/JDI và javac chạy trong container; B1 (1.75 GB) chỉ dùng cho demo. Cân nhắc B2/S1 khi nhiều người dùng.
- **Dữ liệu**: `ddl-auto` mặc định `update`. Với production nên dùng migration có kiểm soát (Flyway/Liquibase) thay vì để Hibernate tự sửa schema.
- **Tài khoản demo**: `DataInitializer` tạo `demo` / `password123` khi database trống. Trên production cần bỏ hoặc vô hiệu hóa seeder này.
