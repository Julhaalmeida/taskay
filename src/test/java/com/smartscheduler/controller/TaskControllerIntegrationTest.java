package com.smartscheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartscheduler.dto.TaskDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("TaskController Integration Tests")
class TaskControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/tasks → 201 Created with priority score")
    void shouldCreateTaskAndReturnPriorityScore() throws Exception {
        var request = buildRequest("Deploy to production", 9, 8);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Deploy to production"))
                .andExpect(jsonPath("$.priorityScore").isNumber())
                .andExpect(jsonPath("$.priorityLevel").isString())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks → returns list sorted by priority desc")
    void shouldReturnTasksSortedByPriority() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("Low task", 1, 1))));
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("Critical task", 10, 10))));

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].priorityScore").value(greaterThan(jsonPath("$[1].priorityScore"))));
    }

    @Test
    @DisplayName("POST /api/v1/tasks with invalid data → 400 Bad Request")
    void shouldReturn400ForInvalidTask() throws Exception {
        var invalid = TaskDTO.Request.builder()
                .title("X")   // too short
                .urgency(15)  // out of range
                .importance(0) // out of range
                .deadline(LocalDateTime.now().minusDays(1)) // past date
                .build();

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} with unknown id → 404 Not Found")
    void shouldReturn404ForUnknownTask() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status → updates status to COMPLETED")
    void shouldUpdateTaskStatus() throws Exception {
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Finish report", 7, 8))))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/v1/tasks/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/tasks/summary → returns schedule summary")
    void shouldReturnScheduleSummary() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").isNumber())
                .andExpect(jsonPath("$.recommendation").isString());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private TaskDTO.Request buildRequest(String title, int urgency, int importance) {
        return TaskDTO.Request.builder()
                .title(title)
                .description("Test task: " + title)
                .urgency(urgency)
                .importance(importance)
                .deadline(LocalDateTime.now().plusDays(3))
                .build();
    }
}
