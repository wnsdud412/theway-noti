package org.silkroadpartnership.theway_noti.webpush.service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.webpush.entity.PushHistory;
import org.silkroadpartnership.theway_noti.webpush.entity.Subscription;
import org.silkroadpartnership.theway_noti.webpush.entity.WebpushSubscription;
import org.silkroadpartnership.theway_noti.webpush.repository.PushHistoryRepository;
import org.silkroadpartnership.theway_noti.webpush.repository.WebpushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebpushService {
  private final WebpushSubscriptionRepository webpushSubscriptionRepository;
  private final PushHistoryRepository pushHistoryRepository;

  @Value("${push.vapid.public-key}")
  private String publicKey;

  @Value("${push.vapid.private-key}")
  private String privateKey;

  @Value("${push.vapid.subject}")
  private String subject;

  public void subscribe(Subscription subscription, User user, String browserInfo) {

    WebpushSubscription webpushSubscription = WebpushSubscription.builder()
        .deviceName(subscription.getDeviceName())
        .browserInfo(browserInfo)
        .endpoint(subscription.getEndpoint())
        .p256dh(subscription.getKeys().getP256dh())
        .auth(subscription.getKeys().getAuth())
        .user(user)
        .build();
    webpushSubscriptionRepository.save(webpushSubscription);
  }

  public void sendPushNotification(WebpushSubscription subscription, Role role, String title, String message)
      throws Exception {
    // BouncyCastle 프로바이더를 추가 (암호화 관련)
    Security.addProvider(new BouncyCastleProvider());

    UUID pushId = UUID.randomUUID();

    String payload = payloadJson(title, message, pushId);

    // Notification 객체 생성 (엔드포인트, p256dh, auth, 메시지 바이트 배열)
    Notification notification = new Notification(
        subscription.getEndpoint(),
        subscription.getP256dh(),
        subscription.getAuth(),
        payload.getBytes(StandardCharsets.UTF_8));

    // PushService 설정: VAPID 공개/비공개키 및 subject 지정
    PushService pushService = new PushService()
        .setPublicKey(Utils.loadPublicKey(publicKey))
        .setPrivateKey(Utils.loadPrivateKey(privateKey))
        .setSubject(subject);

    // 푸시 전송
    HttpResponse response = pushService.send(notification);
    if (response.getEntity() != null) {
      pushHistoryRepository
          .save(
              PushHistory.builder()
                  .id(pushId)
                  .user(subscription.getUser())
                  .role(role)
                  .checkYn(0)
                  .build());
      String responseBody = EntityUtils.toString(response.getEntity());
      log.info(responseBody);
    }
  }

  private String payloadJson(String title, String message, UUID pushId) throws JsonProcessingException {
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", title);
    payload.put("body", message);
    payload.put("openUrl", "/webpush/open-url");
    payload.put("closeUrl", "/webpush/close-url");
    payload.put("pushId", pushId);

    // JSON 문자열로 변환
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(payload);
  }

  public List<WebpushSubscription> findByUser(User user) {
    return webpushSubscriptionRepository.findByUser(user);
  }

  public List<WebpushSubscription> findAll() {
    return webpushSubscriptionRepository.findAll();
  }

  public WebpushSubscription findById(long id) {
    return webpushSubscriptionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No such subscription"));
  }

  public void deleteSubscription(Long id) {
    WebpushSubscription subscription = webpushSubscriptionRepository
        .findById(id).orElseThrow(() -> new IllegalArgumentException("No such subscription"));
    webpushSubscriptionRepository.delete(subscription);
  }

  public String parseUserAgent(String userAgentString) {
    UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
    Browser browser = userAgent.getBrowser();
    Version version = userAgent.getBrowserVersion();
    OperatingSystem os = userAgent.getOperatingSystem();

    return String.format("%s %s on %s",
        browser.getName(),
        (version != null ? version.getVersion() : "unknown"),
        os.getName());
  }

  @Transactional
  public void updateCheckYn(UUID pushId, boolean checkYn) {
    PushHistory pushHistory = pushHistoryRepository.findById(pushId)
        .orElseThrow(() -> new NoSuchElementException("히스토리 없음"));
    pushHistory.setCheckYn(checkYn ? 1 : 0);
  }

  public List<PushHistory> findTodayPushHistoryByRole(Role role) {
    LocalDate today = LocalDate.now();
    LocalDateTime start = today.atStartOfDay();
    LocalDateTime end = today.atTime(LocalTime.MAX);

    return pushHistoryRepository.findByCreateDateBetweenAndRole(start, end, role);
  }

}
