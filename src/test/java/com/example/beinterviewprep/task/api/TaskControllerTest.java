package com.example.beinterviewprep.task.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import com.example.beinterviewprep.task.service.TaskCommand;
import com.example.beinterviewprep.task.service.TaskService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TaskService taskService;

  @Test
  void createsTaskAndReturnsLocation() throws Exception {
    LocalDate dueDate = LocalDate.now().plusDays(7);
    when(taskService.create(any(TaskCommand.class)))
        .thenReturn(task(1L, "Write report", TaskStatus.TODO, dueDate));

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Write report", "description": "Quarterly", "dueDate": "%s"}
                    """
                        .formatted(dueDate)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/tasks/1"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Write report"))
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.dueDate").value(dueDate.toString()))
        .andExpect(jsonPath("$.createdAt").value("2026-10-07T09:00:00Z"));
  }

  static Task task(Long id, String title, TaskStatus status, LocalDate dueDate) {
    Task task = new Task(title, "Quarterly", status, dueDate, CREATED_AT);
    ReflectionTestUtils.setField(task, "id", id);
    return task;
  }
}
