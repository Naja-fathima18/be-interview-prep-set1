package com.example.beinterviewprep.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.user.domain.Role;
import com.example.beinterviewprep.user.domain.User;
import com.example.beinterviewprep.user.persistence.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String HASH_FOR_UNKNOWN_USERS = "hash-for-unknown-users";

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private TokenService tokenService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    when(passwordEncoder.encode(anyString())).thenReturn(HASH_FOR_UNKNOWN_USERS);
    authService = new AuthService(userRepository, passwordEncoder, tokenService);
  }

  @Test
  void issuesTokenWhenCredentialsAreValid() {
    User user = user(5L, Role.ADMIN);
    AccessToken token = new AccessToken("signed-token", Duration.ofMinutes(15));
    when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("correct-horse", "stored-hash")).thenReturn(true);
    when(tokenService.issue(5L, Role.ADMIN)).thenReturn(token);

    AccessToken issued = authService.login(" Alice@Example.com ", "correct-horse");

    assertThat(issued).isEqualTo(token);
  }

  @Test
  void rejectsWrongPassword() {
    when(userRepository.findByEmail("alice@example.com"))
        .thenReturn(Optional.of(user(5L, Role.USER)));
    when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.login("alice@example.com", "wrong-password"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid email or password");
    verifyNoInteractions(tokenService);
  }

  @Test
  void rejectsUnknownEmailWithSameMessageAfterCheckingPasswordAnyway() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login("nobody@example.com", "correct-horse"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid email or password");
    verify(passwordEncoder).matches("correct-horse", HASH_FOR_UNKNOWN_USERS);
    verifyNoInteractions(tokenService);
  }

  @Test
  void rejectsUnknownEmailEvenIfPasswordMatchesPlaceholderHash() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    assertThatThrownBy(() -> authService.login("nobody@example.com", "anything"))
        .isInstanceOf(BadCredentialsException.class);
    verifyNoInteractions(tokenService);
  }

  private static User user(Long id, Role role) {
    User user = new User("alice@example.com", "stored-hash", role, Instant.now());
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
