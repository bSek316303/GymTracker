package org.key0.gymtracker.controllers;

import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WeightRepository;
import org.key0.gymtracker.repositories.WorkoutPlanRepository;
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
public class HomeController {

    private UserRepository userRepository;
    private WeightRepository weightRepository;
    private WorkoutPlanRepository workoutPlanRepository;

    public HomeController (UserRepository userRepository, WeightRepository weightRepository, WorkoutPlanRepository workoutPlanRepository){
        this.userRepository = userRepository;
        this.weightRepository = weightRepository;
        this.workoutPlanRepository = workoutPlanRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/admin-view")
    public String adminView()  { return "admin_view"; }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + currentUser.getUsername()));
        List<Weight> weights = weightRepository.findByUserIdOrderByWeightAsc(user.getId());
        model.addAttribute("user", currentUser);

        Double latestWeight = null;
        LocalDate latestWeightDate = null;
        if (!weights.isEmpty()) {
            latestWeight = weights.get(weights.size() - 1).getWeight();
            latestWeightDate = weights.get(weights.size() -1).getWeightDate();
        }

        // Wrzucamy czystą wartość do modelu pod prostą nazwą
        model.addAttribute("latestWeight", latestWeight);
        model.addAttribute("latestWeightDate", latestWeightDate);
        return "profile";
    }

    @GetMapping("/plan")
    public String plan(Model model, @AuthenticationPrincipal UserDetails currentUser, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + currentUser.getUsername()));
        Optional<WorkoutPlan> oPlan = workoutPlanRepository.findByUserId(user.getId());
        if(oPlan.isPresent()) {
            model.addAttribute("plan", oPlan.get());
        } else {
            model.addAttribute("plan", null);
            redirectAttributes.addFlashAttribute("warningMessage", "Nie znaleziono planu treningowego dla użytkownika");
            return "plan";
        }
        return "plan";
    }

    @GetMapping("/training")
    public String training() {
        return "training";
    }
}
