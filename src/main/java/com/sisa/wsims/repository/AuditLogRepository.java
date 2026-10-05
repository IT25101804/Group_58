package com.sisa.wsims.repository;

import com.sisa.wsims.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {
    List<AuditLogEntry> findTop50ByOrderByTimestampDesc();
    List<AuditLogEntry> findByUserIdOrderByTimestampDesc(String userId);
}