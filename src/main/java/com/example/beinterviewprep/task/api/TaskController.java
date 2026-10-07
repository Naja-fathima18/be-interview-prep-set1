package com.example.beinterviewprep.task.api;

import com.example.beinterviewprep.common.api.PageResponse;
import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import com.example.beinterviewprep.task.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping
  public PageResponse<TaskResponse> list(
      @RequestParam(required = false) TaskStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return PageResponse.from(taskService.list(status, pageable), TaskResponse::from);
  }

  @GetMapping("/{id}")
  public TaskResponse get(@PathVariable Long id) {
    return TaskResponse.from(taskService.get(id));
  }

  @PutMapping("/{id}")
  public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
    return TaskResponse.from(taskService.update(id, request.toCommand()));
  }
}
