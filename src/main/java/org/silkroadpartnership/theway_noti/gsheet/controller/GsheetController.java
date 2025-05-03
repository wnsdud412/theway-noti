package org.silkroadpartnership.theway_noti.gsheet.controller;

import java.util.List;
import java.util.Map;

import org.silkroadpartnership.theway_noti.gsheet.service.GsheetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/gsheet")
@RequiredArgsConstructor
@Slf4j
public class GsheetController {

  private final GsheetService gsheetService;

  @GetMapping("")
  public String index(Model model) {
    model.addAttribute("schedule", gsheetService.getThisWeekSchedule());
    return "gsheet";
  }

  @GetMapping("/schedules")
  @ResponseBody
  public Map<String,List<Object>> getschedule() {
    return gsheetService.getThisWeekSchedule();
  }

}
