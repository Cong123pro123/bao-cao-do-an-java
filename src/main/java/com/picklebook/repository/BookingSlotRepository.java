package com.picklebook.repository;

import com.picklebook.entity.BookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingSlotRepository extends JpaRepository<BookingSlot, Long> {

    List<BookingSlot> findByBookingIdOrderByStartAtAsc(Long bookingId);

    /**
     * Đếm số slot đang giữ chỗ giao với khoảng [start, end) của một sân.
     * excludeBookingId dùng khi đổi lịch (bỏ qua chính đơn đang đổi).
     */
    @Query("""
            select count(s) from BookingSlot s
            where s.court.id = :courtId
              and s.activeKey = 1
              and s.booking.id <> :excludeBookingId
              and s.startAt < :end
              and s.endAt > :start
            """)
    long countActiveOverlap(@Param("courtId") Long courtId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end,
                            @Param("excludeBookingId") Long excludeBookingId);

    @Query("""
            select s from BookingSlot s
            where s.court.id = :courtId
              and s.activeKey = 1
              and s.startAt >= :from and s.startAt < :to
            order by s.startAt asc
            """)
    List<BookingSlot> findActiveInRange(@Param("courtId") Long courtId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Modifying
    @Query("update BookingSlot s set s.activeKey = null where s.booking.id = :bookingId")
    void releaseByBookingId(@Param("bookingId") Long bookingId);
}
