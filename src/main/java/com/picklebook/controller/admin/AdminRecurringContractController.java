package com.picklebook.controller.admin;

import com.picklebook.entity.RecurringContract;
import com.picklebook.service.RecurringBookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/recurring")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Transactional(readOnly = true)
public class AdminRecurringContractController {

    private final RecurringBookingService recurringBookingService;

    public AdminRecurringContractController(RecurringBookingService recurringBookingService) {
        this.recurringBookingService = recurringBookingService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("contracts", recurringBookingService.getAllContracts());
        return "admin/recurring-contracts";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        RecurringContract contract = recurringBookingService.getContractById(id);
        model.addAttribute("contract", contract);
        return "admin/recurring-contract-detail";
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recurringBookingService.approveContract(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt và kích hoạt hợp đồng đặt cố định!");
        return "redirect:/admin/recurring/" + id;
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public String cancel(@PathVariable Long id,
                         @RequestParam(defaultValue = "Hủy theo yêu cầu khách hàng") String reason,
                         RedirectAttributes redirectAttributes) {
        recurringBookingService.cancelContract(id, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hợp đồng đặt sân cố định.");
        return "redirect:/admin/recurring/" + id;
    }
}

