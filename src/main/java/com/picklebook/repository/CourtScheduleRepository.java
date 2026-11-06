package com.picklebook.repository;

import com.picklebook.entity.CourtSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourtScheduleRepository extends JpaRepository<CourtSchedule, Long> {

    List<CourtSchedule> findByCourtIdOrderByDayOfWeekAsc(Long courtId);

    Optional<CourtSchedule> findByCourtIdAndDayOfWeek(Long courtId, int dayOfWeek);
}
