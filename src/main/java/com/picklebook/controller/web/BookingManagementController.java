package com.picklebook.controller.web;

import com.picklebook.dto.view.AvailabilitySlot;
import com.picklebook.entity.Booking;
import com.picklebook.exception.BusinessException;
import com.picklebook.exception.SlotUnavailableException;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.CourtBookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Controller
public class BookingManagementController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CourtBookingService courtBookingService;

    public BookingManagementController(CourtBookingService courtBookingService) {
        this.courtBookingService = courtBookingService;
    }

    @GetMapping("/lich-cua-toi")
    public String myBookings(Model model) {
        Long userId = requireUserId();
        model.addAttribute("bookings", courtBookingService.getBookingsForUser(userId, 0).getContent());
        return "my/bookings";
    }

    @GetMapping("/lich-cua-toi/{id}")
    public String bookingDetail(@PathVariable Long id, Model model) {
        model.addAttribute("booking", courtBookingService.getBookingForUser(id, requireUserId()));
        return "my/booking-detail";
    }

    @PostMapping("/lich-cua-toi/{id}/huy")
    public String cancelMyBooking(@PathVariable Long id,
                                  @RequestParam(required = false) String reason,
                                  RedirectAttributes redirectAttributes) {
        try {
            courtBookingService.cancelBooking(id, requireUserId(), reason);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn đặt sân.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/lich-cua-toi/" + id;
    }

    @GetMapping("/lich-cua-toi/{id}/doi-lich")
    public String rescheduleMyBooking(@PathVariable Long id,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      Model model) {
        Booking booking = courtBookingService.getBookingForUser(id, requireUserId());
        LocalDate selectedDate = date == null ? LocalDate.now(BUSINESS_ZONE).plusDays(1) : date;
        model.addAttribute("booking", booking);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("availability", courtBookingService.getAvailability(booking.getCourt().getId(), selectedDate));
        return "booking/reschedule";
    }

    @PostMapping("/lich-cua-toi/{id}/doi-lich")
    public String saveReschedule(@PathVariable Long id,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam List<String> slotStarts,
                                 RedirectAttributes redirectAttributes) {
        try {
            courtBookingService.rescheduleBooking(id, requireUserId(), date, slotStarts);
            redirectAttributes.addFlashAttribute("success", "Đã đổi lịch đặt sân.");
            return "redirect:/lich-cua-toi/" + id;
        } catch (SlotUnavailableException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/lich-cua-toi/" + id + "/doi-lich?date=" + date;
        }
    }

    @GetMapping("/quan-ly-dat-san")
    public String manageByToken(@RequestParam String token, Model model) {
        model.addAttribute("booking", courtBookingService.getBookingForManagementToken(token));
        model.addAttribute("token", token);
        return "booking/manage";
    }

    @PostMapping("/quan-ly-dat-san/huy")
    public String cancelByToken(@RequestParam String token,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            courtBookingService.cancelBookingByManagementToken(token, reason);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn đặt sân.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/quan-ly-dat-san?token=" + token;
    }

    @GetMapping("/quan-ly-dat-san/doi-lich")
    public String rescheduleByToken(@RequestParam String token,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    Model model) {
        Booking booking = courtBookingService.getBookingForManagementToken(token);
        LocalDate selectedDate = date == null ? LocalDate.now(BUSINESS_ZONE).plusDays(1) : date;
        model.addAttribute("booking", booking);
        model.addAttribute("token", token);
        model.addAttribute("selectedDate", selectedDate);
        List<AvailabilitySlot> availability = courtBookingService.getAvailability(
                booking.getCourt().getId(), selectedDate);
        model.addAttribute("availability", availability);
        return "booking/reschedule";
    }

    @PostMapping("/quan-ly-dat-san/doi-lich")
    public String saveRescheduleByToken(@RequestParam String token,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        @RequestParam List<String> slotStarts,
                                        RedirectAttributes redirectAttributes) {
        try {
            courtBookingService.rescheduleBookingByManagementToken(token, date, slotStarts);
            redirectAttributes.addFlashAttribute("success", "Đã đổi lịch đặt sân.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/quan-ly-dat-san?token=" + token;
    }

    private Long requireUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new BusinessException("Vui lòng đăng nhập để tiếp tục."));
    }
}
