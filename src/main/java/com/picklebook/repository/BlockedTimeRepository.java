package com.picklebook.repository;

import com.picklebook.entity.BlockedTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BlockedTimeRepository extends JpaRepository<BlockedTime, Long> {

    /**
     * Lấy các khoảng chặn giao với [start, end) của một sân, bao gồm cả
     * khoảng chặn toàn hệ thống (court = null).
     */
    @Query("""
            select b from BlockedTime b
            where (b.court is null or b.court.id = :courtId)
              and b.startAt < :end
              and b.endAt > :start
            order by b.startAt asc
            """)
    List<BlockedTime> findOverlapping(@Param("courtId") Long courtId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("""
            select b from BlockedTime b
            where b.affectsPricing = true
              and (b.court is null or b.court.id = :courtId)
              and b.startAt < :end
              and b.endAt > :start
            """)
    List<BlockedTime> findHolidaysOverlapping(@Param("courtId") Long courtId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = "court")
    List<BlockedTime> findAllByOrderByStartAtDesc();

    @EntityGraph(attributePaths = "court")
    List<BlockedTime> findByCourtIdOrderByStartAtDesc(Long courtId);
}
