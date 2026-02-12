package com.example.app.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.app.user.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByPasswordResetToken(String passwordResetToken);

}