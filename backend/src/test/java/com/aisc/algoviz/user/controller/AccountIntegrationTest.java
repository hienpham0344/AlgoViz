package com.aisc.algoviz.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "algoviz.security.rate-limit.enabled=false")
@AutoConfigureMockMvc
class AccountIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private JsonNode register() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 12);
        String body = mapper.writeValueAsString(Map.of("username", username,
                "email", username + "@example.com", "password", "Original123!", "fullName", "Original Name"));
        return mapper.readTree(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test void protectedApiReturnsUnauthorizedForExpiredSessions() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test void profileEditsPersistAcrossReads() throws Exception {
        JsonNode auth = register();
        String token = auth.path("token").asText();
        mvc.perform(put("/api/users/profile").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "fullName", "Updated Name", "bio", "Learning algorithms", "location", "Hanoi",
                        "roleTitle", "Student", "target", "Master DSA", "email", auth.path("user").path("email").asText()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated Name"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bio").value("Learning algorithms"))
                .andExpect(jsonPath("$.location").value("Hanoi")).andExpect(jsonPath("$.target").value("Master DSA"));
    }

    @Test void profileRejectsDuplicateEmailAndInvalidData() throws Exception {
        JsonNode first = register();
        JsonNode second = register();
        String token = second.path("token").asText();
        mvc.perform(put("/api/users/profile").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", first.path("user").path("email").asText()))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/users/profile").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void submissionsCanOnlyBeDeletedByTheirOwner() throws Exception {
        JsonNode owner = register();
        JsonNode other = register();
        JsonNode catalog = mapper.readTree(mvc.perform(get("/api/problems")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String body = mapper.writeValueAsString(Map.of("problemId", catalog.get(0).path("id").asLong(),
                "language", "unsupported", "code", "invalid code"));
        JsonNode submission = mapper.readTree(mvc.perform(post("/api/submissions/submit")
                .header("Authorization", "Bearer " + owner.path("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String endpoint = "/api/submissions/" + submission.path("id").asLong();
        mvc.perform(delete(endpoint).header("Authorization", "Bearer " + other.path("token").asText()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(endpoint).header("Authorization", "Bearer " + owner.path("token").asText()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/submissions/history").header("Authorization", "Bearer " + owner.path("token").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void learningProgressPersistsAndIsPrivatePerAccount() throws Exception {
        JsonNode owner = register();
        JsonNode other = register();
        String token = owner.path("token").asText();
        String body = "{\"courseId\":\"sorting-masterclass\",\"lessonId\":1,\"completed\":true}";
        mvc.perform(put("/api/users/learning-progress").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/learning-progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].courseId").value("sorting-masterclass"))
                .andExpect(jsonPath("$[0].lessonId").value(1));
        mvc.perform(get("/api/users/learning-progress").header("Authorization", "Bearer " + other.path("token").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/users/learning-progress").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("true", "false"))).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/learning-progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void passwordChangeVerifiesOldPasswordAndRevokesRefreshTokens() throws Exception {
        JsonNode auth = register();
        String token = auth.path("token").asText();
        mvc.perform(put("/api/users/password").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"Changed123!\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/users/password").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Original123!\",\"newPassword\":\"Changed123!\"}"))
                .andExpect(status().isNoContent());
        String username = auth.path("user").path("username").asText();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("usernameOrEmail", username, "password", "Original123!"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("usernameOrEmail", username, "password", "Changed123!"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("refreshToken", auth.path("refreshToken").asText()))))
                .andExpect(status().is4xxClientError());
    }
}
