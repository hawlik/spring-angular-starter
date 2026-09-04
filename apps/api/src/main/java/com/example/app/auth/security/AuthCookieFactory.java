package com.example.app.auth.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieFactory {

  public static final String ACCESS_TOKEN_NAME = "ACCESS_TOKEN";
  public static final String REFRESH_TOKEN_NAME = "REFRESH_TOKEN";

  private final boolean secure;

  public AuthCookieFactory(@Value("${security.cookie.secure:true}") boolean secure) {
    this.secure = secure;
  }

  public ResponseCookie accessToken(String token, Duration ttl) {
    return ResponseCookie.from(ACCESS_TOKEN_NAME, token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/")
        .maxAge(ttl)
        .build();
  }

  public ResponseCookie clearAccessToken() {
    return ResponseCookie.from(ACCESS_TOKEN_NAME, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/")
        .maxAge(0)
        .build();
  }

  public ResponseCookie refreshToken(String token, Duration ttl) {
    return ResponseCookie.from(REFRESH_TOKEN_NAME, token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/auth")
        .maxAge(ttl)
        .build();
  }

  public ResponseCookie clearRefreshToken() {
    return ResponseCookie.from(REFRESH_TOKEN_NAME, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/auth")
        .maxAge(0)
        .build();
  }
}
