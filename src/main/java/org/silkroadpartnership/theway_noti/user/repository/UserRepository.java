package org.silkroadpartnership.theway_noti.user.repository;

import java.util.Optional;

import org.silkroadpartnership.theway_noti.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
}
