package com.example.beinterviewprep.user.api;

import static com.example.beinterviewprep.user.api.AuthControllerTest.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.config.ClockConfig;
import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.common.security.JwtProperties;
import com.example.beinterviewprep.common.security.SecurityConfig;
import com.example.beinterviewprep.user.domain.Role;
import com.example.beinterviewprep.user.service.TokenService;
import com.example.beinterviewprep.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, ClockConfig.class})
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtEncoder jwtEncoder;
  @Autowired private JwtProperties jwtProperties;

  @MockitoBean private UserService userService;

  @Test
  void returnsProfileOfAuthenticatedUser() throws Exception {
    when(userService.get(7L)).thenReturn(user(7L, "alice@example.com", Role.USER));

    mockMvc
        .perform(get("/api/users/me").with(jwt().jwt(token -> token.subject("7"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.email").value("alice@example.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void returnsProfileForRealSignedToken() throws Exception {
    when(userService.get(7L)).thenReturn(user(7L, "alice@example.com", Role.USER));
    String token = tokenIssuedAt(Instant.now(), 7L);

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7));
  }

  @Test
  void rejectsUnauthenticatedProfileRequest() throws Exception {
    mockMvc
        .perform(get("/api/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.instance").value("/api/users/me"));
    verifyNoInteractions(userService);
  }

  @Test
  void rejectsTokenOlderThanFifteenMinutes() throws Exception {
    String expired = tokenIssuedAt(Instant.now().minus(jwtProperties.ttl()).minusSeconds(1), 7L);

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("The access token is invalid or has expired"));
    verifyNoInteractions(userService);
  }

  @Test
  void rejectsTamperedToken() throws Exception {
    String token = tokenIssuedAt(Instant.now(), 7L);
    String tampered = token.substring(0, token.length() - 4) + "AAAA";

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(userService);
  }

  @Test
  void returnsNotFoundWhenAccountNoLongerExists() throws Exception {
    when(userService.get(7L)).thenThrow(new ResourceNotFoundException("User", 7L));

    mockMvc
        .perform(get("/api/users/me").with(jwt().jwt(token -> token.subject("7"))))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void forbidsRegularUserFromListingAllUsers() throws Exception {
    mockMvc
        .perform(get("/api/users").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.title").value("Forbidden"))
        .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"))
        .andExpect(jsonPath("$.instance").value("/api/users"));
    verifyNoInteractions(userService);
  }

  @Test
  void forbidsRegularUserWithSignedTokenFromListingAllUsers() throws Exception {
    String token = tokenIssuedAt(Instant.now(), 7L, Role.USER);

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(userService);
  }

  @Test
  void letsAdminListAllUsersOldestFirst() throws Exception {
    when(userService.list(any(Pageable.class)))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    user(1L, "admin@example.com", Role.ADMIN),
                    user(2L, "alice@example.com", Role.USER)),
                PageRequest.of(0, 20),
                2));
    String token = tokenIssuedAt(Instant.now(), 1L, Role.ADMIN);

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
        .andExpect(jsonPath("$.content[1].email").value("alice@example.com"))
        .andExpect(jsonPath("$.content[1].passwordHash").doesNotExist())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.size").value(20));

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(userService).list(pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
        .isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void rejectsUnauthenticatedUserListing() throws Exception {
    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(userService);
  }

  private String tokenIssuedAt(Instant issuedAt, Long userId) {
    return tokenIssuedAt(issuedAt, userId, Role.USER);
  }

  private String tokenIssuedAt(Instant issuedAt, Long userId, Role role) {
    Clock clock = Clock.fixed(issuedAt, ZoneOffset.UTC);
    return new TokenService(jwtEncoder, jwtProperties, clock).issue(userId, role).value();
  }
}
