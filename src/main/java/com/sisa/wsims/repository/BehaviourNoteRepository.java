package com.sisa.wsims.repository;

import com.sisa.wsims.entity.BehaviourNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BehaviourNoteRepository extends JpaRepository<BehaviourNote, Long> {
    List<BehaviourNote> findByStudent_StudentIdOrderByCreatedAtDesc(String studentId);
}
