package com.smartscheduler.service;

import com.smartscheduler.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PriorityAlgorithm Unit Tests")
class PriorityAlgorithmTest {

    private PriorityAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new PriorityAlgorithm();
    }

    @Test
    @DisplayName("Critical task: high urgency + high importance + deadline in 2h → score >= 8.0")
    void shouldScoreCriticalForHighUrgencyHighImportanceNearDeadline() {
        Task task = buildTask(10, 10, LocalDateTime.now().plusHours(2));
        double score = algorithm.calculate(task);
        assertThat(score).isGreaterThanOrEqualTo(8.0);
        assertThat(algorithm.getPriorityLevel(score)).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Low task: low urgency + low importance + far deadline → score < 4.0")
    void shouldScoreLowForLowUrgencyLowImportanceFarDeadline() {
        Task task = buildTask(1, 1, LocalDateTime.now().plusDays(30));
        double score = algorithm.calculate(task);
        assertThat(score).isLessThan(4.0);
        assertThat(algorithm.getPriorityLevel(score)).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Overdue task should receive maximum deadline pressure (score 10)")
    void shouldApplyMaxPressureForOverdueDeadline() {
        Task task = buildTask(5, 5, LocalDateTime.now().minusHours(1));
        double score = algorithm.calculate(task);
        // With max deadline score (10) and moderate urgency/importance:
        // (5×0.35) + (5×0.35) + (10×0.30) = 1.75 + 1.75 + 3.0 = 6.5
        assertThat(score).isGreaterThanOrEqualTo(6.0);
    }

    @Test
    @DisplayName("Score should always be between 0.0 and 10.0")
    void scoreShouldBeWithinBounds() {
        Task low  = buildTask(1, 1, LocalDateTime.now().plusDays(365));
        Task high = buildTask(10, 10, LocalDateTime.now().plusMinutes(1));

        assertThat(algorithm.calculate(low)).isBetween(0.0, 10.0);
        assertThat(algorithm.calculate(high)).isBetween(0.0, 10.0);
    }

    @ParameterizedTest(name = "score={2} → level={3}")
    @CsvSource({
        "9.5, CRITICAL",
        "8.0, CRITICAL",
        "7.9, HIGH",
        "6.0, HIGH",
        "5.9, MEDIUM",
        "4.0, MEDIUM",
        "3.9, LOW",
        "0.0, LOW"
    })
    @DisplayName("Priority level classification should match thresholds")
    void shouldClassifyPriorityLevelCorrectly(double score, String expectedLevel) {
        assertThat(algorithm.getPriorityLevel(score)).isEqualTo(expectedLevel);
    }

    @Test
    @DisplayName("Closer deadline should produce higher score than far deadline (same urgency/importance)")
    void closerDeadlineShouldProduceHigherScore() {
        Task taskSoon = buildTask(7, 7, LocalDateTime.now().plusHours(12));
        Task taskFar  = buildTask(7, 7, LocalDateTime.now().plusDays(14));

        double scoreSoon = algorithm.calculate(taskSoon);
        double scoreFar  = algorithm.calculate(taskFar);

        assertThat(scoreSoon).isGreaterThan(scoreFar);
    }

    @Test
    @DisplayName("getRecommendation should return non-null, non-empty string")
    void recommendationShouldBeNonEmpty() {
        Task task = buildTask(8, 9, LocalDateTime.now().plusHours(6));
        task.setPriorityScore(algorithm.calculate(task));
        assertThat(algorithm.getRecommendation(task)).isNotBlank();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Task buildTask(int urgency, int importance, LocalDateTime deadline) {
        return Task.builder()
                .title("Test Task")
                .urgency(urgency)
                .importance(importance)
                .deadline(deadline)
                .status(Task.TaskStatus.PENDING)
                .build();
    }
}
