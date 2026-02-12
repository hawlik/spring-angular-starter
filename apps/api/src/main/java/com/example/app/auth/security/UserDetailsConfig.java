package com.example.app.auth.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.example.app.user.model.AccountStatus;
import com.example.app.user.repository.UserRepository;

@Configuration
public class UserDetailsConfig {

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepository) {
    return username -> userRepository.findByEmail(username)
        .map(u -> User.withUsername(u.getEmail())
            .password(u.getPasswordHash())
            .roles(u.getRoles()
                .stream()
                .map(Enum::name)
                .toArray(String[]::new))
            .accountLocked(u.getAccountStatus() == AccountStatus.DISABLED)
            .disabled(u.getAccountStatus() != AccountStatus.ACTIVE)
            .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
