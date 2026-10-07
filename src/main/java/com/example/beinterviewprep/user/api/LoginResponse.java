package com.example.beinterviewprep.user.api;

import com.example.beinterviewprep.user.service.AccessToken;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

  static final String BEARER = "Bearer";

  public static LoginResponse from(AccessToken token) {
    return new LoginResponse(token.value(), BEARER, token.expiresIn().toSeconds());
  }

  @Override
  public String toString() {
    return "LoginResponse[accessToken=***, tokenType="
        + tokenType
        + ", expiresIn="
        + expiresIn
        + "]";
  }
}
