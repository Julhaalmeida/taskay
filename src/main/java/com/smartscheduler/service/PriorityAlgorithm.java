package com.smartscheduler.service;

import com.smartscheduler.model.Task;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Smart Priority Algorithm
 *
 * Calculates a priority score (0–10) for each task using a weighted formula
 * inspired by the Eisenhower Matrix and time-pressure heuristics:
 *
 *   score = (urgency × 0.35) + (importance × 0.35) + (deadlineScore × 0.30)
 *
 * Deadline score decreases exponentially as the deadline approaches,
 * creating urgency pressure without ignoring long-term importance.
 *
 * Priority Levels:
 *   CRITICAL  → score >= 8.0  (Do immediately)
 *   HIGH      → score >= 6.0  (Schedule today)
 *   MEDIUM    → score >= 4.0  (Plan this week)
 *   LOW       → score < 4.0   (Delegate or defer)
 */
@Component
public class PriorityAlgorithm {

    private static final double URGENCY_WEIGHT     = 0.35;
    private static final double IMPORTANCE_WEIGHT  = 0.35;
    private static final double DEADLINE_WEIGHT    = 0.30;

    private static final double SCORE_CRITICAL = 8.0;
    private static final double SCORE_HIGH     = 6.0;
    private static final double SCORE_MEDIUM   = 4.0;

    /**
     * Calculates the priority score for a task.
     *
     * @param task the task to evaluate
     * @return score between 0.0 and 10.0
     */
    public double calculate(Task task) {
        double urgencyScore    = normalize(task.getUrgency());
        double importanceScore = normalize(task.getImportance());
        double deadlineScore   = calculateDeadlineScore(task.getDeadline());

        double raw = (urgencyScore    * URGENCY_WEIGHT)
                   + (importanceScore * IMPORTANCE_WEIGHT)
                   + (deadlineScore   * DEADLINE_WEIGHT);

        return round(raw);
    }

    /**
     * Returns a human-readable priority level.
     */
    public String getPriorityLevel(double score) {
        if (score >= SCORE_CRITICAL) return "CRITICAL";
        if (score >= SCORE_HIGH)     return "HIGH";
        if (score >= SCORE_MEDIUM)   return "MEDIUM";
        return "LOW";
    }

    /**
     * Returns a contextual recommendation based on score and deadline.
     */
    public String getRecommendation(Task task) {
        double score = task.getPriorityScore() != null ? task.getPriorityScore() : calculate(task);
        long hours = Duration.between(LocalDateTime.now(), task.getDeadline()).toHours();

        if (score >= SCORE_CRITICAL && hours <= 24) return "⚠️ Drop everything — this needs immediate attention!";
        if (score >= SCORE_CRITICAL)                return "🔴 Critical task — start today.";
        if (score >= SCORE_HIGH && hours <= 48)     return "🟠 High priority — address within 48 hours.";
        if (score >= SCORE_HIGH)                    return "🟡 High priority — schedule for this week.";
        if (score >= SCORE_MEDIUM)                  return "🟢 Medium priority — plan for the near future.";
        return "🔵 Low priority — delegate or defer if needed.";
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Normalizes a 1–10 integer to a 0–10 double scale. */
    private double normalize(int value) {
        return Math.min(10.0, Math.max(0.0, value));
    }

    /**
     * Converts a deadline to a 0–10 urgency score.
     * Uses an exponential decay: the closer the deadline, the higher the score.
     *
     *   deadlineScore = 10 × e^(−hoursUntilDeadline / 168)
     *
     * 168 hours = 1 week — tasks due in a week score ~3.7, in 24h score ~8.7.
     */
    private double calculateDeadlineScore(LocalDateTime deadline) {
        long hours = Duration.between(LocalDateTime.now(), deadline).toHours();
        if (hours <= 0) return 10.0; // already overdue → maximum pressure
        double decay = Math.exp(-hours / 168.0);
        return round(10.0 * decay);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
