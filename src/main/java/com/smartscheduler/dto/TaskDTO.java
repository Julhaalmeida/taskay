package com.smartscheduler.dto;

import com.smartscheduler.model.Task;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class TaskDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 100)
        private String title;

        @Size(max = 500)
        private String description;

        @NotNull
        @Min(1) @Max(10)
        private Integer urgency;

        @NotNull
        @Min(1) @Max(10)
        private Integer importance;

        @NotNull
        @Future
        private LocalDateTime deadline;

        private Task.TaskStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private Integer urgency;
        private Integer importance;
        private LocalDateTime deadline;
        private Task.TaskStatus status;
        private Double priorityScore;
        private String priorityLevel;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private Long hoursUntilDeadline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleSummary {
        private long totalTasks;
        private long pendingTasks;
        private long inProgressTasks;
        private long completedTasks;
        private long overdueTasks;
        private Task.TaskStatus mostUrgentStatus;
        private String recommendation;
    }
}
