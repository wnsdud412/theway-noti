package org.silkroadpartnership.theway_noti.shedule.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DayType {
  WEEK("매주"),
  MONTH("매월");

  private final String koreanName;
}
