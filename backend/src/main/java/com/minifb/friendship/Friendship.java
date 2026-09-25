package com.minifb.friendship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Một dòng cho mỗi cặp người dùng, luôn lưu {@code userLow < userHigh}. */
@Entity
@Table(name = "friendships")
public class Friendship {

  public enum Status {
    PENDING,
    ACCEPTED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_low", nullable = false)
  private Long userLow;

  @Column(name = "user_high", nullable = false)
  private Long userHigh;

  @Column(name = "requester_id", nullable = false)
  private Long requesterId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Friendship() {}

  /** Lời mời từ {@code requesterId} tới {@code otherId}. */
  public static Friendship request(long requesterId, long otherId, Instant now) {
    Friendship f = new Friendship();
    f.userLow = Math.min(requesterId, otherId);
    f.userHigh = Math.max(requesterId, otherId);
    f.requesterId = requesterId;
    f.status = Status.PENDING;
    f.createdAt = now;
    f.updatedAt = now;
    return f;
  }

  public void accept(Instant now) {
    this.status = Status.ACCEPTED;
    this.updatedAt = now;
  }

  public long otherThan(long userId) {
    return userLow == userId ? userHigh : userLow;
  }

  public Long getId() {
    return id;
  }

  public Long getRequesterId() {
    return requesterId;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
