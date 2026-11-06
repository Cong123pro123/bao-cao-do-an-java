package com.picklebook.controller.admin;

import com.picklebook.dto.view.AvailabilitySlot;
import com.picklebook.dto.view.CourtTimelineView;
import com.picklebook.entity.BookingSlot;
import com.picklebook.entity.Court;
import com.picklebook.entity.enums.CourtStatus;
import com.picklebook.repository.BookingSlotRepository;
import com.picklebook.repository.BranchRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.service.CourtBookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/timeline")
public class AdminTimelineController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CourtRepository courtRepository;
    private final BranchRepository branchRepository;
    private final CourtBookingService courtBookingService;
    private final BookingSlotRepository bookingSlotRepository;

    public AdminTimelineController(CourtRepository courtRepository,
                                   BranchRepository branchRepository,
                                   CourtBookingService courtBookingService,
                                   BookingSlotRepository bookingSlotRepository) {
        this.courtRepository = courtRepository;
        this.branchRepository = branchRepository;
        this.courtBookingService = courtBookingService;
        this.bookingSlotRepository = bookingSlotRepository;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String timeline(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) Long branchId,
                           Model model) {
        LocalDate selectedDate = (date != null) ? date : LocalDate.now(BUSINESS_ZONE);
        LocalDateTime fromTime = selectedDate.atStartOfDay();
        LocalDateTime toTime = selectedDate.plusDays(1).atStartOfDay();

        List<Court> courts = (branchId != null)
                ? courtRepository.findWithBranchByBranchIdAndStatus(branchId, CourtStatus.ACTIVE)
                : courtRepository.findWithBranchByStatus(CourtStatus.ACTIVE);

        List<CourtTimelineView> timelineViews = new ArrayList<>();
        int totalDaySlots = 0;
        int bookedDaySlots = 0;

        for (Court court : courts) {
            List<AvailabilitySlot> availability = courtBookingService.getAvailability(court.getId(), selectedDate);
            List<BookingSlot> activeSlots = bookingSlotRepository.findActiveInRange(court.getId(), fromTime, toTime);

            Map<LocalDateTime, BookingSlot> slotByStart = new HashMap<>();
            for (BookingSlot bs : activeSlots) {
                slotByStart.put(bs.getStartAt(), bs);
            }

            List<CourtTimelineView.Item> items = new ArrayList<>();
            int courtTotal = availability.size();
            int courtBooked = 0;

            for (AvailabilitySlot slot : availability) {
                BookingSlot bookedRef = slotByStart.get(slot.startAt());
                String custName = null;
                String custPhone = null;
                String code = null;
                String bookingStatus = null;

                if (bookedRef != null && bookedRef.getBooking() != null) {
                    courtBooked++;
                    custName = bookedRef.getBooking().getCustomerName();
                    custPhone = bookedRef.getBooking().getCustomerPhone();
                    code = bookedRef.getBooking().getBookingCode();
                    bookingStatus = bookedRef.getBooking().getStatus().getLabel();
                } else if (!slot.available() && "BOOKED".equalsIgnoreCase(slot.status())) {
                    courtBooked++;
                }

                items.add(new CourtTimelineView.Item(
                        slot.startAt(),
                        slot.endAt(),
                        slot.price(),
                        slot.status(),
                        slot.available(),
                        custName,
                        custPhone,
                        code,
                        bookingStatus
                ));
            }

            int courtAvailable = courtTotal - courtBooked;
            timelineViews.add(new CourtTimelineView(court, items, courtTotal, courtBooked, Math.max(0, courtAvailable)));
            totalDaySlots += courtTotal;
            bookedDaySlots += courtBooked;
        }

        int occupancyRate = (totalDaySlots > 0) ? (bookedDaySlots * 100 / totalDaySlots) : 0;

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("branches", branchRepository.findByStatusOrderByNameAsc(CourtStatus.ACTIVE));
        model.addAttribute("timelines", timelineViews);
        model.addAttribute("totalDaySlots", totalDaySlots);
        model.addAttribute("bookedDaySlots", bookedDaySlots);
        model.addAttribute("availableDaySlots", Math.max(0, totalDaySlots - bookedDaySlots));
        model.addAttribute("occupancyRate", occupancyRate);

        return "admin/timeline";
    }
}
