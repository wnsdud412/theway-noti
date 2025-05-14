package org.silkroadpartnership.theway_noti.webpush.component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.gsheet.service.GsheetService;
import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.user.service.UserService;
import org.silkroadpartnership.theway_noti.webpush.entity.PushHistory;
import org.silkroadpartnership.theway_noti.webpush.entity.WebpushSubscription;
import org.silkroadpartnership.theway_noti.webpush.service.WebpushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebpushScheduler {

  private final GsheetService gsheetService;
  private final UserService userService;
  private final WebpushService webpushService;
  @Scheduled(cron = "0 0 15,16 * * THU", zone = "Asia/Seoul")
  public void pushSheetBySchedule() {

    List<PushHistory> todayHistory = webpushService.findTodayPushHistoryByRole(Role.SHEET_MUSIC);
    if (todayHistory.size() > 1) {
      log.info("sheet role already push");
      return;
    }

    Map<String, List<Object>> thisWeekSchedule = gsheetService.getThisWeekSchedule();
    int sheetMusicIndex = thisWeekSchedule.get("header").stream()
        .map(Object::toString)
        .collect(Collectors.toList())
        .indexOf("악보");
    String rotationName = thisWeekSchedule.get("value")
        .get(sheetMusicIndex).toString();

    List<User> users = userService.findByNickname(rotationName);
    if (users.isEmpty()) {
      sendToRoleManager(
          Role.SHEET_MUSIC,
          "악보 순번 알림 실패",
          rotationName + "님께서 알림 시스템에 등록되어 있지 않습니다. 대신 안내 부탁드립니다.");
    } else {
      users.forEach(user -> {
        List<WebpushSubscription> subscriptions = webpushService.findByUser(user);
        if (subscriptions.isEmpty()) {
          sendToRoleManager(
              Role.SHEET_MUSIC,
              "악보 순번 알림 실패",
              rotationName + "님의 기기가 알림 시스템에 등록되어 있지 않습니다. 대신 안내 부탁드립니다.");
        } else {
          subscriptions.forEach(subscription -> {
            try {
              webpushService.sendPushNotification(subscription,
                  Role.SHEET_MUSIC,
                  "악보 순번 알림",
                  rotationName + "님, 이번주 악보 담당 입니다.");
            } catch (Exception e) {
              log.error("배치 발송중 오류 발생", e);
            }
          });
        }
      });
    }
  }

  private void sendToRoleManager(Role role, String title, String message) {
    userService.findByRole(role.getManagerRole()).forEach(user -> {
      webpushService.findByUser(user).forEach(subscription -> {
        try {
          webpushService.sendPushNotification(subscription, role, title, message);
        } catch (Exception e) {
          log.error("배치 발송중 오류 발생", e);
        }
      });
    });
  }
}
