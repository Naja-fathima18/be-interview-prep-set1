package com.example.beinterviewprep.task.api;

import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final TaskService taskService;

  @PostMapping
  public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
    Task task = taskService.create(request.toCommand());
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(task.getId())
            .toUri();
    return ResponseEntity.created(location).body(TaskResponse.from(task));
  }
}
