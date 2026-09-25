package com.minifb.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(columnDefinition = "text")
  private String content;

  /** Chỉ đổi bằng câu UPDATE nguyên tử (mục 5.2), không ghi qua entity. */
  @Column(name = "reaction_count", nullable = false, insertable = false, updatable = false)
  private int reactionCount;

  @Column(name = "comment_count", nullable = false, insertable = false, updatable = false)
  private int commentCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Post() {}

  public Post(long userId, String content, Instant now) {
    this.userId = userId;
    this.content = content;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void editContent(String content, Instant now) {
    this.content = content;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getContent() {
    return content;
  }

  public int getReactionCount() {
    return reactionCount;
  }

  public int getCommentCount() {
    return commentCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
