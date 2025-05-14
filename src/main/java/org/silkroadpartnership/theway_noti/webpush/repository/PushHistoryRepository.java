package org.silkroadpartnership.theway_noti.webpush.repository;

import java.util.UUID;

import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.silkroadpartnership.theway_noti.webpush.entity.PushHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;


public interface PushHistoryRepository extends JpaRepository<PushHistory, UUID>{
 
  List<PushHistory> findByCreateDateBetweenAndRole(LocalDateTime start, LocalDateTime end, Role role);
}
