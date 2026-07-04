package org.key0.gymtracker.controllers;

import jakarta.servlet.http.HttpSession;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.PlanExerciseRepository;
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

    private final UserRepository userRepository;
    private final WeightRepository weightRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanExerciseRepository planExerciseRepository;

    public HomeController (UserRepository userRepository, WeightRepository weightRepository, WorkoutPlanRepository workoutPlanRepository, PlanExerciseRepository planExerciseRepository){
        this.userRepository = userRepository;
        this.weightRepository = weightRepository;
        this.workoutPlanRepository = workoutPlanRepository;
        this.planExerciseRepository = planExerciseRepository;
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
        List<Weight> weights = weightRepository.findByUserIdOrderByWeightDateAsc(user.getId());
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
    public String plan(Model model, @AuthenticationPrincipal UserDetails currentUser, RedirectAttributes redirectAttributes, HttpSession httpSession) {
        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + currentUser.getUsername()));

        Optional<WorkoutPlan> oPlan = workoutPlanRepository.findByUserId(user.getId());

        if(oPlan.isEmpty()) {
            redirectAttributes.addFlashAttribute("warningMessage", "Nie znaleziono planu treningowego dla użytkownika");
            return "plan";
        }

        WorkoutPlan plan = oPlan.get();
        model.addAttribute("plan", plan);

        Integer currDay = (Integer) httpSession.getAttribute("currentDay");
        if (currDay == null) {
            currDay = 1;
            httpSession.setAttribute("currentDay", currDay);
        }
        model.addAttribute("currentDay", currDay);

        final Integer currentDay = currDay;

        List<PlanExercise> filteredExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId())
                .stream()
                .filter(pe -> currentDay.equals(pe.getDayNumber()))
                .toList();

        model.addAttribute("exercises", filteredExercises);

        return "plan";
    }

    @GetMapping("/training")
    public String training() {
        return "training";
    }
}
