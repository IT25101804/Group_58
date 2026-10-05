package com.sisa.wsims.service;

import com.sisa.wsims.entity.AttendanceRecord;
import com.sisa.wsims.entity.User;
import com.sisa.wsims.event.AttendanceMarkedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/** Thin wrapper so AttendanceService doesn't depend on Spring's eventing API directly. */
@Service
public class AttendanceEventPublisher {

    private final ApplicationEventPublisher publisher;

    public AttendanceEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishAttendanceMarked(List<AttendanceRecord> absentOrLateRecords, User markedBy) {
        if (absentOrLateRecords.isEmpty()) return;
        publisher.publishEvent(new AttendanceMarkedEvent(this, absentOrLateRecords, markedBy));
    }
}
