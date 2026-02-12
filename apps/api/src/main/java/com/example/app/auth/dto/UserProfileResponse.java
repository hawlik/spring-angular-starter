package com.example.app.auth.dto;

import java.util.Set;

public record UserProfileResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    Set<String> roles
) {
}