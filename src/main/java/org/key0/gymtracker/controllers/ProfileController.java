package org.key0.gymtracker.controllers;

import lombok.AllArgsConstructor;
import org.key0.gymtracker.models.BodyMeasurement;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WeightRepository;
import org.key0.gymtracker.services.BodyMeasurementService;
import org.key0.gymtracker.services.UserService;
import org.key0.gymtracker.services.WeightService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@AllArgsConstructor
public class ProfileController {
    private final UserService userService;
    private final WeightService weightService;
    private final BodyMeasurementService bodyMeasurementService;

    @PostMapping("/update-weight")
    public String updateWeight(@AuthenticationPrincipal UserDetails currentUser, Double weightValue, RedirectAttributes redirectAttributes) {
        try {
            weightService.addWeightToUser(currentUser.getUsername(), weightValue);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Wystąpił nieoczekiwany błąd");
        }

        return "redirect:/profile";
    }

    @PostMapping("/update-measurements")
    public String updateMeasurements(@AuthenticationPrincipal UserDetails currentUser, BodyMeasurement measurementForm, RedirectAttributes redirectAttributes) {
        try
        {
            bodyMeasurementService.addMeasurementToUser(currentUser.getUsername(), measurementForm);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Wystąpił nieoczekiwany błąd");
        }

        return "redirect:/profile";
    }
}
