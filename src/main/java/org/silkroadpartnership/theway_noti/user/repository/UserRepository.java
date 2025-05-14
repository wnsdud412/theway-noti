package org.silkroadpartnership.theway_noti.user.repository;

import java.util.List;
import java.util.Optional;

import org.silkroadpartnership.theway_noti.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  @Query(value = "SELECT * FROM user u WHERE u.id IN (" +
      "SELECT user_id FROM user_nickname WHERE nickname = :nickname" +
      ")", nativeQuery = true)
  List<User> findByNickname(@Param("nickname") String nickname);

  @Query(value = "SELECT * FROM user u WHERE u.id IN (" +
      "SELECT user_id FROM user_role WHERE role = :role" +
      ")", nativeQuery = true)
  List<User> findByRole(@Param("role") String role);
}
