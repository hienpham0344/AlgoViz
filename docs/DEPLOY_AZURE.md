# Triển khai AlgoViz trên Azure

Mô hình: GitHub Actions build Docker image chung cho frontend và backend → đẩy lên Azure Container Registry (ACR) → cập nhật Azure Container App `algoviz-backend` trong resource group `rg-algoviz`. Xác thực bằng OIDC; không dùng ACR Tasks hoặc lưu mật khẩu Azure trong repo. Database: Azure Database for PostgreSQL.

Tài liệu này là hướng dẫn cấu hình; các bước `az` bên dưới chỉ chạy **một lần** sau khi review workflow, trên máy có Azure CLI và đã `az login`. Chưa được kiểm chứng trên subscription Azure thực tế. Thay `<tên-acr>` bằng ACR đang sử dụng.

## 1. Chuẩn bị tài nguyên

```bash
ACR_NAME='<tên-acr>'
az containerapp show --resource-group rg-algoviz --name algoviz-backend \
  --query '{name:name, ingress:properties.configuration.ingress, identity:identity}'
az acr show --name "$ACR_NAME" --query '{id:id, loginServer:loginServer}'
```

Kiểm tra Container App đã tồn tại, ingress trỏ tới cổng 8080 và có thể kết nối PostgreSQL. Workflow không tạo tài nguyên Azure.

Container App `algoviz-backend` cần tồn tại, có ingress tới cổng 8080 và quyền kéo image từ ACR. Sau khi tạo Container App trong một Container Apps environment, cấu hình managed identity:

```bash
ACR_NAME='<tên-acr>'
PRINCIPAL_ID=$(az containerapp identity assign --resource-group rg-algoviz --name algoviz-backend --system-assigned --query principalId -o tsv)
ACR_ID=$(az acr show --name "$ACR_NAME" --query id -o tsv)
az role assignment create --assignee "$PRINCIPAL_ID" --scope "$ACR_ID" --role AcrPull
az containerapp registry set --resource-group rg-algoviz --name algoviz-backend \
  --server "$ACR_NAME.azurecr.io" --identity system
```

Nếu ACR bật repository permissions bằng ABAC, thay `AcrPull` bằng `Container Registry Repository Reader`; workflow cần `Container Registry Repository Writer` thay cho `AcrPush`.

## 2. App registration cho GitHub OIDC

```bash
APP_ID=$(az ad app create --display-name github-algoviz --query appId -o tsv)
az ad sp create --id "$APP_ID"

# Job deploy dùng GitHub environment `production`, nên OIDC subject phải khớp environment này.
# Giới hạn environment chỉ cho nhánh main trong GitHub Settings → Environments → production.
az ad app federated-credential create --id "$APP_ID" --parameters '{
  "name": "github-production",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:hienpham0344/AlgoViz:environment:production",
  "audiences": ["api://AzureADTokenExchange"]
}'
```

Cấp quyền cho service principal (thay ID bằng giá trị của subscription):

```bash
APP_PRINCIPAL_ID=$(az ad sp show --id "$APP_ID" --query id -o tsv)
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
APP_ID_SCOPE="/subscriptions/$SUBSCRIPTION_ID/resourceGroups/rg-algoviz/providers/Microsoft.App/containerApps/algoviz-backend"
az role assignment create --assignee "$APP_PRINCIPAL_ID" --scope "$APP_ID_SCOPE" --role "Container Apps Contributor"
az role assignment create --assignee "$APP_PRINCIPAL_ID" --scope "$ACR_ID" --role AcrPush
```

Workflow cần quyền cập nhật Container App và push ACR. Managed identity của Container App cần quyền pull ACR. Nếu ACR ở resource group khác, dùng ID thực của ACR cho hai role assignments.

## 3. Cấu hình GitHub

Repository → **Settings → Secrets and variables → Actions**:

| Loại | Tên | Giá trị |
|---|---|---|
| Variable | `AZURE_CLIENT_ID` | `$APP_ID` |
| Variable | `AZURE_TENANT_ID` | `az account show --query tenantId -o tsv` |
| Variable | `AZURE_SUBSCRIPTION_ID` | `az account show --query id -o tsv` |
| Variable | `AZURE_CONTAINER_REGISTRY` | `<tên-acr>.azurecr.io` |
| Secret | `APP_ALGOVIZ_JWT_SECRET` | `openssl rand -hex 32` (≥ 32 ký tự) |
| Secret | `APP_SPRING_DATASOURCE_URL` | `jdbc:postgresql://algoviz-pg.postgres.database.azure.com:5432/algoviz?sslmode=require` |
| Secret | `APP_SPRING_DATASOURCE_USERNAME` | `algoviz` |
| Secret | `APP_SPRING_DATASOURCE_PASSWORD` | mật khẩu PostgreSQL ở bước 1 |

Tạo environment `production` (Settings → Environments) và giới hạn deployment branches vào `main`; có thể thêm required reviewers. Nếu đã tạo federated credential với subject `repo:hienpham0344/AlgoViz:ref:refs/heads/main`, tạo thêm credential với subject `repo:hienpham0344/AlgoViz:environment:production` để `azure/login` xác thực được.

## 4. Workflow

`.github/workflows/deploy-azure.yml` chạy khi push `main` hoặc `workflow_dispatch`. Job bị bỏ qua cho đến khi variable `AZURE_CONTAINER_REGISTRY` được đặt; các giá trị còn thiếu sẽ làm bước kiểm tra cấu hình thất bại. Review workflow trước khi merge lên `main`. Các bước:

1. Kiểm tra đủ biến/bí mật.
2. Build image bằng `Dockerfile` (Node build → Maven package → JDK 21 runtime).
3. Chạy container tạm và chờ `/api/ast/health` trả 200 (smoke test).
4. `az login` bằng OIDC, `az acr login` và push image tag `github.sha`.
5. Lưu JWT và datasource thành Container Apps secrets, gắn environment variables qua `secretref:`.
6. `az containerapp update` cập nhật image tag và CORS theo ingress FQDN, tạo revision mới.

Truy cập `https://<ingress-fqdn>`; Swagger ở `/swagger-ui.html`. Workflow chỉ cập nhật Container App đã tồn tại, không tạo resource hoặc thay đổi ingress, scale, database.

## 5. Lưu ý khi chạy trên Azure

- **Chạy code**: mặc định native, không cách ly filesystem/network. Container App chạy code người dùng không tin cậy; trước khi mở công khai cần worker sandbox riêng (container/VM cách ly) hoặc tắt endpoint chạy code (`/api/submissions/run`, `/api/ast/**`, `/api/debug*`). Xem [SANDBOX_CODE_EDITOR.md](SANDBOX_CODE_EDITOR.md).
- **Rate limit**: filter đọc `X-Forwarded-For`; sau proxy của Container Apps cần bật `server.forward-headers-strategy=framework` nếu muốn giới hạn theo IP client thay vì IP proxy.
- **Bộ nhớ/CPU**: AST/JDI và javac chạy trong container; đặt tài nguyên phù hợp cho Container App.
- **Dữ liệu**: `ddl-auto` mặc định `update`. Với production nên dùng migration có kiểm soát (Flyway/Liquibase) thay vì để Hibernate tự sửa schema.
- **Tài khoản demo**: `DataInitializer` tạo `demo` / `password123` khi database trống. Trên production cần bỏ hoặc vô hiệu hóa seeder này.
