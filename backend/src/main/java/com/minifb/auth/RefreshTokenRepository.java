package com.minifb.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /** SELECT ... FOR UPDATE: 2 tab refresh cùng lúc sẽ xếp hàng, tab sau thấy token đã ROTATED. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :hash")
  Optional<RefreshToken> findByHashForUpdate(@Param("hash") String hash);

  Optional<RefreshToken> findByTokenHash(String hash);

  @Query(
      "SELECT COUNT(t) > 0 FROM RefreshToken t WHERE t.familyId = :familyId"
          + " AND t.revokedAt IS NULL AND t.expiresAt > :now")
  boolean familyHasActiveToken(@Param("familyId") String familyId, @Param("now") Instant now);

  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.revokedAt = :now,"
          + " t.revokedReason = com.minifb.auth.RefreshToken.RevokedReason.REUSE"
          + " WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
  int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

  @Modifying
  @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :before")
  int deleteExpiredBefore(@Param("before") Instant before);
}
