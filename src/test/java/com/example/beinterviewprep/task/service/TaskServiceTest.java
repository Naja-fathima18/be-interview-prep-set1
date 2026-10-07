package com.example.beinterviewprep.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import com.example.beinterviewprep.task.persistence.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");

  @Mock private TaskRepository taskRepository;

  private TaskService taskService;

  @BeforeEach
  void setUp() {
    taskService = new TaskService(taskRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsTaskWithCreatedDateFromClock() {
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Task task =
        taskService.create(
            new TaskCommand(
                "Write report", "Quarterly", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 10, 31)));

    assertThat(task.getTitle()).isEqualTo("Write report");
    assertThat(task.getDescription()).isEqualTo("Quarterly");
    assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 31));
    assertThat(task.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void defaultsStatusToTodoWhenNotGiven() {
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Task task = taskService.create(new TaskCommand("Write report", null, null, null));

    assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
  }
}
