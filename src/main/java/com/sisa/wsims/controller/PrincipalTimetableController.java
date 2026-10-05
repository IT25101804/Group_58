package com.sisa.wsims.controller;

import com.sisa.wsims.entity.ResourceType;
import com.sisa.wsims.entity.TimetableSlot;
import com.sisa.wsims.entity.User;
import com.sisa.wsims.repository.ResourceRepository;
import com.sisa.wsims.repository.StudentRepository;
import com.sisa.wsims.repository.TeacherRepository;
import com.sisa.wsims.repository.UserRepository;
import com.sisa.wsims.service.TimetableService;
import com.sisa.wsims.service.dto.TimetableSlotForm;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Principal-only timetable builder (report section 6.2, business rule 1): assign a
 * teacher + room to each class/day/period, with an inline conflict check (business
 * rule 2) before every save. Access is already scoped to PRINCIPAL by SecurityConfig's
 * /principal/** rule.
 */
@Controller
@RequestMapping("/principal/timetable")
public class PrincipalTimetableController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ResourceRepository resourceRepository;
    private final TimetableService timetableService;

    public PrincipalTimetableController(UserRepository userRepository, StudentRepository studentRepository,
                                        TeacherRepository teacherRepository, ResourceRepository resourceRepository,
                                        TimetableService timetableService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.resourceRepository = resourceRepository;
        this.timetableService = timetableService;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping
    public String builder(@RequestParam(required = false) String className, Authentication authentication, Model model) {
        model.addAttribute("user", currentUser(authentication));
        model.addAttribute("activeItem", "timetable");
        model.addAttribute("allClassNames", studentRepository.distinctClassNames());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("bookableRooms", resourceRepository.findAllByOrderByTypeAscNameAsc().stream()
                .filter(r -> r.getType() != ResourceType.EQUIPMENT).toList());
        model.addAttribute("periods", periodRange());
        model.addAttribute("days", TimetableService.SCHOOL_DAYS);
        model.addAttribute("className", className);

        if (className != null && !className.isBlank()) {
            model.addAttribute("grid", timetableService.asGrid(timetableService.slotsForClass(className)));
            model.addAttribute("form", new TimetableSlotForm());
        }
        return "principal/timetable";
    }

    @PostMapping("/slot")
    public String saveSlot(@ModelAttribute("form") TimetableSlotForm form, Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            timetableService.upsertSlot(form);
            redirectAttributes.addFlashAttribute("success",
                    form.getSubject() + " saved for " + form.getDayOfWeek() + " period " + form.getPeriodNumber() + ".");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/principal/timetable?className=" + form.getClassName();
    }

    @PostMapping("/slot/{id}/delete")
    public String deleteSlot(@PathVariable Long id, @RequestParam String className, RedirectAttributes redirectAttributes) {
        timetableService.deleteSlot(id);
        redirectAttributes.addFlashAttribute("success", "Slot removed.");
        return "redirect:/principal/timetable?className=" + className;
    }

    static java.util.List<Integer> periodRange() {
        java.util.List<Integer> periods = new java.util.ArrayList<>();
        for (int i = 1; i <= TimetableService.MAX_PERIODS; i++) periods.add(i);
        return periods;
    }
}
