package com.aisc.algoviz.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SpaWebMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Root URL (/) serves index.html with 200 OK and HTML content type")
    void testRootServesIndexHtml() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("root")));
    }

    @Test
    @DisplayName("Direct SPA route (/problems) serves index.html")
    void testSpaRouteServesIndexHtml() throws Exception {
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("root")));
    }

    @Test
    @DisplayName("Deep SPA route (/visualizer/sorting) serves index.html")
    void testDeepSpaRouteServesIndexHtml() throws Exception {
        mockMvc.perform(get("/visualizer/sorting"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("root")));
    }

    @Test
    @DisplayName("Static assets like favicon.svg are served with correct media type")
    void testStaticAssetServed() throws Exception {
        mockMvc.perform(get("/favicon.svg"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")));
    }

    @Test
    @DisplayName("Backend API GET /api/problems returns JSON and is NOT intercepted by SPA router")
    void testApiEndpointNotIntercepted() throws Exception {
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(not(containsString("<!doctype html>"))));
    }

    @Test
    @DisplayName("Protected API GET /api/nonexistent-xyz without auth returns 401 Unauthorized")
    void testUnauthenticatedApiReturns401() throws Exception {
        mockMvc.perform(get("/api/nonexistent-xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("<div id=\"root\">"))));
    }

    @Test
    @DisplayName("Public API GET /api/problems/9999999 returns 404 Not Found, not intercepted by SPA")
    void testNonExistentProblemReturns404() throws Exception {
        mockMvc.perform(get("/api/problems/9999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(not(containsString("<div id=\"root\">"))));
    }

    @Test
    @DisplayName("Protected API GET /api/users/me without credentials returns 401 Unauthorized")
    void testProtectedApiReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Swagger OpenAPI documentation is accessible and not intercepted")
    void testSwaggerDocumentationAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Missing static JavaScript asset (/assets/missing-file.js) returns 404 Not Found")
    void testMissingStaticJsAssetReturns404() throws Exception {
        mockMvc.perform(get("/assets/missing-file.js"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("<div id=\"root\">"))));
    }

    @Test
    @DisplayName("Missing static CSS file (/styles/missing.css) returns 404 Not Found")
    void testMissingStaticCssReturns404() throws Exception {
        mockMvc.perform(get("/styles/missing.css"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("<div id=\"root\">"))));
    }

    @Test
    @DisplayName("Missing static image file (/favicon-missing.ico) returns 404 Not Found")
    void testMissingStaticImageReturns404() throws Exception {
        mockMvc.perform(get("/favicon-missing.ico"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("<div id=\"root\">"))));
    }

    @Test
    @DisplayName("Dynamic unlisted client SPA route (/dashboard) serves index.html")
    void testDynamicClientRouteServesIndexHtml() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("root")));
    }
}
