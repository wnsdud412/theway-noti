package org.silkroadpartnership.theway_noti.home.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String getMainPage(Model model) {
    return "index";
  }
  
  @GetMapping("/admin")
  public String getHomePage(Model model) {
    return "admin/index";
  }
}
