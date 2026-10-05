package com.sisa.wsims.repository;

import com.sisa.wsims.entity.ClassDiscussionPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassDiscussionPostRepository extends JpaRepository<ClassDiscussionPost, Long> {
    List<ClassDiscussionPost> findByClassNameOrderByPostedAtAsc(String className);
}
