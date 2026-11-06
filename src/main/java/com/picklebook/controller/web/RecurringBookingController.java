package com.picklebook.controller.web;

import com.picklebook.dto.request.RecurringContractForm;
import com.picklebook.dto.view.RecurringSchedulePreviewResult;
import com.picklebook.dto.view.VietQrPaymentInfo;
import com.picklebook.entity.Court;
import com.picklebook.entity.RecurringContract;
import com.picklebook.entity.User;
import com.picklebook.exception.SlotUnavailableException;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.CourtBookingService;
import com.picklebook.service.RecurringBookingService;
import com.picklebook.service.UserService;
import com.picklebook.service.VietQrService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Controller
@Transactional(readOnly = true)
public class RecurringBookingController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RecurringBookingService recurringBookingService;
    private final CourtBookingService courtBookingService;
    private final UserService userService;
    private final VietQrService vietQrService;

    public RecurringBookingController(RecurringBookingService recurringBookingService,
                                      CourtBookingService courtBookingService,
                                      UserService userService,
                                      VietQrService vietQrService) {
        this.recurringBookingService = recurringBookingService;
        this.courtBookingService = courtBookingService;
        this.userService = userService;
        this.vietQrService = vietQrService;
    }

    @GetMapping("/dat-co-dinh")
    public String recurringPage(@RequestParam(required = false) Long courtId, Model model) {
        List<Court> courts = courtBookingService.getBookableCourts();
        model.addAttribute("courts", courts);

        Long selectedCourtId = (courtId != null) ? courtId : (!courts.isEmpty() ? courts.get(0).getId() : null);
        model.addAttribute("selectedCourtId", selectedCourtId);

        LocalDate now = LocalDate.now(BUSINESS_ZONE);
        // Mặc định chọn tháng tiếp theo hoặc tháng hiện tại
        int defaultYear = now.getYear();
        int defaultMonth = now.getMonthValue();
        model.addAttribute("defaultYear", defaultYear);
        model.addAttribute("defaultMonth", defaultMonth);

        RecurringContractForm form = new RecurringContractForm();
        form.setCourtId(selectedCourtId);
        form.setYear(defaultYear);
        form.setMonth(defaultMonth);
        form.setDaysOfWeek(List.of("MONDAY", "WEDNESDAY", "FRIDAY"));
        form.setStartTime("18:00");
        form.setEndTime("20:00");

        SecurityUtils.currentUserId().map(userService::getById).ifPresent(u -> {
            form.setCustomerName(u.getFullName());
            form.setCustomerPhone(u.getPhone());
            form.setCustomerEmail(u.getEmail());
        });

        model.addAttribute("recurringForm", form);
        return "booking/recurring";
    }

    @GetMapping("/api/recurring/preview")
    @ResponseBody
    public RecurringSchedulePreviewResult previewApi(
            @RequestParam Long courtId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam List<String> daysOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return recurringBookingService.previewSchedule(
                courtId, year, month, daysOfWeek,
                LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    @PostMapping("/dat-co-dinh")
    @Transactional
    public String createRecurringContract(
            @Valid @ModelAttribute("recurringForm") RecurringContractForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courts", courtBookingService.getBookableCourts());
            model.addAttribute("selectedCourtId", form.getCourtId());
            return "booking/recurring";
        }

        User user = SecurityUtils.currentUserId().map(userService::getById).orElse(null);
        try {
            RecurringContract contract = recurringBookingService.createContract(form, user);
            return "redirect:/dat-co-dinh/thanh-cong/" + contract.getContractCode();
        } catch (SlotUnavailableException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("courts", courtBookingService.getBookableCourts());
            model.addAttribute("selectedCourtId", form.getCourtId());
            return "booking/recurring";
        }
    }

    @GetMapping("/dat-co-dinh/thanh-cong/{code}")
    public String recurringSuccess(@PathVariable String code, Model model) {
        RecurringContract contract = recurringBookingService.getContractByCode(code);
        VietQrPaymentInfo vietQr = vietQrService.generatePaymentInfo(contract.getContractCode(), contract.getFinalAmount());

        model.addAttribute("contract", contract);
        model.addAttribute("vietQr", vietQr);
        return "booking/recurring-success";
    }

    @GetMapping("/api/bookings/{code}/payment-status")
    @ResponseBody
    public Map<String, Object> bookingPaymentStatus(@PathVariable String code) {
        try {
            var booking = courtBookingService.getBookingByCode(code);
            boolean isPaid = booking.getPaymentStatus() == com.picklebook.entity.enums.PaymentStatus.PAID;
            return Map.of(
                    "status", booking.getPaymentStatus().name(),
                    "bookingStatus", booking.getStatus().name(),
                    "isPaid", isPaid
            );
        } catch (Exception ex) {
            return Map.of("status", "NOT_FOUND", "isPaid", false);
        }
    }

    @GetMapping("/api/recurring/{code}/payment-status")
    @ResponseBody
    public Map<String, Object> recurringPaymentStatus(@PathVariable String code) {
        try {
            var contract = recurringBookingService.getContractByCode(code);
            boolean isPaid = contract.getPaymentStatus() == com.picklebook.entity.enums.PaymentStatus.PAID;
            return Map.of(
                    "status", contract.getPaymentStatus().name(),
                    "contractStatus", contract.getStatus().name(),
                    "isPaid", isPaid
            );
        } catch (Exception ex) {
            return Map.of("status", "NOT_FOUND", "isPaid", false);
        }
    }
}

