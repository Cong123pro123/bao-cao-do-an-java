package com.picklebook.service.impl;

import com.picklebook.dto.request.CourtForm;
import com.picklebook.entity.Branch;
import com.picklebook.entity.Court;
import com.picklebook.entity.CourtSchedule;
import com.picklebook.entity.CourtImage;
import com.picklebook.entity.enums.CourtStatus;
import com.picklebook.exception.BusinessException;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.repository.BranchRepository;
import com.picklebook.repository.CourtImageRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.service.AdminCourtService;
import com.picklebook.service.FileStorageService;
import com.picklebook.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminCourtServiceImpl implements AdminCourtService {

    private final CourtRepository courtRepository;
    private final BranchRepository branchRepository;
    private final CourtImageRepository courtImageRepository;
    private final FileStorageService fileStorageService;
    private final SlugUtils slugUtils;

    public AdminCourtServiceImpl(CourtRepository courtRepository,
                                 BranchRepository branchRepository,
                                 CourtImageRepository courtImageRepository,
                                 FileStorageService fileStorageService,
                                 SlugUtils slugUtils) {
        this.courtRepository = courtRepository;
        this.branchRepository = branchRepository;
        this.courtImageRepository = courtImageRepository;
        this.fileStorageService = fileStorageService;
        this.slugUtils = slugUtils;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Court> getAllCourts() {
        return courtRepository.findAllWithBranchOrderBySortOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Court getCourt(long id) {
        Court court = courtRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        court.getBranch().getName();
        return court;
    }

    @Override
    @Transactional
    public Court create(CourtForm form) {
        validateSchedules(form);
        Branch branch = getActiveBranch(form.getBranchId());
        Court court = new Court();
        applyForm(court, form, branch);
        court.setSlug(createUniqueSlug(form.getName()));
        replaceSchedules(court, form.getSchedules());
        Court saved = courtRepository.save(court);

        // Lưu ảnh đại diện nếu có upload hoặc link ảnh
        if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
            String url = fileStorageService.store(form.getImageFile(), "courts");
            CourtImage img = new CourtImage();
            img.setCourt(saved);
            img.setImageUrl(url);
            img.setPrimary(true);
            img.setSortOrder(0);
            img.setAltText(saved.getName());
            courtImageRepository.save(img);
        } else if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
            CourtImage img = new CourtImage();
            img.setCourt(saved);
            img.setImageUrl(form.getImageUrl().trim());
            img.setPrimary(true);
            img.setSortOrder(0);
            img.setAltText(saved.getName());
            courtImageRepository.save(img);
        }

        return saved;
    }

    @Override
    @Transactional
    public Court update(long id, CourtForm form) {
        validateSchedules(form);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        Branch branch = getActiveBranch(form.getBranchId());
        applyForm(court, form, branch);
        replaceSchedules(court, form.getSchedules());

        if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
            String url = fileStorageService.store(form.getImageFile(), "courts");
            CourtImage img = new CourtImage();
            img.setCourt(court);
            img.setImageUrl(url);
            boolean hasImages = !courtImageRepository.findByCourtIdOrderBySortOrderAsc(id).isEmpty();
            img.setPrimary(!hasImages);
            img.setSortOrder(10);
            img.setAltText(court.getName());
            courtImageRepository.save(img);
        }

        return courtRepository.save(court);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtImage> getCourtImages(long courtId) {
        return courtImageRepository.findByCourtIdOrderBySortOrderAsc(courtId);
    }

    @Override
    @Transactional
    public CourtImage addCourtImage(long courtId, MultipartFile file, String imageUrl, String altText, boolean primary) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        String finalUrl;
        if (file != null && !file.isEmpty()) {
            finalUrl = fileStorageService.store(file, "courts");
        } else if (imageUrl != null && !imageUrl.isBlank()) {
            finalUrl = imageUrl.trim();
        } else {
            throw new BusinessException("Vui lòng tải lên tệp ảnh hoặc dán URL ảnh hợp lệ.");
        }

        List<CourtImage> existing = courtImageRepository.findByCourtIdOrderBySortOrderAsc(courtId);
        if (primary || existing.isEmpty()) {
            for (CourtImage img : existing) {
                if (img.isPrimary()) {
                    img.setPrimary(false);
                    courtImageRepository.save(img);
                }
            }
            primary = true;
        }

        CourtImage img = new CourtImage();
        img.setCourt(court);
        img.setImageUrl(finalUrl);
        img.setAltText(altText != null && !altText.isBlank() ? altText.trim() : court.getName());
        img.setPrimary(primary);
        img.setSortOrder(existing.size() + 1);
        return courtImageRepository.save(img);
    }

    @Override
    @Transactional
    public void deleteCourtImage(long courtId, long imageId) {
        CourtImage img = courtImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hình ảnh."));
        if (!img.getCourt().getId().equals(courtId)) {
            throw new ResourceNotFoundException("Hình ảnh không thuộc về sân này.");
        }
        fileStorageService.delete(img.getImageUrl());
        boolean wasPrimary = img.isPrimary();
        courtImageRepository.delete(img);

        if (wasPrimary) {
            List<CourtImage> remaining = courtImageRepository.findByCourtIdOrderBySortOrderAsc(courtId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setPrimary(true);
                courtImageRepository.save(remaining.get(0));
            }
        }
    }

    @Override
    @Transactional
    public void setPrimaryCourtImage(long courtId, long imageId) {
        List<CourtImage> images = courtImageRepository.findByCourtIdOrderBySortOrderAsc(courtId);
        boolean found = false;
        for (CourtImage img : images) {
            if (img.getId().equals(imageId)) {
                img.setPrimary(true);
                found = true;
            } else if (img.isPrimary()) {
                img.setPrimary(false);
            }
            courtImageRepository.save(img);
        }
        if (!found) {
            throw new ResourceNotFoundException("Không tìm thấy hình ảnh.");
        }
    }

    private Branch getActiveBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("Cơ sở đã chọn không tồn tại."));
        if (branch.getStatus() != CourtStatus.ACTIVE) {
            throw new BusinessException("Chỉ có thể gán sân vào cơ sở đang hoạt động.");
        }
        return branch;
    }

    private void applyForm(Court court, CourtForm form, Branch branch) {
        court.setName(form.getName().trim());
        court.setDescription(blankToNull(form.getDescription()));
        court.setBranch(branch);
        court.setCourtType(form.getCourtType());
        court.setStatus(form.getStatus());
        court.setBasePrice(form.getBasePrice());
        court.setSlotMinutes(form.getSlotMinutes());
        court.setBufferMinutes(form.getBufferMinutes());
        court.setMinNoticeMinutes(form.getMinNoticeMinutes());
        court.setMaxAdvanceDays(form.getMaxAdvanceDays());
        court.setMaxSlotsPerBooking(form.getMaxSlotsPerBooking());
        court.setAmenities(normalizeAmenities(form.getAmenities()));
        court.setSortOrder(form.getSortOrder());
    }

    private void replaceSchedules(Court court, List<CourtForm.ScheduleForm> scheduleForms) {
        court.getSchedules().clear();
        for (CourtForm.ScheduleForm form : scheduleForms) {
            CourtSchedule schedule = new CourtSchedule();
            schedule.setCourt(court);
            schedule.setDayOfWeek(form.getDayOfWeek());
            schedule.setOpenTime(form.getOpenTime());
            schedule.setCloseTime(form.getCloseTime());
            schedule.setClosed(form.isClosed());
            court.getSchedules().add(schedule);
        }
    }

    private void validateSchedules(CourtForm form) {
        if (form.getSchedules() == null || form.getSchedules().size() != 7) {
            throw new BusinessException("Cần khai báo giờ hoạt động cho đủ 7 ngày trong tuần.");
        }
        Set<Integer> days = new HashSet<>();
        for (CourtForm.ScheduleForm schedule : form.getSchedules()) {
            int day = schedule.getDayOfWeek();
            LocalTime open = schedule.getOpenTime();
            LocalTime close = schedule.getCloseTime();
            if (day < 1 || day > 7 || !days.add(day)) {
                throw new BusinessException("Lịch hoạt động phải có đúng một dòng cho mỗi ngày trong tuần.");
            }
            if (open == null || close == null) {
                throw new BusinessException("Vui lòng nhập giờ mở cửa và đóng cửa cho từng ngày.");
            }
            if (!schedule.isClosed() && !close.isAfter(open)) {
                throw new BusinessException("Giờ đóng cửa phải sau giờ mở cửa ở ngày " + schedule.getDayLabel() + ".");
            }
        }
    }

    private String createUniqueSlug(String name) {
        String baseSlug = slugUtils.toSlug(name);
        if (baseSlug.isBlank()) {
            throw new BusinessException("Tên sân cần có ký tự để tạo đường dẫn.");
        }
        String slug = baseSlug;
        int suffix = 2;
        while (courtRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }
        return slug;
    }

    private String normalizeAmenities(String amenities) {
        if (amenities == null || amenities.isBlank()) {
            return null;
        }
        return String.join("|", java.util.Arrays.stream(amenities.split("[,|]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
