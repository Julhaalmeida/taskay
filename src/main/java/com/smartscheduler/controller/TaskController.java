package com.smartscheduler.controller;

import com.smartscheduler.dto.TaskDTO;
import com.smartscheduler.model.Task;
import com.smartscheduler.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Smart Task Scheduler API — manage and prioritize your tasks intelligently")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a task", description = "Creates a new task and automatically calculates its priority score")
    public ResponseEntity<TaskDTO.Response> create(@Valid @RequestBody TaskDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all tasks", description = "Returns all tasks sorted by priority score (highest first)")
    public ResponseEntity<List<TaskDTO.Response>> findAll() {
        return ResponseEntity.ok(taskService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskDTO.Response> findById(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task", description = "Updates task details and recalculates priority score")
    public ResponseEntity<TaskDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskDTO.Request request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status", description = "Changes the status of a task (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)")
    public ResponseEntity<TaskDTO.Response> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Task.TaskStatus status = Task.TaskStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter by status", description = "Returns tasks filtered by status, sorted by priority")
    public ResponseEntity<List<TaskDTO.Response>> findByStatus(
            @PathVariable Task.TaskStatus status) {
        return ResponseEntity.ok(taskService.findByStatus(status));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue tasks", description = "Returns all tasks past their deadline that are not completed")
    public ResponseEntity<List<TaskDTO.Response>> findOverdue() {
        return ResponseEntity.ok(taskService.findOverdue());
    }

    @GetMapping("/due-soon")
    @Operation(summary = "Get tasks due soon", description = "Returns tasks due within the specified number of hours (default: 24h)")
    public ResponseEntity<List<TaskDTO.Response>> findDueSoon(
            @Parameter(description = "Hours window (default 24)") @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(taskService.findDueSoon(hours));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get schedule summary", description = "Returns an overview of your task schedule with recommendations")
    public ResponseEntity<TaskDTO.ScheduleSummary> getSummary() {
        return ResponseEntity.ok(taskService.getSummary());
    }

    @PostMapping("/recalculate")
    @Operation(summary = "Recalculate all scores", description = "Re-runs the priority algorithm for all tasks (useful after time passes)")
    public ResponseEntity<Map<String, Object>> recalculate() {
        int count = taskService.recalculateAllScores();
        return ResponseEntity.ok(Map.of(
                "message", "Priority scores recalculated successfully",
                "tasksUpdated", count
        ));
    }
}
