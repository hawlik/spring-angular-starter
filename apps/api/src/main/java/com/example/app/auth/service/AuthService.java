package com.example.app.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.app.auth.dto.LoginRequest;
import com.example.app.auth.dto.LoginResult;
import com.example.app.auth.dto.UpdateProfileRequest;
import com.example.app.auth.dto.UserProfileResponse;
import com.example.app.user.model.User;
import com.example.app.user.repository.UserRepository;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final AuthenticationConfiguration authConfig;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      UserRepository userRepository,
      AuthenticationConfiguration authConfig,
      JwtService jwtService,
      PasswordEncoder passwordEncoder
  ) {
    this.userRepository = userRepository;
    this.authConfig = authConfig;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
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

    User user = userRepository.findByEmail(auth.getName())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new LoginResult(token, user);
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
        .map(this::toProfileResponse)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Transactional
  public void changePassword(String email, String currentPassword, String newPassword) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new BadCredentialsException("Current password is incorrect");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setUpdatedAt(Instant.now());
    userRepository.save(user);
  }

  @Transactional
  public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
    var user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    user.setFirstName(request.firstName().strip());
    user.setLastName(request.lastName().strip());
    user.setUpdatedAt(Instant.now());
    return toProfileResponse(user);
  }

  private UserProfileResponse toProfileResponse(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getAvatarUrl(),
        user.getRoles().stream()
            .map(Enum::name)
            .collect(Collectors.toSet())
    );
  }
}
