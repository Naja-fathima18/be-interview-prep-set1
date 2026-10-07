package com.example.beinterviewprep.task.service;

import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import com.example.beinterviewprep.task.persistence.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

  private final TaskRepository taskRepository;
  private final Clock clock;

  @Transactional
  public Task create(TaskCommand command) {
    Task task =
        new Task(
            command.title(),
            command.description(),
            Objects.requireNonNullElse(command.status(), TaskStatus.TODO),
            command.dueDate(),
            Instant.now(clock));
    Task saved = taskRepository.save(task);
    log.info("Created task {}", saved.getId());
    return saved;
  }
}
