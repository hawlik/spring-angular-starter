package com.example.app.auth.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.app.auth.dto.LoginRequest;
import com.example.app.auth.dto.LoginResult;
import com.example.app.auth.dto.UserProfileResponse;
import com.example.app.user.repository.UserRepository;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final AuthenticationConfiguration authConfig;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      AuthenticationConfiguration authConfig,
      JwtService jwtService
  ) {
    this.userRepository = userRepository;
    this.authConfig = authConfig;
    this.jwtService = jwtService;
  }

  public LoginResult login(LoginRequest request) {
    Authentication auth = authenticationManager().authenticate(
        new UsernamePasswordAuthenticationToken(
            request.username(),
            request.password()
        )
    );

    List<String> roles = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(Objects::nonNull)
        .filter(a -> a.startsWith("ROLE_"))
        .toList();

    String token = jwtService.createToken(auth.getName(), roles);

    return new LoginResult(token);
  }

  private AuthenticationManager authenticationManager() {
    try {
      return authConfig.getAuthenticationManager();
    } catch (Exception e) {
      throw new IllegalStateException("Could not obtain AuthenticationManager", e);
    }
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getUserProfile(String email) {
    return userRepository.findByEmail(email)
        .map(user -> new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getAvatarUrl(),
            user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet())
        ))
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}