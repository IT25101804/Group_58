package com.sisa.wsims.repository;

import com.sisa.wsims.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByTeacher_TeacherIdOrderByDueDateDesc(String teacherId);
    List<Assignment> findByClassNameOrderByDueDateAsc(String className);
}
