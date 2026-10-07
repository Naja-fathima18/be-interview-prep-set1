package com.example.beinterviewprep.task.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.task.domain.Task;
import com.example.beinterviewprep.task.domain.TaskStatus;
import com.example.beinterviewprep.task.service.TaskCommand;
import com.example.beinterviewprep.task.service.TaskService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
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

  @Test
  void rejectsInvalidTaskWithFieldLevelMessages() throws Exception {
    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "%s", "dueDate": "%s"}
                    """
                        .formatted("x".repeat(101), LocalDate.now().minusDays(1))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[0].field").value("dueDate"))
        .andExpect(jsonPath("$.errors[0].message").value("Due date cannot be in the past"))
        .andExpect(jsonPath("$.errors[1].field").value("title"))
        .andExpect(jsonPath("$.errors[1].message").value("Title must be at most 100 characters"));
    verifyNoInteractions(taskService);
  }

  @Test
  void rejectsMissingTitle() throws Exception {
    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("title"))
        .andExpect(jsonPath("$.errors[0].message").value("Title is required"));
  }

  @Test
  void rejectsUnknownStatusValue() throws Exception {
    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Write report\", \"status\": \"BLOCKED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Invalid value 'BLOCKED'; must be one of [TODO, IN_PROGRESS, DONE]"));
  }

  @Test
  void rejectsMalformedDueDate() throws Exception {
    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Write report\", \"dueDate\": \"31-10-2026\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("dueDate"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Invalid value '31-10-2026'; expected a date in yyyy-MM-dd format"));
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Malformed JSON request"));
  }

  @Test
  void returnsTaskById() throws Exception {
    when(taskService.get(1L)).thenReturn(task(1L, "Write report", TaskStatus.DONE, null));

    mockMvc
        .perform(get("/api/tasks/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Write report"))
        .andExpect(jsonPath("$.status").value("DONE"));
  }

  @Test
  void returnsNotFoundForUnknownTask() throws Exception {
    when(taskService.get(99L)).thenThrow(new ResourceNotFoundException("Task", 99L));

    mockMvc
        .perform(get("/api/tasks/99"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value("Task with id 99 was not found"))
        .andExpect(jsonPath("$.instance").value("/api/tasks/99"));
  }

  @Test
  void rejectsNonNumericTaskId() throws Exception {
    mockMvc
        .perform(get("/api/tasks/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("id"))
        .andExpect(jsonPath("$.errors[0].message").value("Invalid value 'abc'; expected a number"));
  }

  @Test
  void listsTasksNewestFirstWithPageMetadata() throws Exception {
    when(taskService.list(isNull(), any(Pageable.class)))
        .thenReturn(
            new PageImpl<>(
                List.of(task(2L, "Second", TaskStatus.TODO, null)), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/api/tasks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(2))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(taskService).list(isNull(), pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void filtersTasksByStatus() throws Exception {
    when(taskService.list(eq(TaskStatus.DONE), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task(3L, "Done", TaskStatus.DONE, null))));

    mockMvc
        .perform(get("/api/tasks").param("status", "DONE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].status").value("DONE"));
  }

  @Test
  void rejectsUnknownStatusFilter() throws Exception {
    mockMvc
        .perform(get("/api/tasks").param("status", "BLOCKED"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Invalid value 'BLOCKED'; must be one of [TODO, IN_PROGRESS, DONE]"));
    verifyNoInteractions(taskService);
  }

  @Test
  void rejectsUnknownSortProperty() throws Exception {
    when(taskService.list(isNull(), any(Pageable.class)))
        .thenThrow(
            new PropertyReferenceException("colour", TypeInformation.of(Task.class), List.of()));

    mockMvc
        .perform(get("/api/tasks").param("sort", "colour"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("sort"))
        .andExpect(jsonPath("$.errors[0].message").value("Unknown property 'colour'"));
  }

  @Test
  void updatesTask() throws Exception {
    LocalDate dueDate = LocalDate.now().plusDays(3);
    when(taskService.update(eq(1L), any(TaskCommand.class)))
        .thenReturn(task(1L, "Updated", TaskStatus.IN_PROGRESS, dueDate));

    mockMvc
        .perform(
            put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Updated", "status": "IN_PROGRESS", "dueDate": "%s"}
                    """
                        .formatted(dueDate)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void rejectsUpdateWithoutStatusOrTitle() throws Exception {
    mockMvc
        .perform(
            put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[0].field").value("status"))
        .andExpect(jsonPath("$.errors[0].message").value("Status is required"))
        .andExpect(jsonPath("$.errors[1].field").value("title"))
        .andExpect(jsonPath("$.errors[1].message").value("Title is required"));
    verifyNoInteractions(taskService);
  }

  @Test
  void returnsNotFoundWhenUpdatingUnknownTask() throws Exception {
    when(taskService.update(eq(99L), any(TaskCommand.class)))
        .thenThrow(new ResourceNotFoundException("Task", 99L));

    mockMvc
        .perform(
            put("/api/tasks/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Updated\", \"status\": \"DONE\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Task with id 99 was not found"));
  }

  @Test
  void deletesTask() throws Exception {
    mockMvc.perform(delete("/api/tasks/1")).andExpect(status().isNoContent());

    verify(taskService).delete(1L);
  }

  @Test
  void returnsNotFoundWhenDeletingUnknownTask() throws Exception {
    doThrow(new ResourceNotFoundException("Task", 99L)).when(taskService).delete(99L);

    mockMvc
        .perform(delete("/api/tasks/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Task with id 99 was not found"));
  }

  static Task task(Long id, String title, TaskStatus status, LocalDate dueDate) {
    Task task = new Task(title, "Quarterly", status, dueDate, CREATED_AT);
    ReflectionTestUtils.setField(task, "id", id);
    return task;
  }
}
