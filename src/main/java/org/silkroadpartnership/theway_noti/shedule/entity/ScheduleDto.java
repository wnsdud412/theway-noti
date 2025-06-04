package org.silkroadpartnership.theway_noti.shedule.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ScheduleDto {
  private String scheduleId;
  private String targetType;
  private String role;
  private String dayType;
  private String targetDay;
  private String targetHour;
  private String content;
}
