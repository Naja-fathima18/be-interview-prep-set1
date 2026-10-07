package com.example.beinterviewprep.user.service;

import com.example.beinterviewprep.user.domain.User;
import com.example.beinterviewprep.user.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthService {

  static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final String hashForUnknownUsers;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.hashForUnknownUsers = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  public AccessToken login(String email, String password) {
    Optional<User> user = userRepository.findByEmail(User.normalizeEmail(email));
    String storedHash = user.map(User::getPasswordHash).orElse(hashForUnknownUsers);
    boolean passwordMatches = passwordEncoder.matches(password, storedHash);
    if (user.isEmpty() || !passwordMatches) {
      log.info("Rejected login attempt with invalid credentials");
      throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
    }
    User authenticated = user.get();
    log.info("User {} logged in", authenticated.getId());
    return tokenService.issue(authenticated.getId(), authenticated.getRole());
  }
}
