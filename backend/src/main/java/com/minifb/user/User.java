package com.minifb.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 50)
  private String displayName;

  @Column(length = 300)
  private String bio;

  @Column(name = "avatar_key")
  private String avatarKey;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {}

  public User(String email, String passwordHash, String displayName, Instant now) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void updateProfile(String displayName, String bio, Instant now) {
    this.displayName = displayName;
    this.bio = bio;
    this.updatedAt = now;
  }

  public void changeAvatar(String avatarKey, Instant now) {
    this.avatarKey = avatarKey;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getBio() {
    return bio;
  }

  public String getAvatarKey() {
    return avatarKey;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
