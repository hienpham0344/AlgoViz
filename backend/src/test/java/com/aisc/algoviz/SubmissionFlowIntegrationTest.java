package com.aisc.algoviz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubmissionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCanRegisterAndReceiveToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "integration_user",
                                  "email": "integration_user@example.com",
                                  "password": "password123",
                                  "fullName": "Integration User"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("integration_user"));
    }

    @Test
    void invalidLoginReturnsUnauthorizedInsteadOfForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "demo",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
    }

    @Test
    void userCanRunSubmitListAndDeleteSubmission() throws Exception {
        mockMvc.perform(post("/api/submissions/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "language": "python",
                                  "code": "print('ok')",
                                  "customInput": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Success"))
                .andExpect(jsonPath("$.output").value("ok"));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "demo",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        String submissionResponse = mockMvc.perform(post("/api/submissions/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "problemId", 2,
                                "language", "python",
                                "code", "def is_anagram(s, t):\n    return sorted(s) == sorted(t)"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode submission = objectMapper.readTree(submissionResponse);
        long submissionId = submission.get("id").asLong();

        mockMvc.perform(get("/api/submissions/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(submissionId));

        mockMvc.perform(delete("/api/submissions/{submissionId}", submissionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/submissions/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
