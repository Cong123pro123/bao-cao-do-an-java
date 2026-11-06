package com.picklebook.controller.admin;

import com.picklebook.dto.request.BlockedTimeForm;
import com.picklebook.entity.BlockedTime;
import com.picklebook.entity.Court;
import com.picklebook.entity.User;
import com.picklebook.entity.enums.AuditAction;
import com.picklebook.entity.enums.BlockType;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.repository.BlockedTimeRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.security.SecurityUtils;
import com.picklebook.service.AuditLogService;
import com.picklebook.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/blocked-times")
public class AdminBlockedTimeController {

    private final BlockedTimeRepository blockedTimeRepository;
    private final CourtRepository courtRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public AdminBlockedTimeController(BlockedTimeRepository blockedTimeRepository,
                                      CourtRepository courtRepository,
                                      AuditLogService auditLogService,
                                      UserService userService) {
        this.blockedTimeRepository = blockedTimeRepository;
        this.courtRepository = courtRepository;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long courtId, Model model) {
        List<BlockedTime> blockedTimes = (courtId != null)
                ? blockedTimeRepository.findByCourtIdOrderByStartAtDesc(courtId)
                : blockedTimeRepository.findAllByOrderByStartAtDesc();

        model.addAttribute("blockedTimes", blockedTimes);
        model.addAttribute("courts", courtRepository.findAllWithBranchOrderBySortOrderAsc());
        model.addAttribute("selectedCourtId", courtId);
        return "admin/blocked-times";
    }

    @GetMapping("/new")
    public String createPage(@RequestParam(required = false) Long courtId, Model model) {
        BlockedTimeForm form = new BlockedTimeForm();
        form.setCourtId(courtId);
        LocalDateTime nowHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1);
        form.setStartAt(nowHour);
        form.setEndAt(nowHour.plusHours(2));
        model.addAttribute("blockedForm", form);
        model.addAttribute("blockedId", null);
        populateOptions(model);
        return "admin/blocked-time-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("blockedForm") BlockedTimeForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (form.getStartAt() != null && form.getEndAt() != null && !form.getEndAt().isAfter(form.getStartAt())) {
            bindingResult.rejectValue("endAt", "error.endAt", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (bindingResult.hasErrors()) {
            populateOptions(model);
            return "admin/blocked-time-form";
        }

        BlockedTime entity = new BlockedTime();
        mapToEntity(form, entity);
        User creator = SecurityUtils.currentUserId().map(userService::getById).orElse(null);
        entity.setCreatedBy(creator);
        blockedTimeRepository.save(entity);

        auditLogService.log(AuditAction.CREATE, "BlockedTime", entity.getId(),
                "Thêm lịch chặn / bảo trì: " + entity.getTitle());
        redirectAttributes.addFlashAttribute("success", "Đã thêm lịch chặn / bảo trì thành công.");
        return "redirect:/admin/blocked-times";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        BlockedTime entity = blockedTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch chặn", id));
        BlockedTimeForm form = new BlockedTimeForm();
        form.setCourtId(entity.getCourt() != null ? entity.getCourt().getId() : null);
        form.setBlockType(entity.getBlockType());
        form.setTitle(entity.getTitle());
        form.setStartAt(entity.getStartAt());
        form.setEndAt(entity.getEndAt());
        form.setAffectsPricing(entity.isAffectsPricing());

        model.addAttribute("blockedForm", form);
        model.addAttribute("blockedId", id);
        populateOptions(model);
        return "admin/blocked-time-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("blockedForm") BlockedTimeForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (form.getStartAt() != null && form.getEndAt() != null && !form.getEndAt().isAfter(form.getStartAt())) {
            bindingResult.rejectValue("endAt", "error.endAt", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("blockedId", id);
            populateOptions(model);
            return "admin/blocked-time-form";
        }

        BlockedTime entity = blockedTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch chặn", id));
        mapToEntity(form, entity);
        blockedTimeRepository.save(entity);

        auditLogService.log(AuditAction.UPDATE, "BlockedTime", id,
                "Cập nhật lịch chặn / bảo trì: " + entity.getTitle());
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật lịch chặn / bảo trì thành công.");
        return "redirect:/admin/blocked-times";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        BlockedTime entity = blockedTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch chặn", id));
        blockedTimeRepository.delete(entity);
        auditLogService.log(AuditAction.DELETE, "BlockedTime", id,
                "Xóa lịch chặn: " + entity.getTitle());
        redirectAttributes.addFlashAttribute("success", "Đã xóa lịch chặn thành công.");
        return "redirect:/admin/blocked-times";
    }

    private void mapToEntity(BlockedTimeForm form, BlockedTime entity) {
        if (form.getCourtId() != null) {
            Court court = courtRepository.findById(form.getCourtId())
                    .orElseThrow(() -> new ResourceNotFoundException("sân", form.getCourtId()));
            entity.setCourt(court);
        } else {
            entity.setCourt(null);
        }
        entity.setBlockType(form.getBlockType());
        entity.setTitle(form.getTitle());
        entity.setStartAt(form.getStartAt());
        entity.setEndAt(form.getEndAt());
        entity.setAffectsPricing(form.isAffectsPricing());
    }

    private void populateOptions(Model model) {
        model.addAttribute("courts", courtRepository.findAllWithBranchOrderBySortOrderAsc());
        model.addAttribute("blockTypes", BlockType.values());
    }
}
