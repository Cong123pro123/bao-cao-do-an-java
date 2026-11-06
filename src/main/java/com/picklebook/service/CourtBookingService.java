package com.picklebook.service;

import com.picklebook.dto.request.BookingForm;
import com.picklebook.dto.view.AvailabilitySlot;
import com.picklebook.entity.Booking;
import com.picklebook.entity.Court;
import com.picklebook.entity.CustomQuestion;
import com.picklebook.entity.User;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public interface CourtBookingService {

    List<Court> getBookableCourts();

    List<Court> getBookableCourts(long branchId);

    Court getBookableCourt(String slug);

    Court getBookableCourt(long courtId);

    List<CustomQuestion> getActiveQuestions();

    List<AvailabilitySlot> getAvailability(long courtId, LocalDate date);

    Booking createBooking(BookingForm form, User user);

    Booking getBookingByCode(String code);

    Page<Booking> getBookingsForUser(Long userId, int page);

    Booking getBookingForUser(Long bookingId, Long userId);

    Booking getBookingForManagementToken(String rawToken);

    void cancelBooking(Long bookingId, Long userId, String reason);

    void cancelBookingByManagementToken(String rawToken, String reason);

    void rescheduleBooking(Long bookingId, Long userId, LocalDate date, List<String> slotStarts);

    void rescheduleBookingByManagementToken(String rawToken, LocalDate date, List<String> slotStarts);
}
