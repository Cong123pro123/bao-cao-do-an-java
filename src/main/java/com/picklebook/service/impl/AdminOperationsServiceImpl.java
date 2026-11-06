package com.picklebook.service.impl;

import com.picklebook.dto.view.AdminDashboardSummary;
import com.picklebook.entity.Booking;
import com.picklebook.entity.enums.AuditAction;
import com.picklebook.entity.enums.BookingStatus;
import com.picklebook.entity.enums.PaymentStatus;
import com.picklebook.exception.BusinessException;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.repository.BookingRepository;
import com.picklebook.repository.BookingSlotRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.AdminOperationsService;
import com.picklebook.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
public class AdminOperationsServiceImpl implements AdminOperationsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<BookingStatus> REVENUE_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final CourtRepository courtRepository;
    private final AuditLogService auditLogService;

    public AdminOperationsServiceImpl(BookingRepository bookingRepository,
                                      BookingSlotRepository bookingSlotRepository,
                                      CourtRepository courtRepository,
                                      AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.bookingSlotRepository = bookingSlotRepository;
        this.courtRepository = courtRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummary dashboard() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDate monthStartDate = today.withDayOfMonth(1);

        long todayTotal = bookingRepository.countByStartAtGreaterThanEqualAndStartAtLessThan(dayStart, dayEnd);
        long weekTotal = bookingRepository.countByStartAtGreaterThanEqualAndStartAtLessThan(weekStartDate.atStartOfDay(), dayEnd);
        long monthTotal = bookingRepository.countByStartAtGreaterThanEqualAndStartAtLessThan(monthStartDate.atStartOfDay(), dayEnd);
        BigDecimal monthRev = bookingRepository.sumPaidRevenue(REVENUE_STATUSES, monthStartDate.atStartOfDay(), dayEnd);
        BigDecimal todayRev = bookingRepository.sumPaidRevenue(REVENUE_STATUSES, dayStart, dayEnd);
        long activeCourts = courtRepository.countByStatus(com.picklebook.entity.enums.CourtStatus.ACTIVE);
        List<Booking> todaySchedule = bookingRepository.findInRange(dayStart, dayEnd,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

        long todayCompleted = todaySchedule.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long todayConfirmed = todaySchedule.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long todayPending = todaySchedule.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();

        return new AdminDashboardSummary(
                todayTotal,
                weekTotal,
                monthTotal,
                monthRev,
                activeCourts,
                todaySchedule,
                todayCompleted,
                todayConfirmed,
                todayPending,
                todayRev);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> searchBookings(BookingStatus status, Long courtId, LocalDate date,
                                        String keyword, int page) {
        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime to = date == null ? null : date.plusDays(1).atStartOfDay();
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return bookingRepository.searchAdmin(status, courtId, from, to, normalizedKeyword,
                PageRequest.of(Math.max(0, page), 25));
    }

    @Override
    @Transactional
    public void updateBookingStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("đơn đặt sân", bookingId));
        if (status == null || status == BookingStatus.PENDING) {
            throw new BusinessException("Trạng thái mới không hợp lệ.");
        }
        BookingStatus oldStatus = booking.getStatus();
        if (oldStatus == status) {
            throw new BusinessException("Đơn đặt sân đã ở trạng thái này.");
        }
        boolean validTransition = switch (oldStatus) {
            case PENDING -> status == BookingStatus.CONFIRMED || status == BookingStatus.CANCELLED;
            case CONFIRMED -> status == BookingStatus.CANCELLED
                    || status == BookingStatus.COMPLETED || status == BookingStatus.NO_SHOW;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
        if (!validTransition) {
            throw new BusinessException("Không thể chuyển trạng thái đơn "
                    + oldStatus.getLabel() + " sang " + status.getLabel() + ".");
        }
        booking.setStatus(status);
        if (status == BookingStatus.CANCELLED) {
            booking.setCancelledAt(LocalDateTime.now(BUSINESS_ZONE));
            booking.setCancelReason("Đơn được hủy bởi nhân viên/quản trị viên.");
            bookingSlotRepository.releaseByBookingId(booking.getId());
        }
        auditLogService.log(status == BookingStatus.CANCELLED ? AuditAction.CANCEL : AuditAction.STATUS_CHANGE,
                "Booking", booking.getId(), oldStatus + " → " + status + " (" + booking.getBookingCode() + ")");
    }

    @Override
    @Transactional
    public void checkInBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("đơn đặt sân", bookingId));
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Đơn đặt sân này đã được hoàn thành hoặc check-in trước đó.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Không thể check-in cho đơn đặt sân đã bị hủy.");
        }
        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new BusinessException("Không thể check-in cho đơn đặt sân đã đánh dấu vắng mặt (No-show).");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        auditLogService.log(AuditAction.STATUS_CHANGE, "Booking", booking.getId(),
                "Khách hàng check-in tại sân (" + booking.getBookingCode() + ")");
    }
}
