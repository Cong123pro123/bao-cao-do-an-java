package com.picklebook.repository;

import com.picklebook.entity.CustomQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomQuestionRepository extends JpaRepository<CustomQuestion, Long> {

    List<CustomQuestion> findByActiveTrueOrderBySortOrderAsc();

    List<CustomQuestion> findAllByOrderBySortOrderAsc();
}
