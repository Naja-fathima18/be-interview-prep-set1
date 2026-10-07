package com.example.beinterviewprep.user.service;

import java.time.Duration;

public record AccessToken(String value, Duration expiresIn) {

  @Override
  public String toString() {
    return "AccessToken[value=***, expiresIn=" + expiresIn + "]";
  }
}
