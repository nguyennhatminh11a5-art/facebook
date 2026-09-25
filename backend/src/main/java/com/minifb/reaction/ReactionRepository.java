package com.minifb.reaction;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactionRepository extends JpaRepository<Reaction, Reaction.Id> {

  interface MyReaction {
    Long getPostId();

    String getType();
  }

  /** Reaction của tôi cho một lô bài (feed không N+1). */
  @Query(
      value =
          "SELECT post_id AS postId, type FROM reactions WHERE user_id = :userId"
              + " AND post_id IN (:postIds)",
      nativeQuery = true)
  List<MyReaction> findMine(
      @Param("userId") long userId, @Param("postIds") Collection<Long> postIds);

  @Modifying
  @Query(
      value =
          "INSERT IGNORE INTO reactions (post_id, user_id, type, created_at)"
              + " VALUES (:postId, :userId, :type, :now)",
      nativeQuery = true)
  int insertIgnore(
      @Param("postId") long postId,
      @Param("userId") long userId,
      @Param("type") String type,
      @Param("now") Instant now);

  @Modifying
  @Query(
      value = "UPDATE reactions SET type = :type WHERE post_id = :postId AND user_id = :userId",
      nativeQuery = true)
  int updateType(
      @Param("postId") long postId, @Param("userId") long userId, @Param("type") String type);

  @Modifying
  @Query(
      value = "DELETE FROM reactions WHERE post_id = :postId AND user_id = :userId",
      nativeQuery = true)
  int deleteMine(@Param("postId") long postId, @Param("userId") long userId);
}
