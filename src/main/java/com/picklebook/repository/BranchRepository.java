package com.picklebook.repository;

import com.picklebook.entity.Branch;
import com.picklebook.entity.enums.CourtStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findBySlugAndStatus(String slug, CourtStatus status);

    List<Branch> findByStatusOrderByNameAsc(CourtStatus status);

    List<Branch> findAllByOrderByNameAsc();
}
