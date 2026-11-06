package com.picklebook.controller.admin;

import com.picklebook.dto.request.PricingRuleForm;
import com.picklebook.entity.Branch;
import com.picklebook.entity.Court;
import com.picklebook.entity.PricingRule;
import com.picklebook.entity.enums.AuditAction;
import com.picklebook.entity.enums.CourtStatus;
import com.picklebook.entity.enums.DayType;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.repository.BranchRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.repository.PricingRuleRepository;
import com.picklebook.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/pricing")
public class AdminPricingRuleController {

    private final PricingRuleRepository pricingRuleRepository;
    private final CourtRepository courtRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public AdminPricingRuleController(PricingRuleRepository pricingRuleRepository,
                                      CourtRepository courtRepository,
                                      BranchRepository branchRepository,
                                      AuditLogService auditLogService) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.courtRepository = courtRepository;
        this.branchRepository = branchRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(Model model) {
        model.addAttribute("rules", pricingRuleRepository.findAllByOrderByPriorityDesc());
        return "admin/pricing-rules";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("ruleForm", new PricingRuleForm());
        model.addAttribute("ruleId", null);
        populateOptions(model);
        return "admin/pricing-rule-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ruleForm") PricingRuleForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (form.getStartTime() != null && form.getEndTime() != null && !form.getEndTime().isAfter(form.getStartTime())) {
            bindingResult.rejectValue("endTime", "error.endTime", "Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (bindingResult.hasErrors()) {
            populateOptions(model);
            return "admin/pricing-rule-form";
        }

        PricingRule entity = new PricingRule();
        mapToEntity(form, entity);
        pricingRuleRepository.save(entity);

        auditLogService.log(AuditAction.CREATE, "PricingRule", entity.getId(),
                "Thêm quy tắc giá mới: " + entity.getDayType().getLabel() + " " + entity.getPricePerHour() + "đ/h");
        redirectAttributes.addFlashAttribute("success", "Đã thêm quy tắc bảng giá mới thành công.");
        return "redirect:/admin/pricing";
    }

    @GetMapping("/{id}/edit")
    @Transactional(readOnly = true)
    public String editPage(@PathVariable Long id, Model model) {
        PricingRule entity = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("quy tắc giá", id));
        PricingRuleForm form = new PricingRuleForm();
        form.setCourtId(entity.getCourt() != null ? entity.getCourt().getId() : null);
        form.setBranchId(entity.getBranch() != null ? entity.getBranch().getId() : null);
        form.setDayType(entity.getDayType());
        form.setStartTime(entity.getStartTime());
        form.setEndTime(entity.getEndTime());
        form.setPricePerHour(entity.getPricePerHour());
        form.setPriority(entity.getPriority());
        form.setActive(entity.isActive());

        model.addAttribute("ruleForm", form);
        model.addAttribute("ruleId", id);
        populateOptions(model);
        return "admin/pricing-rule-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("ruleForm") PricingRuleForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (form.getStartTime() != null && form.getEndTime() != null && !form.getEndTime().isAfter(form.getStartTime())) {
            bindingResult.rejectValue("endTime", "error.endTime", "Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("ruleId", id);
            populateOptions(model);
            return "admin/pricing-rule-form";
        }

        PricingRule entity = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("quy tắc giá", id));
        mapToEntity(form, entity);
        pricingRuleRepository.save(entity);

        auditLogService.log(AuditAction.UPDATE, "PricingRule", id,
                "Cập nhật quy tắc giá: " + entity.getDayType().getLabel() + " " + entity.getPricePerHour() + "đ/h");
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật quy tắc giá thành công.");
        return "redirect:/admin/pricing";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        PricingRule entity = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("quy tắc giá", id));
        entity.setActive(!entity.isActive());
        pricingRuleRepository.save(entity);

        auditLogService.log(AuditAction.STATUS_CHANGE, "PricingRule", id,
                (entity.isActive() ? "Kích hoạt" : "Tạm ngưng") + " quy tắc giá ID " + id);
        redirectAttributes.addFlashAttribute("success",
                "Đã " + (entity.isActive() ? "kích hoạt" : "tạm ngưng") + " quy tắc giá.");
        return "redirect:/admin/pricing";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        PricingRule entity = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("quy tắc giá", id));
        pricingRuleRepository.delete(entity);

        auditLogService.log(AuditAction.DELETE, "PricingRule", id, "Xóa quy tắc giá ID " + id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa quy tắc giá thành công.");
        return "redirect:/admin/pricing";
    }

    private void mapToEntity(PricingRuleForm form, PricingRule entity) {
        if (form.getCourtId() != null) {
            Court court = courtRepository.findById(form.getCourtId())
                    .orElseThrow(() -> new ResourceNotFoundException("sân", form.getCourtId()));
            entity.setCourt(court);
            entity.setBranch(court.getBranch());
        } else if (form.getBranchId() != null) {
            Branch branch = branchRepository.findById(form.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("cơ sở", form.getBranchId()));
            entity.setCourt(null);
            entity.setBranch(branch);
        } else {
            entity.setCourt(null);
            entity.setBranch(null);
        }
        entity.setDayType(form.getDayType());
        entity.setStartTime(form.getStartTime());
        entity.setEndTime(form.getEndTime());
        entity.setPricePerHour(form.getPricePerHour());
        entity.setPriority(form.getPriority());
        entity.setActive(form.isActive());
    }

    private void populateOptions(Model model) {
        model.addAttribute("courts", courtRepository.findAllWithBranchOrderBySortOrderAsc());
        model.addAttribute("branches", branchRepository.findByStatusOrderByNameAsc(CourtStatus.ACTIVE));
        model.addAttribute("dayTypes", DayType.values());
    }
}
