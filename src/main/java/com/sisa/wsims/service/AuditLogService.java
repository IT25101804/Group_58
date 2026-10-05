package com.sisa.wsims.service;

import com.sisa.wsims.entity.AuditLogEntry;
import com.sisa.wsims.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String userId, String performedByUserId, String action, String detail) {
        auditLogRepository.save(new AuditLogEntry(userId, performedByUserId, action, detail));
    }
}