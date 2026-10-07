package com.example.beinterviewprep.user.service;

import com.example.beinterviewprep.common.error.ConflictException;
import com.example.beinterviewprep.user.domain.Role;
import com.example.beinterviewprep.user.domain.User;
import com.example.beinterviewprep.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  static final String EMAIL_TAKEN_MESSAGE = "An account with this email already exists";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Transactional
  public User register(String email, String password) {
    return create(email, password, Role.USER);
  }

  private User create(String email, String password, Role role) {
    String normalizedEmail = User.normalizeEmail(email);
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new ConflictException(EMAIL_TAKEN_MESSAGE);
    }
    User user =
        new User(normalizedEmail, passwordEncoder.encode(password), role, Instant.now(clock));
    try {
      User saved = userRepository.saveAndFlush(user);
      log.info("Created user {} with role {}", saved.getId(), role);
      return saved;
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException(EMAIL_TAKEN_MESSAGE);
    }
  }
}
