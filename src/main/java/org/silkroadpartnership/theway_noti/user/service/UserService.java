package org.silkroadpartnership.theway_noti.user.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.user.entity.UserManageDto;
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

  public List<User> findByNickname(String nickname) {
    return userRepository.findByNickname(nickname);
  }

  public List<User> findByRole(Role role) {
    return userRepository.findByRole(role.name());
  }

  public List<UserManageDto> findAllByManageDto() {
    return userRepository.findAll().stream()
        .filter(user -> !"admin".equals(user.getUsername()))
        .map(user -> UserManageDto.builder()
            .id(user.getId())
            .name(user.getUsername())
            .nickname(String.join(",", user.getNickname()))
            .admin(user.getPermissions().contains("ADMIN"))
            .build())
        .collect(Collectors.toList());
  }

  @Transactional
  public void toggleAdmin(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    Set<String> updated = new HashSet<>(user.getPermissions());
    if (updated.contains("ADMIN")) {
      updated.remove("ADMIN");
    } else {
      updated.add("ADMIN");
    }

    user.getPermissions().clear();
    user.getPermissions().addAll(updated);
  }

  @Transactional
  public void resetPassword(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    user.setPassword(passwordEncoder.encode("12345"));
  }
}
