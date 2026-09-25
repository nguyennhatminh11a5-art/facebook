package com.minifb.friendship;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

  @Query("SELECT f FROM Friendship f WHERE f.userLow = :low AND f.userHigh = :high")
  Optional<Friendship> findPair(@Param("low") long low, @Param("high") long high);

  default Optional<Friendship> findBetween(long a, long b) {
    return findPair(Math.min(a, b), Math.max(a, b));
  }

  @Query(
      value =
          "SELECT COUNT(*) > 0 FROM friendships"
              + " WHERE user_low = :low AND user_high = :high AND status = 'ACCEPTED'",
      nativeQuery = true)
  long acceptedCount(@Param("low") long low, @Param("high") long high);

  default boolean areFriends(long a, long b) {
    return acceptedCount(Math.min(a, b), Math.max(a, b)) > 0;
  }

  /** Id bạn bè ACCEPTED. UNION ALL để mỗi nhánh dùng được index riêng. */
  @Query(
      value =
          "SELECT user_high FROM friendships WHERE user_low = :me AND status = 'ACCEPTED'"
              + " UNION ALL"
              + " SELECT user_low FROM friendships WHERE user_high = :me AND status = 'ACCEPTED'",
      nativeQuery = true)
  List<Long> friendIds(@Param("me") long me);
}
