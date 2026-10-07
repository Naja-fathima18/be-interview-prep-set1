package com.example.beinterviewprep.task.api;

import com.example.beinterviewprep.task.domain.TaskStatus;
import com.example.beinterviewprep.task.service.TaskCommand;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
    @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,
    @Size(max = 1000, message = "Description must be at most 1000 characters") String description,
    TaskStatus status,
    @FutureOrPresent(message = "Due date cannot be in the past") LocalDate dueDate) {

  public TaskCommand toCommand() {
    return new TaskCommand(title, description, status, dueDate);
  }
}
