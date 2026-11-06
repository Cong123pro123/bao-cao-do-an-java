package com.picklebook.controller.web;

import com.picklebook.dto.request.BookingForm;
import com.picklebook.dto.view.AvailabilitySlot;
import com.picklebook.entity.Booking;
import com.picklebook.entity.Court;
import com.picklebook.entity.User;
import com.picklebook.exception.SlotUnavailableException;
import com.picklebook.entity.enums.CourtStatus;
import com.picklebook.repository.BranchRepository;
import com.picklebook.repository.CourtScheduleRepository;
import com.picklebook.repository.ExtraServiceRepository;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.CourtBookingService;
import com.picklebook.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@Transactional(readOnly = true)
public class CourtController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CourtBookingService courtBookingService;
    private final UserService userService;
    private final ExtraServiceRepository extraServiceRepository;
    private final BranchRepository branchRepository;
    private final CourtScheduleRepository courtScheduleRepository;
    private final com.picklebook.service.VietQrService vietQrService;

    public CourtController(CourtBookingService courtBookingService,
                           UserService userService,
                           ExtraServiceRepository extraServiceRepository,
                           BranchRepository branchRepository,
                           CourtScheduleRepository courtScheduleRepository,
                           com.picklebook.service.VietQrService vietQrService) {
        this.courtBookingService = courtBookingService;
        this.userService = userService;
        this.extraServiceRepository = extraServiceRepository;
        this.branchRepository = branchRepository;
        this.courtScheduleRepository = courtScheduleRepository;
        this.vietQrService = vietQrService;
    }

    @GetMapping("/san")
    public String courts(@RequestParam(required = false) Long branchId,
                         @RequestParam(required = false) com.picklebook.entity.enums.CourtType courtType,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) java.math.BigDecimal maxPrice,
                         Model model) {
        List<Court> courtList = (branchId == null)
                ? courtBookingService.getBookableCourts()
                : courtBookingService.getBookableCourts(branchId);

        if (courtType != null) {
            courtList = courtList.stream()
                    .filter(c -> c.getCourtType() == courtType)
                    .toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String lowerKw = keyword.trim().toLowerCase();
            courtList = courtList.stream()
                    .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(lowerKw))
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(lowerKw))
                            || (c.getAmenities() != null && c.getAmenities().toLowerCase().contains(lowerKw))
                            || (c.getBranch() != null && c.getBranch().getName() != null && c.getBranch().getName().toLowerCase().contains(lowerKw)))
                    .toList();
        }
        if (maxPrice != null && maxPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
            courtList = courtList.stream()
                    .filter(c -> c.getBasePrice() != null && c.getBasePrice().compareTo(maxPrice) <= 0)
                    .toList();
        }

        model.addAttribute("courts", courtList);
        model.addAttribute("branches", branchRepository.findByStatusOrderByNameAsc(CourtStatus.ACTIVE));
        model.addAttribute("courtTypes", com.picklebook.entity.enums.CourtType.values());
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedCourtType", courtType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("maxPrice", maxPrice);
        return "courts/list";
    }

    @GetMapping("/san/{slug}")
    public String court(@PathVariable String slug,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                        LocalDate date,
                        Model model) {
        Court court = courtBookingService.getBookableCourt(slug);
        LocalDate selectedDate = date == null ? LocalDate.now(BUSINESS_ZONE) : date;
        model.addAttribute("court", court);
        model.addAttribute("schedules", courtScheduleRepository.findByCourtIdOrderByDayOfWeekAsc(court.getId()));
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("availability", courtBookingService.getAvailability(court.getId(), selectedDate));
        return "courts/detail";
    }

    @GetMapping("/api/courts/{courtId}/slots")
    @ResponseBody
    public List<AvailabilitySlot> availability(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return courtBookingService.getAvailability(courtId, date);
    }

    @GetMapping("/dat-san")
    public String bookingForm(@RequestParam Long courtId,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam(required = false) List<String> slotStarts,
                              Model model) {
        BookingForm form = new BookingForm();
        form.setCourtId(courtId);
        form.setDate(date == null ? LocalDate.now(BUSINESS_ZONE) : date);
        if (slotStarts != null) {
            form.setSlotStarts(slotStarts);
        }
        SecurityUtils.currentUserId().map(userService::getById).ifPresent(user -> populateCustomer(form, user));
        model.addAttribute("bookingForm", form);
        addBookingOptions(model, form);
        return "booking/confirm";
    }

    @PostMapping("/dat-san")
    @Transactional
    public String createBooking(@Valid @org.springframework.web.bind.annotation.ModelAttribute("bookingForm")
                                BookingForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addBookingOptions(model, form);
            return "booking/confirm";
        }
        User user = SecurityUtils.currentUserId().map(userService::getById)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng đã đăng nhập."));
        try {
            Booking booking = courtBookingService.createBooking(form, user);
            return "redirect:/dat-san/thanh-cong/" + booking.getBookingCode();
        } catch (SlotUnavailableException ex) {
            model.addAttribute("bookingError", ex.getMessage());
            addBookingOptions(model, form);
            return "booking/confirm";
        }
    }

    @GetMapping("/dat-san/thanh-cong/{code}")
    @Transactional(readOnly = true)
    public String bookingSuccess(@PathVariable String code, Model model) {
        Booking booking = courtBookingService.getBookingByCode(code);
        model.addAttribute("booking", booking);
        model.addAttribute("vietQr", vietQrService.generatePaymentInfo(booking.getBookingCode(), booking.getFinalAmount()));
        return "booking/success";
    }

    private void addBookingOptions(Model model, BookingForm form) {
        Court court = courtBookingService.getBookableCourt(form.getCourtId());
        LocalDate date = form.getDate() == null ? LocalDate.now(BUSINESS_ZONE) : form.getDate();
        List<AvailabilitySlot> allSlots = courtBookingService.getAvailability(court.getId(), date);
        model.addAttribute("court", court);
        model.addAttribute("selectedDate", date);
        model.addAttribute("availability", allSlots);
        model.addAttribute("extraServices", extraServiceRepository.findByActiveTrueOrderBySortOrderAsc());
        model.addAttribute("questions", courtBookingService.getActiveQuestions());

        List<AvailabilitySlot> chosenSlots = new ArrayList<>();
        BigDecimal slotsTotal = BigDecimal.ZERO;
        if (form.getSlotStarts() != null && !form.getSlotStarts().isEmpty()) {
            Set<String> chosenStarts = new HashSet<>(form.getSlotStarts());
            for (AvailabilitySlot s : allSlots) {
                String startTimeStr = s.startAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                if (chosenStarts.contains(startTimeStr)) {
                    chosenSlots.add(s);
                    slotsTotal = slotsTotal.add(s.price());
                }
            }
        }
        model.addAttribute("chosenSlots", chosenSlots);
        model.addAttribute("slotsTotal", slotsTotal);
        int depositPercent = vietQrService.getDepositPercent();
        BigDecimal depositMin = slotsTotal.multiply(BigDecimal.valueOf(depositPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        model.addAttribute("depositPercent", depositPercent);
        model.addAttribute("depositMin", depositMin);
    }

    private void populateCustomer(BookingForm form, User user) {
        form.setCustomerName(user.getFullName());
        form.setCustomerPhone(user.getPhone());
        form.setCustomerEmail(user.getEmail());
    }
}
