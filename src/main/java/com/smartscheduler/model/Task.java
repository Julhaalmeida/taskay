package com.smartscheduler.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Column(nullable = false)
    private String title;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Urgency is required")
    @Min(value = 1, message = "Urgency must be between 1 and 10")
    @Max(value = 10, message = "Urgency must be between 1 and 10")
    @Column(nullable = false)
    private Integer urgency;

    @NotNull(message = "Importance is required")
    @Min(value = 1, message = "Importance must be between 1 and 10")
    @Max(value = 10, message = "Importance must be between 1 and 10")
    @Column(nullable = false)
    private Integer importance;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be a future date")
    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    /**
     * Priority score calculated by the Smart Scheduler Algorithm.
     * Formula: (urgency * 0.4) + (importance * 0.4) + (deadlineScore * 0.2)
     * Stored for sorting and querying efficiency.
     */
    @Column(name = "priority_score")
    private Double priorityScore;

    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
