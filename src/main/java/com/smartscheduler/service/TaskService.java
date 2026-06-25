package com.smartscheduler.service;

import com.smartscheduler.dto.TaskDTO;
import com.smartscheduler.exception.TaskNotFoundException;
import com.smartscheduler.model.Task;
import com.smartscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final PriorityAlgorithm priorityAlgorithm;

    @Transactional
    public TaskDTO.Response create(TaskDTO.Request request) {
        log.info("Creating task: {}", request.getTitle());

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .urgency(request.getUrgency())
                .importance(request.getImportance())
                .deadline(request.getDeadline())
                .status(Task.TaskStatus.PENDING)
                .build();

        task.setPriorityScore(priorityAlgorithm.calculate(task));

        Task saved = taskRepository.save(task);
        log.info("Task created with id={} and score={}", saved.getId(), saved.getPriorityScore());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO.Response> findAll() {
        return taskRepository.findAllByOrderByPriorityScoreDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDTO.Response findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public TaskDTO.Response update(Long id, TaskDTO.Request request) {
        Task task = getOrThrow(id);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setUrgency(request.getUrgency());
        task.setImportance(request.getImportance());
        task.setDeadline(request.getDeadline());

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            if (request.getStatus() == Task.TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }

        task.setPriorityScore(priorityAlgorithm.calculate(task));
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskDTO.Response updateStatus(Long id, Task.TaskStatus status) {
        Task task = getOrThrow(id);
        task.setStatus(status);
        if (status == Task.TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = getOrThrow(id);
        taskRepository.delete(task);
        log.info("Task deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO.Response> findByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatusOrderByPriorityScoreDesc(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO.Response> findOverdue() {
        return taskRepository.findOverdueTasks(LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO.Response> findDueSoon(int hours) {
        LocalDateTime now = LocalDateTime.now();
        return taskRepository.findTasksDueSoon(now, now.plusHours(hours))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDTO.ScheduleSummary getSummary() {
        long total      = taskRepository.count();
        long pending    = taskRepository.countByStatus(Task.TaskStatus.PENDING);
        long inProgress = taskRepository.countByStatus(Task.TaskStatus.IN_PROGRESS);
        long completed  = taskRepository.countByStatus(Task.TaskStatus.COMPLETED);
        long overdue    = taskRepository.findOverdueTasks(LocalDateTime.now()).size();

        String recommendation = buildRecommendation(pending, inProgress, overdue);

        return TaskDTO.ScheduleSummary.builder()
                .totalTasks(total)
                .pendingTasks(pending)
                .inProgressTasks(inProgress)
                .completedTasks(completed)
                .overdueTasks(overdue)
                .recommendation(recommendation)
                .build();
    }

    // ── Recalculate scores (useful for scheduled jobs) ─────────────────────────

    @Transactional
    public int recalculateAllScores() {
        List<Task> tasks = taskRepository.findAll();
        tasks.forEach(t -> t.setPriorityScore(priorityAlgorithm.calculate(t)));
        taskRepository.saveAll(tasks);
        log.info("Recalculated scores for {} tasks", tasks.size());
        return tasks.size();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Task getOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
    }

    private TaskDTO.Response toResponse(Task task) {
        long hoursUntilDeadline = Duration.between(LocalDateTime.now(), task.getDeadline()).toHours();
        double score = task.getPriorityScore() != null ? task.getPriorityScore() : 0.0;

        return TaskDTO.Response.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .urgency(task.getUrgency())
                .importance(task.getImportance())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .priorityScore(score)
                .priorityLevel(priorityAlgorithm.getPriorityLevel(score))
                .hoursUntilDeadline(hoursUntilDeadline)
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private String buildRecommendation(long pending, long inProgress, long overdue) {
        if (overdue > 0)    return "You have " + overdue + " overdue task(s). Address them immediately!";
        if (inProgress > 3) return "Too many tasks in progress. Focus and complete before starting new ones.";
        if (pending > 10)   return "High task backlog. Consider prioritizing or delegating.";
        return "Your schedule looks healthy. Keep it up!";
    }
}
