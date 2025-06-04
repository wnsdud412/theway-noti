package org.silkroadpartnership.theway_noti.webpush.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.user.service.UserService;
import org.silkroadpartnership.theway_noti.webpush.entity.Subscription;
import org.silkroadpartnership.theway_noti.webpush.entity.WebpushSubscription;
import org.silkroadpartnership.theway_noti.webpush.service.WebpushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/webpush")
@RequiredArgsConstructor
@Slf4j
public class WebpushController {
  private final WebpushService webpushService;
  private final UserService userService;

  @Value("${push.vapid.public-key}")
  private String vapidPublicKey;

  // 메인 페이지에서 VAPID 공개키를 전달
  @GetMapping("")
  public String getWebpushPage(Model model, Principal principal) {
    User user = userService.findByUsername(principal.getName());
    List<WebpushSubscription> subscriptions = webpushService.findByUser(user);
    model.addAttribute("subscriptions", subscriptions);
    model.addAttribute("vapidPublicKey", vapidPublicKey);
    return "webpush";
  }

  // 클라이언트의 구독 정보를 저장
  @PostMapping("/subscribe")
  @ResponseBody
  public ResponseEntity<String> subscribe(HttpServletRequest request, @RequestBody Subscription subscription,
      Principal principal) {
    String userAgentString = request.getHeader("User-Agent");
    String browserInfo = webpushService.parseUserAgent(userAgentString);

    User user = userService.findByUsername(principal.getName());
    webpushService.subscribe(subscription, user, browserInfo);
    return ResponseEntity.ok("구독 등록 완료");
  }

  @PostMapping("/delete/{id}")
  @ResponseBody
  public String deleteSubscription(@PathVariable Long id, Principal principal) {
    webpushService.deleteSubscription(id);
    return "redirect:/";
  }

  @GetMapping("/push-test")
  public String getPushTestPage(Model model) {
    List<WebpushSubscription> subscriptions = webpushService.findAll();
    model.addAttribute("subscriptions", subscriptions);
    return "admin/pushTest";
  }

  // 입력된 메시지를 구독된 모든 클라이언트로 푸시 전송
  @PostMapping("/push")
  @ResponseBody
  public ResponseEntity<String> sendPush(@RequestParam String message) {
    webpushService.findAll().forEach(subscription -> {
      try {
        webpushService.sendPushNotification(subscription, null, "전체 알림 테스트", message);
      } catch (Exception e) {
        log.info("[Push] 푸시 전송 중 오류 발생: " + e.getMessage());
        e.printStackTrace();
      }
    });
    return ResponseEntity.ok("푸시 전송 완료");
  }

  @PostMapping("/push/{id}")
  @ResponseBody
  public ResponseEntity<String> sendPushById(@PathVariable long id, @RequestParam String message) {
    WebpushSubscription subscription = webpushService.findById(id);
    try {
      webpushService.sendPushNotification(subscription, null, "개별 알림 테스트", message);
    } catch (Exception e) {
      log.info("[Push] 푸시 전송 중 오류 발생: " + e.getMessage());
      e.printStackTrace();
    }
    return ResponseEntity.ok("푸시 전송 완료");
  }

  @GetMapping("/open-url")
  public RedirectView openUrl(@RequestParam UUID pushId) {
    webpushService.updateCheckYn(pushId, true);
    return new RedirectView("https://docs.google.com/spreadsheets/d/1WaO_hHdzLxzyZiIPVLFbYk-w4RTQADWGMfFbnpHtoqU");
  }

}
