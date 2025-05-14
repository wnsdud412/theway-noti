package org.silkroadpartnership.theway_noti.user.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.silkroadpartnership.theway_noti.user.entity.User;
import org.silkroadpartnership.theway_noti.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/login")
  public String loginPage(Model Model) {
    return "login";
  }

  @GetMapping("/register")
  public String registerForm() {
    return "register";
  }

  @PostMapping("/register")
  public String register(@RequestParam String username,
      @RequestParam String password,
      @RequestParam String nicknames,
      @RequestParam String roles,
      Model model) {
    try {
      userService.registerUser(username, password, nicknames, roles);
      return "redirect:/login"; // 가입 후 로그인 페이지로
    } catch (IllegalArgumentException e) {
      model.addAttribute("error", e.getMessage());
      return "register"; // 다시 회원가입 폼으로
    }
  }

  @GetMapping("/profile")
  public String userProfile(Model model, Principal principal) {
    User user = userService.findByUsername(principal.getName());
    model.addAttribute("nicknames", String.join(",", user.getNickname()));
    model.addAttribute("roles", String.join(",", user.getRoles().stream().map(role -> role.name()).toList()));
    return "profile";
  }

  @PostMapping("/profile/update")
  public String postMethodName(
      @RequestParam String nicknames,
      @RequestParam String roles,
      Principal principal) {
    userService.updateNicknamesAndRoles(principal.getName(), nicknames, roles);
    return "redirect:/";
  }

  @GetMapping("/roles")
  @ResponseBody
  public List<Map<String, String>> getRoles() {
    return Arrays.stream(Role.values())
        .map(role -> {
          Map<String, String> roleMap = new HashMap<>();
          roleMap.put("code", role.name());
          roleMap.put("name", role.getKoreanName());
          return roleMap;
        })
        .collect(Collectors.toList());
  }

  @GetMapping("/users/manage")
  public String getUserManage(Model model, Principal principal) {
    model.addAttribute("users", userService.findAllByManageDto());
    return "manage";
  }

  @PostMapping("/users/toggle-admin")
  public String toggleAdmin(@RequestParam("userId") Long userId, HttpServletRequest request) {
    userService.toggleAdmin(userId);

    String referer = request.getHeader("Referer");
    return "redirect:" + referer;
  }

  @PostMapping("/users/reset-password")
  public String resetPassword(@RequestParam("userId") Long userId, HttpServletRequest request) {
    userService.resetPassword(userId);

    String referer = request.getHeader("Referer");
    return "redirect:" + referer;
  }

  @GetMapping("/profile/password")
  public String getPasswordChange(Model model, Principal principal) {
    return "passwordChange";
  }

  @PostMapping("/profile/password")
  @ResponseBody
  public ResponseEntity<String> passwordChange(
      @RequestParam String before,
      @RequestParam String after,
      @RequestParam String confirm,
      Model model, Principal principal) {
    if (!userService.checkPassword(principal.getName(), before)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("기존 비밀번호가 틀렸습니다.");
    }
    if (!after.equals(confirm)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호와 비밀번호 확인이 서로 다릅니다.");
    }
    userService.changePassword(principal.getName(), after);

    return ResponseEntity.ok("비밀번호 변경 성공");
  }

}
