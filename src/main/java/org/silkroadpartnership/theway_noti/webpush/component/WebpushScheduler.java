package org.silkroadpartnership.theway_noti.webpush.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebpushScheduler {
  @Scheduled(cron = "0 0 14-16 * * *")  
  public void pushBySchedule(){
    //push 할거리 목록 확인하고
    //push 이력 확인하고
    //push 하고
    //push 이력 남기고
  }
}
