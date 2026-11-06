package com.picklebook.controller.admin;

import com.picklebook.dto.request.ExtraServiceForm;
import com.picklebook.entity.ExtraService;
import com.picklebook.entity.enums.AuditAction;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.repository.ExtraServiceRepository;
import com.picklebook.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/services")
public class AdminExtraServiceController {

    private final ExtraServiceRepository extraServiceRepository;
    private final AuditLogService auditLogService;

    public AdminExtraServiceController(ExtraServiceRepository extraServiceRepository,
            AuditLogService auditLogService) {
        this.extraServiceRepository = extraServiceRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("services", extraServiceRepository.findAllByOrderBySortOrderAsc());
        return "admin/services";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("serviceForm", new ExtraServiceForm());
        model.addAttribute("serviceId", null);
        return "admin/service-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("serviceForm") ExtraServiceForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/service-form";
        }

        ExtraService entity = new ExtraService();
        mapToEntity(form, entity);
        extraServiceRepository.save(entity);

        auditLogService.log(AuditAction.CREATE, "ExtraService", entity.getId(),
                "Thêm dịch vụ / HLV mới: " + entity.getName());
        redirectAttributes.addFlashAttribute("success", "Đã thêm dịch vụ mới thành công.");
        return "redirect:/admin/services";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        ExtraService entity = extraServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("dịch vụ", id));
        ExtraServiceForm form = new ExtraServiceForm();
        form.setName(entity.getName());
        form.setDescription(entity.getDescription());
        form.setPrice(entity.getPrice());
        form.setUnit(entity.getUnit());
        form.setActive(entity.isActive());
        form.setSortOrder(entity.getSortOrder());

        model.addAttribute("serviceForm", form);
        model.addAttribute("serviceId", id);
        return "admin/service-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
            @Valid @ModelAttribute("serviceForm") ExtraServiceForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("serviceId", id);
            return "admin/service-form";
        }

        ExtraService entity = extraServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("dịch vụ", id));
        mapToEntity(form, entity);
        extraServiceRepository.save(entity);

        auditLogService.log(AuditAction.UPDATE, "ExtraService", id,
                "Cập nhật dịch vụ / HLV: " + entity.getName());
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật dịch vụ thành công.");
        return "redirect:/admin/services";
    }

    @PostMapping("/{id}/toggle")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ExtraService entity = extraServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("dịch vụ", id));
        entity.setActive(!entity.isActive());
        extraServiceRepository.save(entity);

        auditLogService.log(AuditAction.STATUS_CHANGE, "ExtraService", id,
                (entity.isActive() ? "Kích hoạt" : "Tạm ngưng") + " dịch vụ: " + entity.getName());
        redirectAttributes.addFlashAttribute("success",
                "Đã " + (entity.isActive() ? "kích hoạt" : "tạm ngưng") + " dịch vụ " + entity.getName() + ".");
        return "redirect:/admin/services";
    }

    private void mapToEntity(ExtraServiceForm form, ExtraService entity) {
        entity.setName(form.getName());
        entity.setDescription(form.getDescription());
        entity.setPrice(form.getPrice());
        entity.setUnit(form.getUnit());
        entity.setActive(form.isActive());
        entity.setSortOrder(form.getSortOrder());
    }
}
