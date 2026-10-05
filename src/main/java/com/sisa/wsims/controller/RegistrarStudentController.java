package com.sisa.wsims.controller;

import com.sisa.wsims.entity.Student;
import com.sisa.wsims.entity.StudentStatus;
import com.sisa.wsims.entity.User;
import com.sisa.wsims.repository.BehaviourNoteRepository;
import com.sisa.wsims.repository.UserRepository;
import com.sisa.wsims.service.StudentRegistrationService;
import com.sisa.wsims.service.dto.GuardianEditRequest;
import com.sisa.wsims.service.dto.StudentEditRequest;
import com.sisa.wsims.service.dto.StudentRegistrationRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Registrar (and Principal) desk for Student Information Management —
 * registration, profile edits, guardian management, transfers and archiving
 * (report FR-03, FR-04, section 6.3). Access is already scoped to
 * PRINCIPAL/REGISTRAR by SecurityConfig's /registrar/** rule.
 */
@Controller
@RequestMapping("/registrar/students")
public class RegistrarStudentController {

    private final UserRepository userRepository;
    private final StudentRegistrationService studentRegistrationService;
    private final BehaviourNoteRepository behaviourNoteRepository;

    public RegistrarStudentController(UserRepository userRepository,
                                      StudentRegistrationService studentRegistrationService,
                                      BehaviourNoteRepository behaviourNoteRepository) {
        this.userRepository = userRepository;
        this.studentRegistrationService = studentRegistrationService;
        this.behaviourNoteRepository = behaviourNoteRepository;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String status,
                       Authentication authentication, Model model) {
        model.addAttribute("user", currentUser(authentication));
        model.addAttribute("activeItem", "students");

        StudentStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = StudentStatus.valueOf(status.toUpperCase());
        }
        model.addAttribute("students", studentRegistrationService.search(q, statusFilter));
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", StudentStatus.values());
        return "registrar/students";
    }

    @GetMapping("/new")
    public String newForm(Authentication authentication, Model model) {
        model.addAttribute("user", currentUser(authentication));
        model.addAttribute("activeItem", "students");
        model.addAttribute("request", new StudentRegistrationRequest());
        model.addAttribute("classOptions", studentRegistrationService.classOptions());
        return "registrar/student-new";
    }

    @PostMapping("/new")
    public String register(@ModelAttribute("request") StudentRegistrationRequest request,
                           Authentication authentication, Model model) {
        model.addAttribute("user", currentUser(authentication));
        model.addAttribute("activeItem", "students");
        try {
            var result = studentRegistrationService.register(request, currentUser(authentication));
            model.addAttribute("result", result);
            model.addAttribute("request", new StudentRegistrationRequest());
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("request", request);
        }
        model.addAttribute("classOptions", studentRegistrationService.classOptions());
        return "registrar/student-new";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Authentication authentication, Model model) {
        model.addAttribute("user", currentUser(authentication));
        model.addAttribute("activeItem", "students");
        Student student = studentRegistrationService.getOrThrow(id);
        model.addAttribute("student", student);
        model.addAttribute("guardian", studentRegistrationService.getGuardianOrNull(student));
        model.addAttribute("classOptions", studentRegistrationService.classOptions());
        // Student Information Management's "discipline records" (System Functions doc,
        // Principal) — read-only here, added by the Teacher via /teacher/academic/report-cards/{id}/notes.
        model.addAttribute("behaviourNotes", behaviourNoteRepository.findByStudent_StudentIdOrderByCreatedAtDesc(id));
        return "registrar/student-detail";
    }

    @PostMapping("/{id}/edit")
    public String editDetails(@PathVariable String id,
                              @ModelAttribute StudentEditRequest request,
                              Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            studentRegistrationService.updateStudentDetails(id, request, currentUser(authentication));
            redirectAttributes.addFlashAttribute("success", "Student details updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/registrar/students/" + id;
    }

    @PostMapping("/{id}/guardian")
    public String editGuardian(@PathVariable String id,
                               @ModelAttribute GuardianEditRequest request,
                               Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            studentRegistrationService.updateGuardianDetails(id, request, currentUser(authentication));
            redirectAttributes.addFlashAttribute("success", "Guardian details updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/registrar/students/" + id;
    }

    @PostMapping("/{id}/transfer-out")
    public String transferOut(@PathVariable String id, @RequestParam(required = false) String note,
                              Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            studentRegistrationService.transferOut(id, note, currentUser(authentication));
            redirectAttributes.addFlashAttribute("success", "Student " + id + " marked as transferred out of SISA.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/registrar/students/" + id;
    }

    @PostMapping("/{id}/transfer-in")
    public String transferIn(@PathVariable String id, @RequestParam(required = false) String note,
                             Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            studentRegistrationService.transferIn(id, note, currentUser(authentication));
            redirectAttributes.addFlashAttribute("success", "Student " + id + " marked as active / transferred in.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/registrar/students/" + id;
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable String id, @RequestParam(required = false) String note,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            studentRegistrationService.archive(id, note, currentUser(authentication));
            redirectAttributes.addFlashAttribute("success", "Student " + id + " archived.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/registrar/students/" + id;
    }
}
