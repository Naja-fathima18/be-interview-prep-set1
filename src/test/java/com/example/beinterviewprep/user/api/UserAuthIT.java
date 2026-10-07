package com.example.beinterviewprep.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.TestcontainersConfiguration;
import com.example.beinterviewprep.shorturl.persistence.ShortUrlRepository;
import com.example.beinterviewprep.task.persistence.TaskRepository;
import com.example.beinterviewprep.user.domain.User;
import com.example.beinterviewprep.user.persistence.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(
    properties = {
      "app.security.admin.email=" + UserAuthIT.ADMIN_EMAIL,
      "app.security.admin.password=" + UserAuthIT.ADMIN_PASSWORD
    })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserAuthIT {

  static final String ADMIN_EMAIL = "admin@example.com";
  static final String ADMIN_PASSWORD = "admin-it-password";

  private static final String USER_EMAIL = "alice@example.com";
  private static final String USER_PASSWORD = "alice-secret-1";

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private TaskRepository taskRepository;

  @Autowired private ShortUrlRepository shortUrlRepository;

  @AfterEach
  void cleanUp() {
    taskRepository.deleteAll();
    shortUrlRepository.deleteAll();
    userRepository.deleteAll(
        userRepository.findAll().stream()
            .filter(user -> !ADMIN_EMAIL.equals(user.getEmail()))
            .toList());
  }

  @Test
  void registeredUserLogsInAndReadsOwnProfile() throws Exception {
    register(USER_EMAIL, USER_PASSWORD)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(USER_EMAIL))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    login(USER_EMAIL, USER_PASSWORD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900));
    String token = tokenFor(USER_EMAIL, USER_PASSWORD);

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(USER_EMAIL))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.id").isNumber());
  }

  @Test
  void storesPasswordAsBcryptHash() throws Exception {
    register(USER_EMAIL, USER_PASSWORD).andExpect(status().isCreated());

    User stored = userRepository.findByEmail(USER_EMAIL).orElseThrow();

    assertThat(stored.getPasswordHash()).startsWith("$2").isNotEqualTo(USER_PASSWORD);
  }

  @Test
  void forbidsUserTokenFromListingAllUsers() throws Exception {
    register(USER_EMAIL, USER_PASSWORD).andExpect(status().isCreated());
    String token = tokenFor(USER_EMAIL, USER_PASSWORD);

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.instance").value("/api/users"));
  }

  @Test
  void bootstrappedAdminListsAllUsers() throws Exception {
    register(USER_EMAIL, USER_PASSWORD).andExpect(status().isCreated());
    String adminToken = tokenFor(ADMIN_EMAIL, ADMIN_PASSWORD);

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[*].email", containsInAnyOrder(ADMIN_EMAIL, USER_EMAIL)))
        .andExpect(jsonPath("$.content[*].role", containsInAnyOrder("ADMIN", "USER")));
  }

  @Test
  void rejectsRequestWithoutTokenWithBearerChallenge() throws Exception {
    mockMvc
        .perform(get("/api/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(
            jsonPath("$.detail").value("Authentication is required to access this resource"));
  }

  @Test
  void rejectsMalformedToken() throws Exception {
    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer("not.a.jwt")))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("The access token is invalid or has expired"));
  }

  @Test
  void rejectsDuplicateRegistration() throws Exception {
    register(USER_EMAIL, USER_PASSWORD).andExpect(status().isCreated());

    register("ALICE@example.com", "another-password")
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
  }

  @Test
  void rejectsLoginWithWrongPassword() throws Exception {
    register(USER_EMAIL, USER_PASSWORD).andExpect(status().isCreated());

    login(USER_EMAIL, "wrong-password")
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.accessToken").doesNotExist());
  }

  @Test
  void realTokenGrantsAccessToTaskApi() throws Exception {
    register(USER_EMAIL, USER_PASSWORD).andExpect(status().isCreated());
    String token = tokenFor(USER_EMAIL, USER_PASSWORD);

    mockMvc
        .perform(
            post("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Secured task\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Secured task"));

    mockMvc
        .perform(get("/api/tasks").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
  }

  @Test
  void realTokenCreatesShortUrlThatRedirectsWithoutToken() throws Exception {
    register(USER_EMAIL, USER_PASSWORD).andExpect(status().isCreated());
    String token = tokenFor(USER_EMAIL, USER_PASSWORD);
    String url = "https://example.com/some/long/path";

    String code =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/short-urls")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"%s\"}".formatted(url)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.code");

    mockMvc
        .perform(get("/{code}", code))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, url));
    mockMvc
        .perform(post("/api/short-urls").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  private ResultActions register(String email, String password) throws Exception {
    return mockMvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentials(email, password)));
  }

  private ResultActions login(String email, String password) throws Exception {
    return mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentials(email, password)));
  }

  private String tokenFor(String email, String password) throws Exception {
    String body =
        login(email, password)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  private static String credentials(String email, String password) {
    return "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
