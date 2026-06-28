package org.key0.gymtracker.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/admin-view")
    public String adminView()  { return "admin_view"; }

    @GetMapping("/profile")
    public String profile() { return "profile"; }

    @GetMapping("/plan")
    public String plan() {
        return "plan";
    }

    @GetMapping("/training")
    public String training() {
        return "training";
    }


    @GetMapping("/login")
    public String loginView() {
        return "login";
    }

    @GetMapping("/register")
    public String registerView() {
        return "register";
    }
}
