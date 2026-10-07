package com.example.beinterviewprep.task.api;

import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    LocalDate dueDate,
    Instant createdAt) {

  public static TaskResponse from(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getDueDate(),
        task.getCreatedAt());
  }
}
