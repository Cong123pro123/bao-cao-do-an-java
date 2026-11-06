package com.picklebook.repository;

import com.picklebook.entity.RecurringContract;
import com.picklebook.entity.enums.RecurringContractStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringContractRepository extends JpaRepository<RecurringContract, Long> {

    Optional<RecurringContract> findByContractCode(String contractCode);

    @EntityGraph(attributePaths = {"court", "sessions", "user"})
    @Query("select r from RecurringContract r where r.contractCode = :code")
    Optional<RecurringContract> findWithDetailsByContractCode(@Param("code") String code);

    @EntityGraph(attributePaths = {"court", "sessions", "user"})
    @Query("select r from RecurringContract r where r.id = :id")
    Optional<RecurringContract> findWithDetailsById(@Param("id") Long id);

    List<RecurringContract> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"court", "user"})
    List<RecurringContract> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"court", "user"})
    List<RecurringContract> findByStatusOrderByCreatedAtDesc(RecurringContractStatus status);

    @Query("select count(r) from RecurringContract r where r.court.id = :courtId and r.status in ('APPROVED', 'ACTIVE') " +
           "and r.startDate <= :endDate and r.endDate >= :startDate")
    long countOverlappingContracts(@Param("courtId") Long courtId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}

