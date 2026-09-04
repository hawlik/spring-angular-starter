package com.example.app.auth.dto;

import com.example.app.user.model.User;

public record LoginResult(
    String accessToken,
    User user
) {
}
