package org.silkroadpartnership.theway_noti.webpush.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.gsheet.service.GsheetService;
import org.silkroadpartnership.theway_noti.shedule.entity.NotiSchedule;
import org.silkroadpartnership.theway_noti.shedule.entity.TargetType;
import org.silkroadpartnership.theway_noti.shedule.service.NotiScheduleService;
import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.user.service.UserService;
import org.silkroadpartnership.theway_noti.webpush.entity.WebpushSubscription;
import org.silkroadpartnership.theway_noti.webpush.service.WebpushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component @RequiredArgsConstructor @Slf4j
public class WebpushScheduler {

  private final GsheetService gsheetService;
  private final UserService userService;
  private final WebpushService webpushService;
  private final NotiScheduleService notiScheduleService;

  @Scheduled(cron = "10 0 * * * *", zone = "Asia/Seoul")
  public void pushByNotiSchedule() {

    List<NotiSchedule> notiSchedules = notiScheduleService.findByNow();
    Map<String, String> failSchedule = new HashMap<>();
    failSchedule.put("inst_fill", "");
    failSchedule.put("inst_regist", "");
    failSchedule.put("sing_fill", "");
    failSchedule.put("sing_regist", "");

    for (NotiSchedule schdl : notiSchedules) {
      log.info("알림 스케줄링 시작 : " + schdl.toString());
      TargetType targetType = schdl.getTargetType();
      Role scheduledRole = schdl.getRole();

      if (TargetType.LINE_UP == targetType) {
        String[] rotationNames = getRotationName(scheduledRole);
        if (rotationNames.length == 1 && "".equals(rotationNames[0])) {
          // 순번표 미등록
          if (!"manage".equals(scheduledRole.getGroup()) && scheduledRole.isRequired()) {
            failSchedule.put(scheduledRole.getGroup() + "_fill",
                failSchedule.get(scheduledRole.getGroup() + "_fill") + ", " + scheduledRole.getKoreanName());
          }
          continue;
        }
        for (String rotationName : rotationNames) {
          List<User> users = userService.findByNickname(rotationName);
          if (users.isEmpty()) {
            // 순번표에 적힌 이름을 가진 사용자가 없음
            if (!"manage".equals(scheduledRole.getGroup()) && scheduledRole.isRequired()) {
              failSchedule.put(scheduledRole.getGroup() + "_regist",
                  failSchedule.get(scheduledRole.getGroup() + "_regist") + ", " + rotationName);
            }
            continue;
          }
          for (User user : users) {
            List<WebpushSubscription> subscriptions = webpushService.findByUser(user);
            if (subscriptions.isEmpty()) {
              // 등록된 기기 없음
              if (!"manage".equals(scheduledRole.getGroup()) && scheduledRole.isRequired()) {
                failSchedule.put(scheduledRole.getGroup() + "_regist",
                    failSchedule.get(scheduledRole.getGroup() + "_regist") + ", " + rotationName);
              }
              continue;
            }
            for (WebpushSubscription subscription : subscriptions) {
              try {
                String roleText = scheduledRole.getKoreanName().split("\\(")[0];
                webpushService.sendPushNotification(subscription, scheduledRole, roleText + " 순번 알림",
                    StringUtils.hasText(schdl.getContent()) ? schdl.getContent()
                        : rotationName + "님, 이번주 " + roleText + " 담당 입니다.");
              } catch (Exception e) {
                log.error("배치 발송중 오류 발생", e);
              }
            }
          }
        }
      } else if (TargetType.ROLE == targetType) {
        List<User> users = userService.findByRole(scheduledRole);
        for (User user : users) {
          List<WebpushSubscription> subscriptions = webpushService.findByUser(user);
          if (subscriptions.isEmpty()) {
            // 등록된 기기 없음
            if (!"manage".equals(scheduledRole.getGroup())) {
              failSchedule.put(scheduledRole.getGroup() + "_regist",
                  failSchedule.get(scheduledRole.getGroup() + "_regist") + ", " + user.getNickname().getFirst());
            }
            continue;
          }
          for (WebpushSubscription subscription : subscriptions) {
            try {
              String roleText = scheduledRole.getKoreanName().split("\\(")[0];
              webpushService.sendPushNotification(subscription, scheduledRole, roleText + " 전체 알림", schdl.getContent());
            } catch (Exception e) {
              log.error("배치 발송중 오류 발생", e);
            }
          }
        }
      }
    }

    for (Map.Entry<String, String> entry : failSchedule.entrySet()) {
      Role managerRole;
      String title = "";
      String key = entry.getKey();
      String message = entry.getValue();

      if (message.length() < 1) {
        continue;
      }
      message = message.substring(2);

      if ("inst".equals(key.split("_")[0])) {
        managerRole = Role.INSTRUMENT_MANAGER;
        title += "악기";
      } else {
        managerRole = Role.SINGER_MANAGER;
        title += "싱어";
      }

      if ("fill".equals(key.split("_")[1])) {
        title += " 순번 미등록 알림";
        message += " 의 다음주 순번이 기입되지 않아, 알림 발송이 실패하였습니다.";
      } else {
        title += " 순번 알림 실패";
        message += " 님 혹은 기기가 시스템에 등록되어 있지 않아, 알림 발송이 실패하였습니다.";
      }
      sendToRoleManager(managerRole, title, message);
    }
  }

  private void sendToRoleManager(Role role, String title, String message) {
    userService.findByRole(role).forEach(user -> {
      webpushService.findByUser(user).forEach(subscription -> {
        try {
          webpushService.sendPushNotification(subscription, role, title, message);
        } catch (Exception e) {
          log.error("배치 발송중 오류 발생", e);
        }
      });
    });
  }

  private String[] getRotationName(Role role) {
    String roleKorean = role.getKoreanName();

    Map<String, List<Object>> thisWeekSchedule = gsheetService.getThisWeekSchedule();
    int roleIndex = thisWeekSchedule.get("header").stream().map(Object::toString).collect(Collectors.toList()).indexOf(
        roleKorean);
    String rotationName = thisWeekSchedule.get("value").get(roleIndex).toString();
    return rotationName.split(",");
  }
}
