package org.silkroadpartnership.theway_noti.user.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserManageDto {
  private Long id;
  private String name;
  private String nickname;
  private boolean admin;
}
