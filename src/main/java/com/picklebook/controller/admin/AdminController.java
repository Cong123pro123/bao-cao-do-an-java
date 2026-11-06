package com.picklebook.controller.admin;

import com.picklebook.entity.Court;
import com.picklebook.entity.enums.BookingStatus;
import com.picklebook.exception.BusinessException;
import com.picklebook.repository.CourtRepository;
import com.picklebook.service.AdminOperationsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;

@Controller
public class AdminController {

    private final AdminOperationsService adminOperationsService;
    private final CourtRepository courtRepository;

    public AdminController(AdminOperationsService adminOperationsService, CourtRepository courtRepository) {
        this.adminOperationsService = adminOperationsService;
        this.courtRepository = courtRepository;
    }

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", adminOperationsService.dashboard());
        return "admin/dashboard";
    }

    @GetMapping("/admin/bookings")
    public String bookings(@RequestParam(required = false) BookingStatus status,
                           @RequestParam(required = false) Long courtId,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        model.addAttribute("bookings", adminOperationsService.searchBookings(status, courtId, date, keyword, page));
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("courts", courtRepository.findAllByOrderBySortOrderAsc());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCourtId", courtId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("keyword", keyword);
        return "admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/status")
    public String changeBookingStatus(@PathVariable Long id,
                                      @RequestParam BookingStatus status,
                                      RedirectAttributes redirectAttributes) {
        try {
            adminOperationsService.updateBookingStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái đơn.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/check-in")
    public String checkIn(@PathVariable Long id,
                          @RequestParam(required = false) String returnUrl,
                          RedirectAttributes redirectAttributes) {
        try {
            adminOperationsService.checkInBooking(id);
            redirectAttributes.addFlashAttribute("success", "Đã check-in thành công cho khách hàng!");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        // Phòng chống lỗ hổng Open Redirect: chỉ cho phép chuyển hướng về URL nội bộ hợp lệ
        String safeTarget = (returnUrl != null
                && returnUrl.startsWith("/")
                && !returnUrl.startsWith("//")
                && !returnUrl.contains("\\")
                && !returnUrl.contains(":"))
                ? returnUrl : "/admin/bookings";
        return "redirect:" + safeTarget;
    }

    @GetMapping("/admin/bookings/export")
    public void exportBookings(@RequestParam(required = false) BookingStatus status,
                               @RequestParam(required = false) Long courtId,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) String keyword,
                               HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"picklebook-bookings.csv\"");
        var writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("Mã đặt sân,Sân,Khách hàng,Email,Điện thoại,Bắt đầu,Kết thúc,Trạng thái,Tổng tiền");
        var page = adminOperationsService.searchBookings(status, courtId, date, keyword, 0);
        while (true) {
            page.getContent().forEach(booking -> writer.println(csv(booking.getBookingCode()) + ","
                    + csv(booking.getCourt().getName()) + "," + csv(booking.getCustomerName()) + ","
                    + csv(booking.getCustomerEmail()) + "," + csv(booking.getCustomerPhone()) + ","
                    + csv(booking.getStartAt().toString()) + "," + csv(booking.getEndAt().toString()) + ","
                    + csv(booking.getStatus().getLabel()) + "," + booking.getFinalAmount().toPlainString()));
            if (!page.hasNext()) {
                break;
            }
            page = adminOperationsService.searchBookings(status, courtId, date, keyword, page.getNumber() + 1);
        }
        writer.flush();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
