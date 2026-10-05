package com.sisa.wsims.service;

import com.sisa.wsims.entity.*;
import com.sisa.wsims.repository.NotificationRepository;
import com.sisa.wsims.repository.StudentRepository;
import com.sisa.wsims.repository.TeacherRepository;
import com.sisa.wsims.repository.UserRepository;
import com.sisa.wsims.service.dto.AnnouncementForm;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Communication & Notification Management (report FR-11, business rules 1-4): the
 * Principal/Registrar/Teacher's "post an announcement/alert/notice" path, fanning out
 * to one Notification row per resolved recipient. This is the NotificationCenter any
 * module can call for a broadcast; Module 3's single-recipient system alerts keep
 * using the plain Notification constructor directly since they already know their one
 * recipient and don't need scope resolution.
 */
@Service
public class AnnouncementService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public AnnouncementService(NotificationRepository notificationRepository, UserRepository userRepository,
                               StudentRepository studentRepository, TeacherRepository teacherRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    /**
     * Creates one broadcast, fanned out to every resolved recipient (business rule 4).
     * Only PRINCIPAL, REGISTRAR and TEACHER may call this (business rules 1-3); a
     * Teacher may only target their own class (business rule 3 — "class notices").
     * Returns how many recipients were reached.
     */
    @Transactional
    public int create(AnnouncementForm form, User sender) {
        if (sender.getRole() != Role.PRINCIPAL && sender.getRole() != Role.REGISTRAR && sender.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Principal, Registrar or a Teacher can post notices.");
        }
        NotificationCategory category = NotificationCategory.valueOf(form.getCategory());
        NotificationScope scope = NotificationScope.valueOf(form.getTargetScope());

        String className = form.getClassName();
        if (sender.getRole() == Role.TEACHER) {
            // A Teacher's notices are always their own class, per business rule 3 — never the whole school
            // or someone else's class, even if the form tried to say otherwise.
            Teacher teacher = teacherRepository.findById(sender.getUserId())
                    .orElseThrow(() -> new IllegalStateException("No teacher record for " + sender.getUserId()));
            if (!teacher.isClassTeacher() || teacher.getAssignedClassName() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a Class Teacher can post class notices.");
            }
            scope = NotificationScope.CLASS;
            className = teacher.getAssignedClassName();
        }

        Set<String> recipients = resolveRecipients(scope, className, form.getStudentId());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("No recipients found for that target.");
        }

        LocalDateTime scheduledFor = parseScheduledFor(form.getScheduledFor());
        LocalDateTime now = LocalDateTime.now();
        boolean sendNow = scheduledFor == null || !scheduledFor.isAfter(now);
        String broadcastId = UUID.randomUUID().toString();

        for (String recipientId : recipients) {
            Notification notification = new Notification();
            notification.setRecipientUserId(recipientId);
            notification.setSenderUserId(sender.getUserId());
            notification.setCategory(category);
            notification.setSubject(form.getSubject());
            notification.setBody(form.getBody());
            notification.setTargetScope(scope);
            notification.setBroadcastId(broadcastId);
            notification.setScheduledFor(scheduledFor);
            notification.setSentAt(sendNow ? now : null);
            notificationRepository.save(notification);
        }
        return recipients.size();
    }

    /** SCHOOL -> every approved (enabled) user; CLASS -> that class's active students + their linked parents; STUDENT -> that student + their parent. */
    private Set<String> resolveRecipients(NotificationScope scope, String className, String studentId) {
        Set<String> ids = new LinkedHashSet<>();
        switch (scope) {
            case SCHOOL -> userRepository.findByStatus(AccountStatus.APPROVED).forEach(u -> ids.add(u.getUserId()));
            case CLASS -> {
                if (className == null || className.isBlank()) {
                    throw new IllegalArgumentException("A class is required for a class-scoped notice.");
                }
                for (Student student : studentRepository.findByClassNameAndStatusOrderByUser_FullNameAsc(className, StudentStatus.ACTIVE)) {
                    ids.add(student.getStudentId());
                    if (student.getParent() != null) ids.add(student.getParent().getUserId());
                }
            }
            case STUDENT -> {
                if (studentId == null || studentId.isBlank()) {
                    throw new IllegalArgumentException("A student is required for a student-scoped notice.");
                }
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new IllegalArgumentException("No such student: " + studentId));
                ids.add(student.getStudentId());
                if (student.getParent() != null) ids.add(student.getParent().getUserId());
            }
        }
        return ids;
    }

    private LocalDateTime parseScheduledFor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDateTime.parse(raw);
    }

    // ---------- reading back what was sent (the "message log") ----------

    public record Broadcast(String broadcastId, String senderUserId, NotificationCategory category, String subject,
                            String body, NotificationScope targetScope, LocalDateTime scheduledFor,
                            LocalDateTime sentAt, LocalDateTime createdAt, int recipientCount) {}

    private Broadcast toBroadcast(List<Notification> rows) {
        Notification first = rows.get(0);
        return new Broadcast(first.getBroadcastId(), first.getSenderUserId(), first.getCategory(), first.getSubject(),
                first.getBody(), first.getTargetScope(), first.getScheduledFor(), first.getSentAt(), first.getCreatedAt(), rows.size());
    }

    /** Every broadcast sent school-wide, newest first — the Principal's oversight log. */
    public List<Broadcast> allBroadcasts() {
        return groupIntoBroadcasts(notificationRepository.findByBroadcastIdIsNotNullOrderByCreatedAtDesc());
    }

    /** Just this sender's own broadcasts — the Registrar/Teacher "sent" log. */
    public List<Broadcast> broadcastsBySender(String senderUserId) {
        return groupIntoBroadcasts(notificationRepository.findBySenderUserIdAndBroadcastIdIsNotNullOrderByCreatedAtDesc(senderUserId));
    }

    private List<Broadcast> groupIntoBroadcasts(List<Notification> rows) {
        Map<String, List<Notification>> byBroadcast = new LinkedHashMap<>();
        for (Notification n : rows) byBroadcast.computeIfAbsent(n.getBroadcastId(), k -> new ArrayList<>()).add(n);
        List<Broadcast> broadcasts = new ArrayList<>();
        for (List<Notification> group : byBroadcast.values()) broadcasts.add(toBroadcast(group));
        return broadcasts;
    }
}
