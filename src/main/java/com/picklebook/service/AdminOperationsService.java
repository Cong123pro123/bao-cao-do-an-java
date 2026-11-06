package com.picklebook.service;

import com.picklebook.dto.view.AdminDashboardSummary;
import com.picklebook.entity.Booking;
import com.picklebook.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AdminOperationsService {

    AdminDashboardSummary dashboard();

    Page<Booking> searchBookings(BookingStatus status, Long courtId, LocalDate date,
                                 String keyword, int page);

    void updateBookingStatus(Long bookingId, BookingStatus status);

    void checkInBooking(Long bookingId);
}
