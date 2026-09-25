package com.minifb.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<User> findByIdIn(Collection<Long> ids);

  /**
   * Tìm theo tên chứa từ khoá; collation {@code utf8mb4_0900_ai_ci} → không phân biệt hoa thường và
   * dấu. {@code pattern} đã escape {@code %}, {@code _}, {@code \} và bọc {@code %...%}.
   */
  @Query(
      value =
          "SELECT * FROM users u WHERE u.display_name LIKE :pattern ESCAPE '\\\\'"
              + " ORDER BY u.id ASC LIMIT :limit",
      nativeQuery = true)
  List<User> searchFirstPage(@Param("pattern") String pattern, @Param("limit") int limit);

  @Query(
      value =
          "SELECT * FROM users u WHERE u.display_name LIKE :pattern ESCAPE '\\\\'"
              + " AND u.id > :cId ORDER BY u.id ASC LIMIT :limit",
      nativeQuery = true)
  List<User> searchNextPage(
      @Param("pattern") String pattern, @Param("cId") long cursorId, @Param("limit") int limit);
}
