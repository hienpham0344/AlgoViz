package com.aisc.algoviz.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller phục vụ Single Page Application (SPA) index.html
 * cho trang chủ và các đường dẫn client-side của React.
 * Trả về trực tiếp tài nguyên HTML với Content-Type text/html.
 */
@Controller
public class SpaForwardController {

    private final Resource indexHtml = new ClassPathResource("static/index.html");

    @GetMapping(value = {
        "/",
        "/problems",
        "/problems/**",
        "/problem-details",
        "/visualizer",
        "/visualizer/**",
        "/analyze",
        "/analyze/**",
        "/learn",
        "/learn/**",
        "/lesson",
        "/history",
        "/history/**",
        "/profile",
        "/profile/**",
        "/login",
        "/register"
    }, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> serveIndexHtml() {
        if (!indexHtml.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }
}
