package org.silkroadpartnership.theway_noti.user.component;

import java.util.List;
import java.util.Set;

import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class UserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    if (userRepository.count() == 0) {
      userRepository.save(
          User.builder()
              .username("admin")
              .password(passwordEncoder.encode("admin"))
              .enabled(true)
              .permissions(Set.of("USER", "ADMIN"))
              .nickname(List.of("관리자"))
              .build());
    }
  }
}
