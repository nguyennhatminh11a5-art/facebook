package com.minifb.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "post_id", nullable = false)
  private Long postId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false, length = 1000)
  private String content;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Comment() {}

  public Comment(long postId, long userId, String content, Instant now) {
    this.postId = postId;
    this.userId = userId;
    this.content = content;
    this.createdAt = now;
  }

  public Long getId() {
    return id;
  }

  public Long getPostId() {
    return postId;
  }

  public Long getUserId() {
    return userId;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
