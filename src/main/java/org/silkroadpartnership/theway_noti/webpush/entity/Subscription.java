package org.silkroadpartnership.theway_noti.webpush.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Subscription {
  private String deviceName;
  private String endpoint;
  private Keys keys;

  public static class Keys {
      private String p256dh;
      private String auth;

      public Keys() {}

      public String getP256dh() {
          return p256dh;
      }
      public void setP256dh(String p256dh) {
          this.p256dh = p256dh;
      }

      public String getAuth() {
          return auth;
      }
      public void setAuth(String auth) {
          this.auth = auth;
      }
  }
}
