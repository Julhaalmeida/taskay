package com.smartscheduler.repository;

import com.smartscheduler.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByPriorityScoreDesc();

    List<Task> findByStatus(Task.TaskStatus status);

    List<Task> findByStatusOrderByPriorityScoreDesc(Task.TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.deadline < :now AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now);

    @Query("SELECT t FROM Task t WHERE t.deadline BETWEEN :now AND :soon AND t.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY t.priorityScore DESC")
    List<Task> findTasksDueSoon(@Param("now") LocalDateTime now, @Param("soon") LocalDateTime soon);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatus(@Param("status") Task.TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.urgency >= :minUrgency AND t.importance >= :minImportance ORDER BY t.priorityScore DESC")
    List<Task> findHighPriorityTasks(@Param("minUrgency") int minUrgency, @Param("minImportance") int minImportance);
}
