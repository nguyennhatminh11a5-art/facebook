package com.minifb.post;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Phân trang cursor (mục 5.1): 2 truy vấn riêng cho trang đầu/trang sau, điều kiện tường minh,
 * {@code LIMIT size + 1}.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

  @Query(
      value =
          "SELECT p.id FROM posts p WHERE p.user_id IN (:authorIds)"
              + " ORDER BY p.created_at DESC, p.id DESC LIMIT :limit",
      nativeQuery = true)
  List<Long> feedFirstPage(
      @Param("authorIds") Collection<Long> authorIds, @Param("limit") int limit);

  @Query(
      value =
          "SELECT p.id FROM posts p WHERE p.user_id IN (:authorIds)"
              + " AND (p.created_at < :cAt OR (p.created_at = :cAt AND p.id < :cId))"
              + " ORDER BY p.created_at DESC, p.id DESC LIMIT :limit",
      nativeQuery = true)
  List<Long> feedNextPage(
      @Param("authorIds") Collection<Long> authorIds,
      @Param("cAt") Instant cursorCreatedAt,
      @Param("cId") long cursorId,
      @Param("limit") int limit);

  @Query(
      value =
          "SELECT p.id FROM posts p WHERE p.user_id = :userId"
              + " ORDER BY p.created_at DESC, p.id DESC LIMIT :limit",
      nativeQuery = true)
  List<Long> userFirstPage(@Param("userId") long userId, @Param("limit") int limit);

  @Query(
      value =
          "SELECT p.id FROM posts p WHERE p.user_id = :userId"
              + " AND (p.created_at < :cAt OR (p.created_at = :cAt AND p.id < :cId))"
              + " ORDER BY p.created_at DESC, p.id DESC LIMIT :limit",
      nativeQuery = true)
  List<Long> userNextPage(
      @Param("userId") long userId,
      @Param("cAt") Instant cursorCreatedAt,
      @Param("cId") long cursorId,
      @Param("limit") int limit);

  List<Post> findByIdIn(Collection<Long> ids);

  /**
   * Khoá dòng bài trước khi ghi bảng con + cập nhật bộ đếm (mục 5.2), tránh deadlock khoá S của
   * khoá ngoại → khoá X của UPDATE.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Post p WHERE p.id = :id")
  Optional<Post> findByIdForUpdate(@Param("id") long id);

  @Query(value = "SELECT reaction_count FROM posts WHERE id = :id", nativeQuery = true)
  int reactionCount(@Param("id") long id);

  @Query(value = "SELECT comment_count FROM posts WHERE id = :id", nativeQuery = true)
  int commentCount(@Param("id") long id);

  @Modifying
  @Query(
      value = "UPDATE posts SET reaction_count = reaction_count + :delta WHERE id = :id",
      nativeQuery = true)
  int addReactionCount(@Param("id") long id, @Param("delta") int delta);

  @Modifying
  @Query(
      value = "UPDATE posts SET comment_count = comment_count + :delta WHERE id = :id",
      nativeQuery = true)
  int addCommentCount(@Param("id") long id, @Param("delta") int delta);
}
