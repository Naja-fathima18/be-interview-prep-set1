package com.example.beinterviewprep.user.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.config.ClockConfig;
import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.common.security.SecurityConfig;
import com.example.beinterviewprep.user.domain.Role;
import com.example.beinterviewprep.user.domain.User;
import com.example.beinterviewprep.user.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ClockConfig.class})
class AuthControllerTest {

  static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @Test
  void registersUserWithoutAuthenticationAndHidesPassword() throws Exception {
    when(userService.register("alice@example.com", "correct-horse"))
        .thenReturn(user(1L, "alice@example.com", Role.USER));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"alice@example.com\", \"password\": \"correct-horse\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("alice@example.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.createdAt").value("2026-10-07T09:00:00Z"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void rejectsRegistrationWithInvalidEmailAndShortPassword() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"not-an-email\", \"password\": \"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[0].field").value("email"))
        .andExpect(jsonPath("$.errors[0].message").value("Email must be a valid email address"))
        .andExpect(jsonPath("$.errors[1].field").value("password"))
        .andExpect(jsonPath("$.errors[1].message").value("Password must be at least 8 characters"));
    verifyNoInteractions(userService);
  }

  @Test
  void rejectsRegistrationWithoutEmailOrPassword() throws Exception {
    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].message").value("Email is required"))
        .andExpect(jsonPath("$.errors[1].message").value("Password is required"));
    verifyNoInteractions(userService);
  }

  @Test
  void rejectsPasswordLongerThanSeventyTwoBytes() throws Exception {
    String seventyTwoCharactersButMoreBytes = "é".repeat(72);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\": \"alice@example.com\", \"password\": \"%s\"}"
                        .formatted(seventyTwoCharactersButMoreBytes)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("password"))
        .andExpect(jsonPath("$.errors[0].message").value("Password must be at most 72 bytes"));
    verifyNoInteractions(userService);
  }

  @Test
  void returnsConflictWhenEmailIsAlreadyRegistered() throws Exception {
    when(userService.register("alice@example.com", "correct-horse"))
        .thenThrow(new ConflictException("An account with this email already exists"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"alice@example.com\", \"password\": \"correct-horse\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
  }

  static User user(Long id, String email, Role role) {
    User user = new User(email, "hashed-password", role, CREATED_AT);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
