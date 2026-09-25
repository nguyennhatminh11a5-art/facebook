package com.minifb.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  public enum RevokedReason {
    ROTATED,
    LOGOUT,
    REUSE
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "family_id", nullable = false, columnDefinition = "char(36)")
  private String familyId;

  @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "char(64)")
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "revoked_reason")
  private RevokedReason revokedReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected RefreshToken() {}

  public RefreshToken(
      long userId, String familyId, String tokenHash, Instant expiresAt, Instant now) {
    this.userId = userId;
    this.familyId = familyId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = now;
  }

  public void revoke(RevokedReason reason, Instant now) {
    this.revokedReason = reason;
    this.revokedAt = now;
  }

  public boolean isActive(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getFamilyId() {
    return familyId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public RevokedReason getRevokedReason() {
    return revokedReason;
  }
}
