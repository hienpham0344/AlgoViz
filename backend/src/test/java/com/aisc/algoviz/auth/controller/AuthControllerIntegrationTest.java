package com.aisc.algoviz.auth.controller;

import com.aisc.algoviz.auth.dto.AuthResponse;
import com.aisc.algoviz.auth.dto.LoginRequest;
import com.aisc.algoviz.auth.dto.RefreshTokenRequest;
import com.aisc.algoviz.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete Auth Lifecycle: Register -> Login -> Access Protected -> Refresh -> Logout -> Verify Blacklist")
    void testCompleteAuthLifecycle() throws Exception {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 100000);
        String username = "enterprise_user_" + uniqueSuffix;
        String email = "enterprise_" + uniqueSuffix + "@algoviz.com";
        String password = "EnterprisePassword123!";

        // 1. Register new user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .fullName("Enterprise Test User")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(username))
                .andReturn();

        AuthResponse regAuth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(regAuth.getToken());

        // 2. Reject duplicate registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        // 3. Login with credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail(username)
                .password(password)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse loginAuth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String accessToken = loginAuth.getToken();
        String refreshToken = loginAuth.getRefreshToken();

        // 4. Access protected endpoint /api/users/me with token -> 200 OK
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        // 5. Access protected endpoint without token -> 401 or 403 Forbidden
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());

        // 6. Refresh token rotation
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse rotatedAuth = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), AuthResponse.class);
        String rotatedAccessToken = rotatedAuth.getToken();
        String rotatedRefreshToken = rotatedAuth.getRefreshToken();

        // 7. Old refresh token cannot be reused
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());

        // 8. Logout and blacklist token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + rotatedAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(rotatedRefreshToken).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 9. Blacklisted access token should now be rejected on protected endpoint
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + rotatedAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login with invalid password should return 401 Unauthorized")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest badLogin = LoginRequest.builder()
                .usernameOrEmail("demo")
                .password("wrong_password_attempt")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }
}
