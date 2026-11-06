package com.picklebook.controller.admin;

import com.picklebook.dto.request.CourtForm;
import com.picklebook.entity.Court;
import com.picklebook.entity.CourtSchedule;
import com.picklebook.entity.enums.CourtStatus;
import com.picklebook.entity.enums.CourtType;
import com.picklebook.exception.BusinessException;
import com.picklebook.repository.BranchRepository;
import com.picklebook.service.AdminCourtService;
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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/courts")
public class AdminCourtController {

    private final AdminCourtService adminCourtService;
    private final BranchRepository branchRepository;

    public AdminCourtController(AdminCourtService adminCourtService, BranchRepository branchRepository) {
        this.adminCourtService = adminCourtService;
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("courts", adminCourtService.getAllCourts());
        return "admin/courts";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("courtForm", new CourtForm());
        model.addAttribute("courtId", null);
        populateFormOptions(model);
        return "admin/court-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("courtForm") CourtForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormOptions(model);
            return "admin/court-form";
        }
        try {
            adminCourtService.create(form);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sân và lịch hoạt động.");
            return "redirect:/admin/courts";
        } catch (BusinessException ex) {
            bindingResult.reject("court.invalid", ex.getMessage());
            populateFormOptions(model);
            return "admin/court-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        Court court = adminCourtService.getCourt(id);
        model.addAttribute("courtForm", toForm(court));
        model.addAttribute("courtId", id);
        model.addAttribute("court", court);
        model.addAttribute("courtImages", adminCourtService.getCourtImages(id));
        populateFormOptions(model);
        return "admin/court-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("courtForm") CourtForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courtId", id);
            model.addAttribute("courtImages", adminCourtService.getCourtImages(id));
            populateFormOptions(model);
            return "admin/court-form";
        }
        try {
            adminCourtService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật thông tin và lịch hoạt động của sân.");
            return "redirect:/admin/courts";
        } catch (BusinessException ex) {
            bindingResult.reject("court.invalid", ex.getMessage());
            model.addAttribute("courtId", id);
            model.addAttribute("courtImages", adminCourtService.getCourtImages(id));
            populateFormOptions(model);
            return "admin/court-form";
        }
    }

    @PostMapping("/{id}/images")
    public String uploadImage(@PathVariable Long id,
                              @RequestParam(required = false) org.springframework.web.multipart.MultipartFile imageFile,
                              @RequestParam(required = false) String imageUrl,
                              @RequestParam(required = false) String altText,
                              @RequestParam(defaultValue = "false") boolean primary,
                              RedirectAttributes redirectAttributes) {
        try {
            adminCourtService.addCourtImage(id, imageFile, imageUrl, altText, primary);
            redirectAttributes.addFlashAttribute("success", "Đã tải lên hình ảnh mới cho sân.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/courts/" + id + "/edit";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id,
                              @PathVariable Long imageId,
                              RedirectAttributes redirectAttributes) {
        try {
            adminCourtService.deleteCourtImage(id, imageId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa hình ảnh của sân.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/courts/" + id + "/edit";
    }

    @PostMapping("/{id}/images/{imageId}/primary")
    public String setPrimaryImage(@PathVariable Long id,
                                 @PathVariable Long imageId,
                                 RedirectAttributes redirectAttributes) {
        try {
            adminCourtService.setPrimaryCourtImage(id, imageId);
            redirectAttributes.addFlashAttribute("success", "Đã đặt làm ảnh đại diện chính của sân.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/courts/" + id + "/edit";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam CourtStatus status,
                               RedirectAttributes redirectAttributes) {
        CourtForm form = toForm(adminCourtService.getCourt(id));
        form.setStatus(status);
        try {
            adminCourtService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái sân.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/courts";
    }

    private CourtForm toForm(Court court) {
        CourtForm form = new CourtForm();
        form.setName(court.getName());
        form.setDescription(court.getDescription());
        form.setBranchId(court.getBranch().getId());
        form.setCourtType(court.getCourtType());
        form.setStatus(court.getStatus());
        form.setBasePrice(court.getBasePrice());
        form.setSlotMinutes(court.getSlotMinutes());
        form.setBufferMinutes(court.getBufferMinutes());
        form.setMinNoticeMinutes(court.getMinNoticeMinutes());
        form.setMaxAdvanceDays(court.getMaxAdvanceDays());
        form.setMaxSlotsPerBooking(court.getMaxSlotsPerBooking());
        form.setAmenities(court.getAmenities() == null ? "" : court.getAmenities().replace('|', ','));
        form.setSortOrder(court.getSortOrder());

        Map<Integer, CourtSchedule> schedulesByDay = court.getSchedules().stream()
                .collect(Collectors.toMap(CourtSchedule::getDayOfWeek, Function.identity()));
        form.setSchedules(CourtForm.defaultSchedules().stream().map(defaultSchedule -> {
            CourtSchedule schedule = schedulesByDay.get(defaultSchedule.getDayOfWeek());
            if (schedule == null) {
                return defaultSchedule;
            }
            return new CourtForm.ScheduleForm(schedule.getDayOfWeek(), schedule.getOpenTime(),
                    schedule.getCloseTime(), schedule.isClosed());
        }).toList());
        return form;
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("branches", branchRepository.findByStatusOrderByNameAsc(CourtStatus.ACTIVE));
        model.addAttribute("courtTypes", CourtType.values());
        model.addAttribute("courtStatuses", CourtStatus.values());
    }
}
