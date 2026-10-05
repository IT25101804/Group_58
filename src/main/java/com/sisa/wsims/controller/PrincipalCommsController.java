package com.sisa.wsims.controller;

import com.sisa.wsims.entity.User;
import com.sisa.wsims.repository.StudentRepository;
import com.sisa.wsims.repository.UserRepository;
import com.sisa.wsims.service.AnnouncementService;
import com.sisa.wsims.service.dto.AnnouncementForm;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Principal desk for Communication & Notification Management (report FR-11, business
 * rule 1): post announcements/alerts, optionally scheduled, to the whole school, one
 * class, or one student's guardians — plus an oversight log of every broadcast sent
 * school-wide. Access is already scoped to PRINCIPAL by SecurityConfig's
 * /principal/** rule.
 */
@Controller
@RequestMapping("/principal/comms")
public class PrincipalCommsController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AnnouncementService announcementService;

    public PrincipalCommsController(UserRepository userRepository, StudentRepository studentRepository,
                                    AnnouncementService announcementService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.announcementService = announcementService;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping
    public String log(Authentication authentication, Model model) {
        model.addAttribute("user", currentUser(authentication));
        model.addAttribute("activeItem", "comm");
        model.addAttribute("broadcasts", announcementService.allBroadcasts());
        return "principal/comms";
    }

    @GetMapping("/new")
    public String newForm(Authentication authentication, Model model) {
        model.addAttribute("user", currentUser(authentication));
        model.addAttribute("activeItem", "comm");
        model.addAttribute("allClassNames", studentRepository.distinctClassNames());
        model.addAttribute("canSchedule", true);
        model.addAttribute("form", new AnnouncementForm());
        return "principal/comms-new";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") AnnouncementForm form, Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("activeItem", "comm");
        model.addAttribute("allClassNames", studentRepository.distinctClassNames());
        model.addAttribute("canSchedule", true);
        try {
            int reached = announcementService.create(form, user);
            model.addAttribute("success", "Sent to " + reached + " recipient(s).");
            model.addAttribute("form", new AnnouncementForm());
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("form", form);
        }
        return "principal/comms-new";
    }
}
