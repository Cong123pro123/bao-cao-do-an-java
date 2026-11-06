package com.picklebook.service.impl;

import com.picklebook.entity.Booking;
import com.picklebook.entity.enums.BookingStatus;
import com.picklebook.exception.BusinessException;
import com.picklebook.repository.BookingRepository;
import com.picklebook.repository.BookingSlotRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOperationsServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingSlotRepository bookingSlotRepository;
    @Mock private CourtRepository courtRepository;
    @Mock private AuditLogService auditLogService;

    private AdminOperationsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminOperationsServiceImpl(bookingRepository, bookingSlotRepository,
                courtRepository, auditLogService);
    }

    @Test
    void pendingBookingCannotBeMarkedCompleted() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class,
                () -> service.updateBookingStatus(1L, BookingStatus.COMPLETED));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verifyNoInteractions(bookingSlotRepository, auditLogService);
    }

    @Test
    void cancellingActiveBookingReleasesItsSlots() {
        Booking booking = booking(BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        service.updateBookingStatus(1L, BookingStatus.CANCELLED);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingSlotRepository).releaseByBookingId(1L);
        verify(auditLogService).log(any(), eq("Booking"), eq(1L), contains("CONFIRMED"));
    }

    private Booking booking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingCode("PB-TEST-1");
        booking.setStatus(status);
        return booking;
    }
}
