package com.picklebook.repository;

import com.picklebook.entity.CourtImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourtImageRepository extends JpaRepository<CourtImage, Long> {

    List<CourtImage> findByCourtIdOrderBySortOrderAsc(Long courtId);

    Optional<CourtImage> findFirstByCourtIdAndPrimaryTrueOrderBySortOrderAsc(Long courtId);
}
