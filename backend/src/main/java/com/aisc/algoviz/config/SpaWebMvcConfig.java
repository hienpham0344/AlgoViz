package com.aisc.algoviz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Cấu hình Spring WebMvc phục vụ Static Single Page Application (SPA) của frontend.
 * Tự động phân phối tài nguyên tĩnh (HTML, JS, CSS, SVG, PNG) và điều hướng (forward)
 * các đường dẫn client-side về /index.html nhưng không can thiệp vào các API hoặc công cụ quản trị.
 */
@Configuration
public class SpaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Tuyệt đối không forward các endpoint backend, tài liệu API hoặc console quản trị về index.html
                        if (resourcePath.startsWith("api") ||
                            resourcePath.startsWith("v3/api-docs") ||
                            resourcePath.startsWith("swagger-ui") ||
                            resourcePath.startsWith("h2-console")) {
                            return null;
                        }

                        // Tuyệt đối không trả về index.html cho các tài nguyên tĩnh bị thiếu (file có đuôi mở rộng hoặc thư mục assets)
                        if (hasFileExtension(resourcePath) ||
                            resourcePath.startsWith("assets/") ||
                            resourcePath.startsWith("problem-icons/")) {
                            return null;
                        }

                        // SPA fallback: trả về index.html cho các route phía client (không chứa file extension)
                        Resource indexResource = location.createRelative("index.html");
                        return (indexResource.exists() && indexResource.isReadable()) ? indexResource : null;
                    }

                    private boolean hasFileExtension(String path) {
                        int lastSlash = path.lastIndexOf('/');
                        String filename = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
                        int lastDot = filename.lastIndexOf('.');
                        return lastDot > 0 && lastDot < filename.length() - 1;
                    }
                });
    }
}
