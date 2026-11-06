package com.picklebook.repository;

import com.picklebook.entity.RecurringContractSession;
import com.picklebook.entity.enums.RecurringSessionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface RecurringContractSessionRepository extends JpaRepository<RecurringContractSession, Long> {

    List<RecurringContractSession> findByContractIdOrderBySessionDateAscStartTimeAsc(Long contractId);

    @EntityGraph(attributePaths = {"contract", "contract.court"})
    List<RecurringContractSession> findBySessionDateOrderByStartTimeAsc(LocalDate sessionDate);

    @Query("select s from RecurringContractSession s where s.contract.court.id = :courtId " +
           "and s.sessionDate = :date and s.status = 'SCHEDULED' " +
           "and s.startTime < :endTime and s.endTime > :startTime")
    List<RecurringContractSession> findConflictingSessions(@Param("courtId") Long courtId,
                                                          @Param("date") LocalDate date,
                                                          @Param("startTime") LocalTime startTime,
                                                          @Param("endTime") LocalTime endTime);
}

