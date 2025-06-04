package org.silkroadpartnership.theway_noti.shedule.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TargetType {
  LINE_UP("순번표 기준"),
  ROLE("해당 역할 전체");

  private final String koreanName;
}
