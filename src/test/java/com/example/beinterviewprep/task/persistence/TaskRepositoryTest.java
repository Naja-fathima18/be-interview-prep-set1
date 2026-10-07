package com.example.beinterviewprep.task.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryTest {

  @Autowired private TaskRepository taskRepository;

  @Test
  void persistsTaskWithAllFields() {
    Instant createdAt = Instant.parse("2026-10-07T09:00:00Z");
    Task saved =
        taskRepository.saveAndFlush(
            new Task(
                "Write report",
                "Quarterly numbers",
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 10, 31),
                createdAt));

    Task found = taskRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getTitle()).isEqualTo("Write report");
    assertThat(found.getDescription()).isEqualTo("Quarterly numbers");
    assertThat(found.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(found.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 31));
    assertThat(found.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  void findsOnlyTasksWithRequestedStatus() {
    taskRepository.save(task("Todo task", TaskStatus.TODO));
    taskRepository.save(task("Done task", TaskStatus.DONE));
    taskRepository.save(task("Another done task", TaskStatus.DONE));

    Page<Task> done = taskRepository.findAllByStatus(TaskStatus.DONE, PageRequest.of(0, 10));

    assertThat(done.getContent())
        .extracting(Task::getTitle)
        .containsExactlyInAnyOrder("Done task", "Another done task");
  }

  private static Task task(String title, TaskStatus status) {
    return new Task(title, null, status, null, Instant.parse("2026-10-07T09:00:00Z"));
  }
}
