package org.key0.gymtracker.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.PlanExerciseRepository;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WeightRepository;
import org.key0.gymtracker.repositories.WorkoutPlanRepository;
import org.key0.gymtracker.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@AllArgsConstructor
public class HomeController {
    private final WeightRepository weightRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/admin-view")
    public String adminView()  { return "admin_view"; }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userService.getUser(currentUser);
        List<Weight> weights = weightRepository.findByUserIdOrderByWeightDateAsc(user.getId());
        model.addAttribute("user", currentUser);

        Double latestWeight = null;
        LocalDate latestWeightDate = null;
        if (!weights.isEmpty()) {
            latestWeight = weights.getLast().getWeight();
            latestWeightDate = weights.getLast().getWeightDate();
        }

        // Wrzucamy czystą wartość do modelu pod prostą nazwą
        model.addAttribute("latestWeight", latestWeight);
        model.addAttribute("latestWeightDate", latestWeightDate);
        return "profile";
    }

    @GetMapping("/plan")
    public String plan(Model model, @AuthenticationPrincipal UserDetails currentUser, RedirectAttributes redirectAttributes) {
        User user = userService.getUser(currentUser);
        Optional<WorkoutPlan> oPlan = workoutPlanRepository.findByUserId(user.getId());

        if(oPlan.isEmpty()) {
            redirectAttributes.addFlashAttribute("warningMessage", "Nie znaleziono planu treningowego dla użytkownika");
            model.addAttribute("workoutPlan", null);
        } else {
            model.addAttribute("workoutPlan", oPlan.get());
        }

        model.addAttribute("exercises", null);
        model.addAttribute("currentDay", 0);

        return "plan";
    }

    @GetMapping("/training")
    public String training() {
        return "training";
    }
}
