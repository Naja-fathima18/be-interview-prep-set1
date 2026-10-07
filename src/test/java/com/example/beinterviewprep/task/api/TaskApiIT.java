package com.example.beinterviewprep.task.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.task.persistence.TaskRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class TaskApiIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private TaskRepository taskRepository;

  @AfterEach
  void cleanUp() {
    taskRepository.deleteAll();
  }

  @Test
  void managesTaskThroughItsWholeLifecycle() throws Exception {
    LocalDate dueDate = LocalDate.now().plusDays(5);
    String location =
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
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    assertThat(location).isNotNull();

    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Write report"))
        .andExpect(jsonPath("$.dueDate").value(dueDate.toString()));

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Write final report\", \"status\": \"DONE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Write final report"))
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.description").doesNotExist())
        .andExpect(jsonPath("$.dueDate").doesNotExist());

    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Write final report"))
        .andExpect(jsonPath("$.status").value("DONE"));

    mockMvc.perform(delete(location)).andExpect(status().isNoContent());

    mockMvc
        .perform(get(location))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void listsTasksFilteredByStatusWithPageMetadata() throws Exception {
    createTask("First todo", "TODO");
    createTask("Done task", "DONE");
    createTask("Second todo", "TODO");

    mockMvc
        .perform(get("/api/tasks").param("status", "TODO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[*].title", containsInAnyOrder("First todo", "Second todo")));

    mockMvc
        .perform(get("/api/tasks").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void rejectsSortingByUnknownProperty() throws Exception {
    mockMvc
        .perform(get("/api/tasks").param("sort", "colour"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors[0].field").value("sort"));
  }

  @Test
  void returnsNotFoundWhenDeletingUnknownTask() throws Exception {
    mockMvc
        .perform(delete("/api/tasks/987654"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Task with id 987654 was not found"));
  }

  private void createTask(String title, String status) throws Exception {
    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"%s\", \"status\": \"%s\"}".formatted(title, status)))
        .andExpect(status().isCreated());
  }
}
