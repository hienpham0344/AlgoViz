package com.aisc.algoviz.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cấu hình Swagger / OpenAPI 3.0 Documentation.
 * Cho phép hiển thị giao diện UI tại: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI algoVizOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("AlgoViz Platform REST API Engine")
                        .description("Tài liệu đặc tả toàn bộ RESTful API cho nền tảng trực quan hóa thuật toán và chấm bài tự động AlgoViz.\n\n"
                                + "### Các module API chính:\n"
                                + "- **Problems (Core & Studio)**: Kho hơn 4,000 bài toán LeetCode với test cases, constraints, và solutions đa ngôn ngữ.\n"
                                + "- **Problem Management (/api/v1/problems)**: API quản lý bài toán nâng cao có lọc, phân trang PageResponse.\n"
                                + "- **Submissions & Execution**: Sandbox chạy code an toàn hỗ trợ Python, Java, C++, JS và lưu lịch sử nộp bài.\n"
                                + "- **Authentication & Authorization**: Đăng ký, đăng nhập JWT, rate limiting, refresh token và blacklist.\n"
                                + "- **User Management**: Hồ sơ cá nhân, điểm thưởng (gamification) và streak.\n"
                                + "- **AI Code Analysis**: Phân tích Big-O Time & Space complexity và đề xuất tối ưu hóa.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AlgoViz Dev Team")
                                .email("support@algoviz.aisc.com")
                                .url("http://localhost:5173"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("https://api.algoviz.internal").description("Production / Staging Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập Bearer Token theo định dạng: `Bearer <access_token>`")));
    }
}
