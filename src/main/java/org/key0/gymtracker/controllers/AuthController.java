package org.key0.gymtracker.controllers;


import org.key0.gymtracker.models.User;
import org.key0.gymtracker.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") User user, BindingResult bindingResult) {
        if (userService.checkUsernameAvailability(user)) {
            bindingResult.rejectValue("username", "error.user", "Użytkownik o takiej nazwie już istnieje!");
        }

        if (userService.checkEmailAvailability(user)) {
            bindingResult.rejectValue("email", "error.user", "Użytkownik o takim e-mail'u już istnieje!");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        userService.registerNewUser(user);

        return "redirect:/login?registered=true";
    }
}
