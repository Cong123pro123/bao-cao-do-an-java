package com.picklebook.scheduler;

import com.picklebook.entity.Booking;
import com.picklebook.entity.enums.BookingStatus;
import com.picklebook.entity.enums.EmailTemplateCode;
import com.picklebook.repository.BookingRepository;
import com.picklebook.repository.BookingSlotRepository;
import com.picklebook.service.EmailService;
import com.picklebook.service.SettingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
public class BookingLifecycleScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingRepository bookingRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final EmailService emailService;
    private final SettingService settingService;

    public BookingLifecycleScheduler(BookingRepository bookingRepository,
                                     BookingSlotRepository bookingSlotRepository,
                                     EmailService emailService,
                                     SettingService settingService) {
        this.bookingRepository = bookingRepository;
        this.bookingSlotRepository = bookingSlotRepository;
        this.emailService = emailService;
        this.settingService = settingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.lifecycle-delay-ms:60000}")
    @Transactional
    public void processLifecycle() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        cancelExpiredPending(now);
        completeFinishedBookings(now);
        sendUpcomingReminders(now);
    }

    private void cancelExpiredPending(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(settingService.getInt("booking.pending_timeout_minutes", 15));
        for (Booking booking : bookingRepository.findPendingBefore(BookingStatus.PENDING, cutoff)) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);
            booking.setCancelReason("Đơn hết thời gian chờ xác nhận.");
            bookingSlotRepository.releaseByBookingId(booking.getId());
            Map<String, Object> variables = new HashMap<>();
            variables.put("name", booking.getCustomerName());
            variables.put("customerName", booking.getCustomerName());
            variables.put("court", booking.getCourt().getName());
            variables.put("date", booking.getStartAt().toLocalDate().toString());
            variables.put("time", booking.getStartAt().toLocalTime() + " - " + booking.getEndAt().toLocalTime());
            variables.put("bookingCode", booking.getBookingCode());
            variables.put("total", booking.getFinalAmount().toPlainString() + " ₫");
            variables.put("paymentMethod", booking.getPaymentMethod().getLabel());
            emailService.sendTemplate(EmailTemplateCode.BOOKING_CANCELLED,
                    booking.getCustomerEmail(), variables);
        }
    }

    private void completeFinishedBookings(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(settingService.getInt("booking.auto_complete_minutes", 30));
        for (Booking booking : bookingRepository.findToComplete(BookingStatus.CONFIRMED, cutoff)) {
            booking.setStatus(BookingStatus.COMPLETED);
        }
    }

    private void sendUpcomingReminders(LocalDateTime now) {
        long delayMillis = Math.max(1000L, settingService.getInt("booking.scheduler_window_minutes", 1) * 60_000L);
        long reminderHours = Math.max(0, settingService.getInt("booking.reminder_hours", 3));
        LocalDateTime target = now.plusHours(reminderHours);
        for (Booking booking : bookingRepository.findForReminder(
                BookingStatus.CONFIRMED, target.minusNanos(delayMillis * 1_000_000L), target)) {
            booking.setReminderSentAt(now);
            Map<String, Object> variables = new HashMap<>();
            variables.put("name", booking.getCustomerName());
            variables.put("customerName", booking.getCustomerName());
            variables.put("court", booking.getCourt().getName());
            variables.put("date", booking.getStartAt().toLocalDate().toString());
            variables.put("time", booking.getStartAt().toLocalTime() + " - " + booking.getEndAt().toLocalTime());
            variables.put("bookingCode", booking.getBookingCode());
            variables.put("total", booking.getFinalAmount().toPlainString() + " ₫");
            variables.put("paymentMethod", booking.getPaymentMethod().getLabel());
            emailService.sendTemplate(EmailTemplateCode.BOOKING_REMINDER,
                    booking.getCustomerEmail(), variables);
        }
    }
}
