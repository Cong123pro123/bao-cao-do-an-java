package com.picklebook.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picklebook.entity.Court;
import com.picklebook.entity.CourtSchedule;
import com.picklebook.entity.Branch;
import com.picklebook.entity.enums.CourtStatus;
import com.picklebook.repository.BlockedTimeRepository;
import com.picklebook.repository.BookingRepository;
import com.picklebook.repository.BookingSlotRepository;
import com.picklebook.repository.CouponRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.repository.CourtScheduleRepository;
import com.picklebook.repository.CustomQuestionRepository;
import com.picklebook.repository.ExtraServiceRepository;
import com.picklebook.repository.PaymentRepository;
import com.picklebook.repository.PricingRuleRepository;
import com.picklebook.repository.UserRepository;
import com.picklebook.security.TokenService;
import com.picklebook.service.EmailService;
import com.picklebook.service.SettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtBookingServiceImplTest {

    @Mock private CourtRepository courtRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingSlotRepository bookingSlotRepository;
    @Mock private CourtScheduleRepository scheduleRepository;
    @Mock private BlockedTimeRepository blockedTimeRepository;
    @Mock private PricingRuleRepository pricingRuleRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private ExtraServiceRepository extraServiceRepository;
    @Mock private CustomQuestionRepository questionRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private SettingService settingService;
    @Mock private TokenService tokenService;

    private CourtBookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourtBookingServiceImpl(courtRepository, bookingRepository, bookingSlotRepository,
                scheduleRepository, blockedTimeRepository, pricingRuleRepository, couponRepository,
                extraServiceRepository, questionRepository, paymentRepository, userRepository,
                emailService, settingService, tokenService, new ObjectMapper(), "http://localhost:8081");
    }

    @Test
    void availabilityStartsAtConfiguredOpeningTimeAndPricesByDuration() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        Court court = new Court();
        court.setId(7L);
        court.setStatus(CourtStatus.ACTIVE);
        Branch branch = new Branch();
        branch.setId(1L);
        court.setBranch(branch);
        court.setSlotMinutes(60);
        court.setBufferMinutes(10);
        court.setMinNoticeMinutes(0);
        court.setMaxAdvanceDays(14);
        court.setBasePrice(new BigDecimal("150000"));

        CourtSchedule schedule = new CourtSchedule();
        schedule.setDayOfWeek(date.getDayOfWeek().getValue());
        schedule.setOpenTime(LocalTime.of(6, 0));
        schedule.setCloseTime(LocalTime.of(8, 0));

        when(courtRepository.findById(7L)).thenReturn(Optional.of(court));
        when(scheduleRepository.findByCourtIdAndDayOfWeek(7L, schedule.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(pricingRuleRepository.findApplicable(7L, 1L)).thenReturn(List.of());
        when(blockedTimeRepository.findOverlapping(eq(7L), any(), any())).thenReturn(List.of());
        when(bookingSlotRepository.countActiveOverlap(eq(7L), any(), any(), eq(-1L))).thenReturn(0L);

        var slots = service.getAvailability(7L, date);

        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(6, 0), slots.get(0).startAt().toLocalTime());
        assertEquals(new BigDecimal("150000"), slots.get(0).price());
        assertEquals(LocalTime.of(7, 0), slots.get(1).startAt().toLocalTime());
        assertEquals(LocalTime.of(8, 0), slots.get(1).endAt().toLocalTime());
    }
}
