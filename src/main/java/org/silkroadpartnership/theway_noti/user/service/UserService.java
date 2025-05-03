package org.silkroadpartnership.theway_noti.user.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
  }

  public void registerUser(String username, String rawPassword, String nicknames, String roles) {
    if (userRepository.findByUsername(username).isPresent()) {
      throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    }

    String encodedPassword = passwordEncoder.encode(rawPassword);

    User user = User.builder()
        .username(username)
        .password(encodedPassword)
        .enabled(true)
        .permissions(Set.of("USER"))
        .nickname(
            Arrays.stream(nicknames.split(","))
                .map(nameStr -> nameStr.trim())
                .collect(Collectors.toList()))
        .roles(
            Arrays.stream(roles.split(","))
                .map(roleStr -> Role.valueOf(roleStr))
                .collect(Collectors.toSet()))
        .build();

    userRepository.save(user);
  }

  public User findByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
  }

  @Transactional
  public void updateNicknamesAndRoles(String username, String nicknames, String roles) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    user.setNickname(
        Arrays.stream(nicknames.split(","))
            .map(nameStr -> nameStr.trim())
            .collect(Collectors.toList()));
    user.setRoles(
      Arrays.stream(roles.split(","))
          .map(roleStr -> Role.valueOf(roleStr))
          .collect(Collectors.toSet()));
  }
}
