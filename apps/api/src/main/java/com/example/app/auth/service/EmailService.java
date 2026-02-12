package com.example.app.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final String from;
  private final String frontendBaseUrl;

  public EmailService(
      JavaMailSender mailSender,
      @Value("${spring.mail.from:noreply@example.com}") String from,
      @Value("${app.frontend-base-url:http://localhost:4200}") String frontendBaseUrl
  ) {
    this.mailSender = mailSender;
    this.from = from;
    this.frontendBaseUrl = frontendBaseUrl;
  }

  public void sendPasswordResetEmail(String to, String token) {
    String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(to);
    message.setSubject("Password Reset Request");
    message.setText(
        "You requested a password reset.\n\n"
            + "Click the link below to set a new password:\n"
            + resetLink + "\n\n"
            + "This link will expire in 1 hour.\n\n"
            + "If you did not request this, please ignore this email."
    );

    mailSender.send(message);
  }
}
