package com.example.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyResetTokenRequest(@NotBlank String token) {}
