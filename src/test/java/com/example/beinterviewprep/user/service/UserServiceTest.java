package com.example.beinterviewprep.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.user.domain.Role;
import com.example.beinterviewprep.user.domain.User;
import com.example.beinterviewprep.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(userRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void registersUserWithHashedPasswordAndUserRole() {
    when(passwordEncoder.encode("correct-horse")).thenReturn("hashed-password");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User user = userService.register("Alice@Example.com", "correct-horse");

    assertThat(user.getEmail()).isEqualTo("alice@example.com");
    assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
    assertThat(user.getRole()).isEqualTo(Role.USER);
    assertThat(user.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  void rejectsRegistrationWhenEmailIsAlreadyTaken() {
    when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

    assertThatThrownBy(() -> userService.register("ALICE@example.com", "correct-horse"))
        .isInstanceOf(ConflictException.class)
        .hasMessage("An account with this email already exists");
    verify(userRepository, never()).saveAndFlush(any(User.class));
  }

  @Test
  void reportsConcurrentDuplicateRegistrationAsConflict() {
    when(passwordEncoder.encode("correct-horse")).thenReturn("hashed-password");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("uk_app_user_email"));

    assertThatThrownBy(() -> userService.register("alice@example.com", "correct-horse"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void returnsUserById() {
    User user = new User("alice@example.com", "hash", Role.USER, NOW);
    when(userRepository.findById(5L)).thenReturn(Optional.of(user));

    assertThat(userService.get(5L)).isSameAs(user);
  }

  @Test
  void throwsNotFoundWhenUserIsMissing() {
    when(userRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.get(5L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("User with id 5 was not found");
  }

  @Test
  void listsUsersPageByPage() {
    Pageable pageable = PageRequest.of(1, 10);
    when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

    assertThat(userService.list(pageable)).isEmpty();
    verify(userRepository).findAll(pageable);
  }
}
