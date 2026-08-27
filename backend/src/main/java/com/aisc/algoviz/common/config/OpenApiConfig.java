package com.aisc.algoviz.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger / OpenAPI 3.0 Documentation.
 * Cho phép hiển thị giao diện UI tại: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI algoVizOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AlgoViz REST API Documentation")
                        .description("Tài liệu đặc tả toàn bộ RESTful API cho nền tảng trực quan hóa thuật toán AlgoViz.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AlgoViz Dev Team")
                                .email("support@algoviz.aisc.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
