package org.silkroadpartnership.theway_noti.webpush.repository;

import java.util.UUID;

import org.silkroadpartnership.theway_noti.webpush.entity.PushHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushHistoryRepository extends JpaRepository<PushHistory, UUID>{
  
}
