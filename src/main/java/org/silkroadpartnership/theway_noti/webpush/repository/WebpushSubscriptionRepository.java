package org.silkroadpartnership.theway_noti.webpush.repository;

import java.util.List;
import java.util.Optional;

import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.webpush.entity.WebpushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebpushSubscriptionRepository  extends JpaRepository<WebpushSubscription, Long> {
  List<WebpushSubscription> findByUser(User user);

  WebpushSubscription findByIdAndUser(Long id, User user);

  Optional<WebpushSubscription> findByEndpointAndUser(String endpoint, User user);
}
