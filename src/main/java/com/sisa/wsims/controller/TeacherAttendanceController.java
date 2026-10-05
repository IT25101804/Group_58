package com.sisa.wsims.controller;

import com.sisa.wsims.entity.*;
import com.sisa.wsims.repository.UserRepository;
import com.sisa.wsims.service.AttendanceService;
import com.sisa.wsims.service.dto.AttendanceMarkForm;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 - Teacher (and Principal, view-only) desk for Attendance Management (report FR-05/FR-06,
 - section 6.4). Access is already scoped to PRINCIPAL/TEACHER by SecurityConfig's
 - /teacher/** rule; fine-grained "only the Class Teacher for this exact class" enforcement
 - happens in AttendanceService and surfaces here as a 403.
 */
@Controller
@RequestMapping("/teacher/attendance")
public class TeacherAttendanceController {

    private final UserRepository userRepository;
    private final AttendanceService attendanceService;

    // Wiring up repositories and services using constructor injection
    public TeacherAttendanceController(UserRepository userRepository, AttendanceService attendanceService) {
        this.userRepository = userRepository;
        this.attendanceService = attendanceService;
    }

    // Grab the currently logged-in user details from Spring Security authentication
    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping//R
    public String view(@RequestParam(required = false) String className,
                       @RequestParam(required = false) String date,
                       Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("activeItem", "attendance");

        LocalDate today = LocalDate.now();
        LocalDate resolvedDate = parseDateOrToday(date, today);
        model.addAttribute("date", resolvedDate.toString());
        model.addAttribute("todayIso", today.toString());
        model.addAttribute("isToday", resolvedDate.equals(today));

        boolean canMark;
        String resolvedClassName;

        // Principal can view all classes (read-only), but teachers can only manage their assigned class
        if (user.getRole() == Role.PRINCIPAL) {
            resolvedClassName = className;
            canMark = false;
            model.addAttribute("allClassNames", attendanceService.distinctClassNames());
        } else {
            Teacher teacher = attendanceService.requireTeacher(user);
            resolvedClassName = teacher.getAssignedClassName();
            canMark = teacher.isClassTeacher();
            model.addAttribute("teacher", teacher);
        }
        model.addAttribute("className", resolvedClassName);
        model.addAttribute("canMark", canMark);

        if (resolvedClassName == null || resolvedClassName.isBlank()) {
            return "teacher/attendance";
        }

        List<Student> roster = attendanceService.rosterFor(resolvedClassName);
        Map<String, AttendanceRecord> onDate = attendanceService.recordsByStudentFor(resolvedClassName, resolvedDate);

        // Build the form to mark attendance for each student in the class list
        AttendanceMarkForm form = new AttendanceMarkForm();
        for (Student student : roster) {
            AttendanceMarkForm.Entry entry = new AttendanceMarkForm.Entry();
            entry.setStudentId(student.getStudentId());
            entry.setFullName(student.getUser().getFullName());
            AttendanceRecord existing = onDate.get(student.getStudentId());
            entry.setStatus(existing != null ? existing.getStatus().name() : "PRESENT");
            form.getEntries().add(entry);
        }

        model.addAttribute("roster", roster);
        model.addAttribute("form", form);
        model.addAttribute("alreadyMarked", !onDate.isEmpty());
        return "teacher/attendance";
    }

    @PostMapping("/mark")//CUD — a row's status can create, update, or (submitted blank) delete that day's record
    public String mark(@RequestParam String className, @RequestParam(required = false) String date,
                       @ModelAttribute("form") AttendanceMarkForm form,
                       Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        LocalDate resolvedDate = parseDateOrToday(date, LocalDate.now());
        try {
            // Save or update attendance records submitted by the teacher
            attendanceService.markOrUpdate(className, resolvedDate, form, user);
            redirectAttributes.addFlashAttribute("success", "Attendance saved for " + className + " on " + resolvedDate + ".");
        } catch (ResponseStatusException rse) {
            throw rse; // authorization failure — let it surface as its real HTTP status
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/attendance?className=" + className + "&date=" + resolvedDate;
    }

    // Helper method to parse the date string safely, falling back to today if invalid or empty
    private LocalDate parseDateOrToday(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    // Load list of students who have frequent absences
    @GetMapping("/often-absent")
    public String oftenAbsent(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("activeItem", "attendance");

        String className = user.getRole() == Role.PRINCIPAL ? null : attendanceService.requireTeacher(user).getAssignedClassName();
        model.addAttribute("className", className);
        model.addAttribute("entries", attendanceService.oftenAbsent(className));
        return "teacher/attendance-often-absent";
    }

    // Show pending attendance correction requests submitted by students
    @GetMapping("/corrections")
    public String corrections(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("activeItem", "attendance");

        String className = user.getRole() == Role.TEACHER ? attendanceService.requireTeacher(user).getAssignedClassName() : null;
        model.addAttribute("canReview", user.getRole() == Role.TEACHER);
        model.addAttribute("requests", attendanceService.pendingCorrectionsFor(className));
        return "teacher/attendance-corrections";
    }

    // Handle approval of a student's correction request
    @PostMapping("/corrections/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String note,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        return review(id, true, note, authentication, redirectAttributes);
    }

    // Handle rejection of a student's correction request
    @PostMapping("/corrections/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String note,
                         Authentication authentication, RedirectAttributes redirectAttributes) {
        return review(id, false, note, authentication, redirectAttributes);
    }

    // Core method to process teacher's review (approve/reject) on correction requests
    private String review(Long id, boolean approve, String note, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            attendanceService.reviewCorrection(id, approve, note, currentUser(authentication));
            redirectAttributes.addFlashAttribute("success", approve ? "Correction approved." : "Correction rejected.");
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/attendance/corrections";
    }
}