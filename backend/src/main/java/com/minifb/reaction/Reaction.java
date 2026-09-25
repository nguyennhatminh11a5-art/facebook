package com.minifb.reaction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;

/** PK {@code (post_id, user_id)}: DB tự chặn reaction 2 lần. Ghi bằng native query (mục 5.2). */
@Entity
@Table(name = "reactions")
public class Reaction {

  @Embeddable
  public record Id(@Column(name = "post_id") Long postId, @Column(name = "user_id") Long userId)
      implements Serializable {}

  @EmbeddedId private Id id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReactionType type;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Reaction() {}

  public Id getId() {
    return id;
  }

  public ReactionType getType() {
    return type;
  }
}
