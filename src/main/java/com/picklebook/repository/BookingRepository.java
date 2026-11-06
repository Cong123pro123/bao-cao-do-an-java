package com.picklebook.repository;

import com.picklebook.entity.Booking;
import com.picklebook.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingCode(String bookingCode);

    Optional<Booking> findByManageTokenHash(String manageTokenHash);

    @EntityGraph(attributePaths = {"court", "court.branch", "user"})
    Optional<Booking> findWithCourtAndUserByManageTokenHash(String manageTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"court", "court.branch", "user"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"court", "court.branch"})
    Page<Booking> findByUserIdOrderByStartAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"court", "court.branch", "user"})
    @Query("""
            select b from Booking b
            where (:status is null or b.status = :status)
              and (:courtId is null or b.court.id = :courtId)
              and (:from is null or b.startAt >= :from)
              and (:to is null or b.startAt < :to)
              and (:keyword is null or lower(b.bookingCode) like lower(concat('%', :keyword, '%'))
                   or lower(b.customerName) like lower(concat('%', :keyword, '%'))
                   or lower(b.customerEmail) like lower(concat('%', :keyword, '%')))
            """)
    Page<Booking> searchAdmin(@Param("status") BookingStatus status,
                              @Param("courtId") Long courtId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"court", "court.branch", "user"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findWithCourtAndUserById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"court", "court.branch", "user"})
    @Query("select b from Booking b where b.bookingCode = :code")
    Optional<Booking> findWithCourtAndUserByCode(@Param("code") String code);

    @EntityGraph(attributePaths = {"court", "court.branch", "user"})
    List<Booking> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"court", "court.branch"})
    @Query("""
            select b from Booking b
            where b.startAt >= :from and b.startAt < :to
              and b.status in :statuses
            order by b.startAt asc
            """)
    List<Booking> findInRange(@Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("statuses") Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {"court", "court.branch"})
    @Query("""
            select b from Booking b
            where b.court.id = :courtId
              and b.startAt >= :from and b.startAt < :to
              and b.status in :statuses
            order by b.startAt asc
            """)
    List<Booking> findInRangeByCourt(@Param("courtId") Long courtId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to,
                                     @Param("statuses") Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {"court", "court.branch"})
    @Query("""
            select b from Booking b
            where b.user.id = :userId
              and b.status in :statuses
              and b.endAt >= :now
            order by b.startAt asc
            """)
    Page<Booking> findUpcomingByUser(@Param("userId") Long userId,
                                     @Param("statuses") Collection<BookingStatus> statuses,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);

    @EntityGraph(attributePaths = {"court", "court.branch"})
    @Query("""
            select b from Booking b
            where b.user.id = :userId
              and b.status in :statuses
            order by b.startAt desc
            """)
    Page<Booking> findByUserAndStatuses(@Param("userId") Long userId,
                                        @Param("statuses") Collection<BookingStatus> statuses,
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"court", "user"})
    @Query("""
            select b from Booking b
            where b.status = :status
              and b.reminderSentAt is null
              and b.startAt >= :from and b.startAt <= :to
            order by b.startAt asc
            """)
    List<Booking> findForReminder(@Param("status") BookingStatus status,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    @EntityGraph(attributePaths = "court")
    @Query("select b from Booking b where b.status = :status and b.createdAt < :cutoff")
    List<Booking> findPendingBefore(@Param("status") BookingStatus status,
                                    @Param("cutoff") LocalDateTime cutoff);

    @EntityGraph(attributePaths = "court")
    @Query("select b from Booking b where b.status = :status and b.endAt < :cutoff")
    List<Booking> findToComplete(@Param("status") BookingStatus status,
                                 @Param("cutoff") LocalDateTime cutoff);

    long countByStartAtGreaterThanEqualAndStartAtLessThan(LocalDateTime from, LocalDateTime to);

    long countByStatusAndStartAtBetween(BookingStatus status, LocalDateTime from, LocalDateTime to);

    long countByStatus(BookingStatus status);

    @Query("""
            select coalesce(sum(b.finalAmount), 0) from Booking b
            where b.status in :statuses
              and b.startAt >= :from and b.startAt < :to
            """)
    BigDecimal sumRevenue(@Param("statuses") Collection<BookingStatus> statuses,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(b.finalAmount), 0) from Booking b
            where b.status in :statuses
              and b.startAt >= :from and b.startAt < :to
              and b.paymentStatus in (com.picklebook.entity.enums.PaymentStatus.PAID,
                                      com.picklebook.entity.enums.PaymentStatus.DEPOSIT_PAID)
            """)
    BigDecimal sumPaidRevenue(@Param("statuses") Collection<BookingStatus> statuses,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
