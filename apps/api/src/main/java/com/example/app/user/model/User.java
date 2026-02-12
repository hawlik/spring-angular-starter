package com.example.app.user.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  private String avatarUrl;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private Set<Role> roles = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus accountStatus = AccountStatus.PENDING_APPROVAL;

  private Instant emailVerifiedAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approved_by")
  private Long approvedBy;

  @Column(name = "verification_token")
  private String verificationToken;

  @Column(name = "token_expires_at")
  private Instant tokenExpiresAt;

  @Column(name = "password_reset_token")
  private String passwordResetToken;

  @Column(name = "password_reset_token_expires_at")
  private Instant passwordResetTokenExpiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  private Instant updatedAt = Instant.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public Set<Role> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  public void setRoles(Set<Role> roles) {
    this.roles = roles;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(AccountStatus accountStatus) {
    this.accountStatus = accountStatus;
  }

  public Instant getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public void setEmailVerifiedAt(Instant emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(Instant approvedAt) {
    this.approvedAt = approvedAt;
  }

  public Long getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(Long approvedBy) {
    this.approvedBy = approvedBy;
  }

  public String getVerificationToken() {
    return verificationToken;
  }

  public void setVerificationToken(String verificationToken) {
    this.verificationToken = verificationToken;
  }

  public Instant getTokenExpiresAt() {
    return tokenExpiresAt;
  }

  public void setTokenExpiresAt(Instant tokenExpiresAt) {
    this.tokenExpiresAt = tokenExpiresAt;
  }

  public String getPasswordResetToken() {
    return passwordResetToken;
  }

  public void setPasswordResetToken(String passwordResetToken) {
    this.passwordResetToken = passwordResetToken;
  }

  public Instant getPasswordResetTokenExpiresAt() {
    return passwordResetTokenExpiresAt;
  }

  public void setPasswordResetTokenExpiresAt(Instant passwordResetTokenExpiresAt) {
    this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
