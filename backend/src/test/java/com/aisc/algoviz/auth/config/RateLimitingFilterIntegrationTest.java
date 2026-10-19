package com.aisc.algoviz.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitingFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService.reset();
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests with headers when rate limit is exceeded")
    void testRateLimitingExceeded() throws Exception {
        String testIp = "203.0.113.199";
        Map<String, String> dummyBody = Map.of(
                "usernameOrEmail", "nonexistent",
                "password", "invalidpassword"
        );

        // Limit for /api/auth/* is 20 requests per minute
        // Send enough requests in rapid succession to exceed capacity + refill
        for (int i = 0; i < 35; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dummyBody)));
        }

        // 21st request MUST be rejected with HTTP 429
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dummyBody)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value(4290))
                .andExpect(jsonPath("$.message").value("Quá nhiều yêu cầu gửi lên hệ thống. Vui lòng thử lại sau."));
    }
}
