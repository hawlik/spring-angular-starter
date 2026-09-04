package com.example.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password, Boolean rememberMe) {

  public boolean isRememberMe() {
    return Boolean.TRUE.equals(rememberMe);
  }
}
