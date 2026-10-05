package com.sisa.wsims.repository;

import com.sisa.wsims.entity.LibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryItemRepository extends JpaRepository<LibraryItem, Long> {
    List<LibraryItem> findAllByOrderByTitleAsc();
    List<LibraryItem> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String title, String author, String category);
}
