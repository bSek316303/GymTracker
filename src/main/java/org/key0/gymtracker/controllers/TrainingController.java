package org.key0.gymtracker.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/training")
public class TrainingController {
    @GetMapping("choose-training")
    public String chooseTraining(Model model){
        return "/training"; // wersja robocza
    }
}
