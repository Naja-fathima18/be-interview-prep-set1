package com.example.beinterviewprep.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import com.example.beinterviewprep.task.persistence.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

  @Test
  void listsAllTasksWhenNoStatusGiven() {
    Pageable pageable = PageRequest.of(0, 20);
    when(taskRepository.findAll(pageable)).thenReturn(Page.empty());

    taskService.list(null, pageable);

    verify(taskRepository).findAll(pageable);
  }

  @Test
  void listsTasksWithStatusWhenStatusGiven() {
    Pageable pageable = PageRequest.of(0, 20);
    when(taskRepository.findAllByStatus(TaskStatus.DONE, pageable)).thenReturn(Page.empty());

    taskService.list(TaskStatus.DONE, pageable);

    verify(taskRepository).findAllByStatus(TaskStatus.DONE, pageable);
  }

  @Test
  void throwsNotFoundWhenTaskIsMissing() {
    when(taskRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.get(42L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Task with id 42 was not found");
  }
}
