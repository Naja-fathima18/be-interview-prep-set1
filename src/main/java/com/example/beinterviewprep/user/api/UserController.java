package com.example.beinterviewprep.user.api;

import com.example.beinterviewprep.common.api.PageResponse;
import com.example.beinterviewprep.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  public PageResponse<UserResponse> list(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return PageResponse.from(userService.list(pageable), UserResponse::from);
  }

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return UserResponse.from(userService.get(Long.valueOf(jwt.getSubject())));
  }
}
