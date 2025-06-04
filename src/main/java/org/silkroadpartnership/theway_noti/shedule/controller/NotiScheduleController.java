package org.silkroadpartnership.theway_noti.shedule.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.shedule.entity.ScheduleDto;
import org.silkroadpartnership.theway_noti.shedule.service.NotiScheduleService;
import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
@Slf4j
public class NotiScheduleController {
  private final NotiScheduleService notiScheduleService;

  @GetMapping("")
  public String getScheduleList(Model model) {
    List<ScheduleDto> schedules = notiScheduleService.findSchedules();
    List<Map<String, String>> roles = Arrays.stream(Role.values())
        .map(role -> {
          Map<String, String> roleMap = new HashMap<>();
          roleMap.put("code", role.name());
          roleMap.put("name", role.getKoreanName());
          return roleMap;
        })
        .collect(Collectors.toList());
    model.addAttribute("schedules", schedules);
    model.addAttribute("roles", roles);
    return "admin/schedule";
  }

  @PostMapping("/save")
  public String saveNewSchedule(@RequestBody ScheduleDto dto, HttpServletRequest request) {

    notiScheduleService.saveSchedule(dto);
    
    String referer = request.getHeader("Referer");
    return "redirect:" + referer;
  }

  @PostMapping("/delete")
  public String deleteSchedule(@RequestParam String scheduleId, HttpServletRequest request) {
    notiScheduleService.deleteSchedule(scheduleId);
    
    String referer = request.getHeader("Referer");
    return "redirect:" + referer;
  }

  @GetMapping("/test")
  @ResponseBody
  public ResponseEntity<String> getMethodName() {
      return ResponseEntity.ok(String.join(",",notiScheduleService.findByNow().stream().map(schld->schld.toString()).toList()));
  }
  
}
