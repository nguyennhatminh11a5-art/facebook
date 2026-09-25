package com.minifb.comment;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Bình luận: cũ trước, mới sau (mục 5.1). */
public interface CommentRepository extends JpaRepository<Comment, Long> {

  @Query(
      value =
          "SELECT * FROM comments c WHERE c.post_id = :postId"
              + " ORDER BY c.created_at ASC, c.id ASC LIMIT :limit",
      nativeQuery = true)
  List<Comment> firstPage(@Param("postId") long postId, @Param("limit") int limit);

  @Query(
      value =
          "SELECT * FROM comments c WHERE c.post_id = :postId"
              + " AND (c.created_at > :cAt OR (c.created_at = :cAt AND c.id > :cId))"
              + " ORDER BY c.created_at ASC, c.id ASC LIMIT :limit",
      nativeQuery = true)
  List<Comment> nextPage(
      @Param("postId") long postId,
      @Param("cAt") Instant cursorCreatedAt,
      @Param("cId") long cursorId,
      @Param("limit") int limit);

  @Modifying
  @Query(value = "DELETE FROM comments WHERE id = :id", nativeQuery = true)
  int deleteByIdReturningCount(@Param("id") long id);
}
