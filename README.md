# Taskay

A REST API that automatically calculates task priority scores using a custom heuristic algorithm based on urgency, importance, and deadline proximity.

Built with Java 17 + Spring Boot 3.2.

---

## The Problem

Most task management tools let you assign priority manually. The problem is that humans are poor judges of their own workload — we overreact to urgent tasks, underestimate important ones, and ignore deadlines until they're critical.

Taskay removes that judgment from the equation.

---

## How It Works

Every task receives a priority score from 0 to 10, calculated automatically:

```
score = (urgency × 0.35) + (importance × 0.35) + (deadlineScore × 0.30)
```

The deadline component uses exponential decay — the closer the due date, the higher the pressure:

```
deadlineScore = 10 × e^(−hoursUntilDeadline / 168)
```

| Hours until deadline | Deadline score |
|----------------------|----------------|
| 0 (overdue)          | 10.0           |
| 24h                  | ~8.7           |
| 72h                  | ~6.6           |
| 168h (1 week)        | ~3.7           |
| 336h (2 weeks)       | ~1.4           |

The score maps to four actionable levels:

| Score  | Level    | Action              |
|--------|----------|---------------------|
| >= 8.0 | CRITICAL | Do immediately      |
| >= 6.0 | HIGH     | Schedule today      |
| >= 4.0 | MEDIUM   | Plan for this week  |
| < 4.0  | LOW      | Delegate or defer   |

---

## Getting Started

**Requirements:** Java 17+, Maven 3.8+

```bash
git clone https://github.com/Julhaalmeida/taskay.git
cd taskay
mvn spring-boot:run
```

API available at `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

H2 console (dev): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:scheduler`
- Username: `sa` / Password: *(empty)*

---

## Endpoints

| Method | Endpoint                        | Description                          |
|--------|---------------------------------|--------------------------------------|
| POST   | /api/v1/tasks                   | Create task (auto-calculates score)  |
| GET    | /api/v1/tasks                   | List all tasks sorted by priority    |
| GET    | /api/v1/tasks/{id}              | Get task by ID                       |
| PUT    | /api/v1/tasks/{id}              | Update task and recalculate score    |
| PATCH  | /api/v1/tasks/{id}/status       | Update task status                   |
| DELETE | /api/v1/tasks/{id}              | Delete task                          |
| GET    | /api/v1/tasks/status/{status}   | Filter by status                     |
| GET    | /api/v1/tasks/overdue           | List overdue tasks                   |
| GET    | /api/v1/tasks/due-soon?hours=   | Tasks due within N hours             |
| GET    | /api/v1/tasks/summary           | Schedule overview + recommendation   |
| POST   | /api/v1/tasks/recalculate       | Refresh all scores                   |

### Example

```json
POST /api/v1/tasks
{
  "title": "Deploy to production",
  "urgency": 9,
  "importance": 8,
  "deadline": "2025-01-15T18:00:00"
}
```

```json
{
  "id": 1,
  "title": "Deploy to production",
  "priorityScore": 8.75,
  "priorityLevel": "CRITICAL",
  "hoursUntilDeadline": 6,
  "status": "PENDING"
}
```

---

## Architecture

```
src/
├── controller/       TaskController.java
├── service/          TaskService.java
│                     PriorityAlgorithm.java
├── repository/       TaskRepository.java
├── model/            Task.java
├── dto/              TaskDTO.java
└── exception/        GlobalExceptionHandler.java
```

- `PriorityAlgorithm` is a pure component with no database access — fully testable in isolation
- DTO pattern prevents entity leakage into the API layer
- Read operations use `@Transactional(readOnly = true)`
- Custom JPQL queries keep business logic out of the service layer

---

## Tests

```bash
mvn test
```

- `PriorityAlgorithmTest` — 8 unit tests (edge cases, boundary scores, parameterized classification)
- `TaskControllerIntegrationTest` — 6 integration tests with MockMvc

---

## Stack

| Layer         | Technology                   |
|---------------|------------------------------|
| Language      | Java 17                      |
| Framework     | Spring Boot 3.2              |
| Persistence   | Spring Data JPA + H2         |
| Validation    | Jakarta Bean Validation      |
| Documentation | SpringDoc OpenAPI (Swagger)  |
| Testing       | JUnit 5 + AssertJ + MockMvc  |
| Build         | Maven                        |

---

## Possible Extensions

- PostgreSQL/MySQL for production
- Spring Security + JWT
- Scheduled score recalculation (`@Scheduled`)
- Notifications for CRITICAL tasks
- Completion rate statistics by priority level
- Task categories and tags

---

MIT License
