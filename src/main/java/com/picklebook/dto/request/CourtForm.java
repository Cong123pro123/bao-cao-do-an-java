package com.picklebook.dto.request;

import com.picklebook.entity.enums.CourtStatus;
import com.picklebook.entity.enums.CourtType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CourtForm {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 4000)
    private String description;

    @NotNull
    private Long branchId;

    @NotNull
    private CourtType courtType = CourtType.OUTDOOR;

    @NotNull
    private CourtStatus status = CourtStatus.ACTIVE;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 12, fraction = 0)
    private BigDecimal basePrice = BigDecimal.valueOf(150000);

    @Min(15)
    @Max(240)
    private int slotMinutes = 60;

    @Min(0)
    @Max(180)
    private int bufferMinutes;

    @Min(0)
    @Max(10080)
    private int minNoticeMinutes = 60;

    @Min(1)
    @Max(365)
    private int maxAdvanceDays = 30;

    @Min(1)
    @Max(24)
    private int maxSlotsPerBooking = 4;

    @Size(max = 500)
    private String amenities;

    @Min(0)
    @Max(9999)
    private int sortOrder;

    private org.springframework.web.multipart.MultipartFile imageFile;

    @Size(max = 500)
    private String imageUrl;

    private List<ScheduleForm> schedules = defaultSchedules();

    public static List<ScheduleForm> defaultSchedules() {
        List<ScheduleForm> schedules = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            schedules.add(new ScheduleForm(day, LocalTime.of(6, 0), LocalTime.of(22, 0), false));
        }
        return schedules;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public CourtType getCourtType() {
        return courtType;
    }

    public void setCourtType(CourtType courtType) {
        this.courtType = courtType;
    }

    public CourtStatus getStatus() {
        return status;
    }

    public void setStatus(CourtStatus status) {
        this.status = status;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public void setSlotMinutes(int slotMinutes) {
        this.slotMinutes = slotMinutes;
    }

    public int getBufferMinutes() {
        return bufferMinutes;
    }

    public void setBufferMinutes(int bufferMinutes) {
        this.bufferMinutes = bufferMinutes;
    }

    public int getMinNoticeMinutes() {
        return minNoticeMinutes;
    }

    public void setMinNoticeMinutes(int minNoticeMinutes) {
        this.minNoticeMinutes = minNoticeMinutes;
    }

    public int getMaxAdvanceDays() {
        return maxAdvanceDays;
    }

    public void setMaxAdvanceDays(int maxAdvanceDays) {
        this.maxAdvanceDays = maxAdvanceDays;
    }

    public int getMaxSlotsPerBooking() {
        return maxSlotsPerBooking;
    }

    public void setMaxSlotsPerBooking(int maxSlotsPerBooking) {
        this.maxSlotsPerBooking = maxSlotsPerBooking;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public org.springframework.web.multipart.MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(org.springframework.web.multipart.MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<ScheduleForm> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ScheduleForm> schedules) {
        this.schedules = schedules;
    }

    public static class ScheduleForm {

        private int dayOfWeek;

        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime openTime;

        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime closeTime;
        private boolean closed;

        public ScheduleForm() {
        }

        public ScheduleForm(int dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
            this.dayOfWeek = dayOfWeek;
            this.openTime = openTime;
            this.closeTime = closeTime;
            this.closed = closed;
        }

        public int getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(int dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public LocalTime getOpenTime() {
            return openTime;
        }

        public void setOpenTime(LocalTime openTime) {
            this.openTime = openTime;
        }

        public LocalTime getCloseTime() {
            return closeTime;
        }

        public void setCloseTime(LocalTime closeTime) {
            this.closeTime = closeTime;
        }

        public boolean isClosed() {
            return closed;
        }

        public void setClosed(boolean closed) {
            this.closed = closed;
        }

        public String getDayLabel() {
            return switch (dayOfWeek) {
                case 1 -> "Thứ 2";
                case 2 -> "Thứ 3";
                case 3 -> "Thứ 4";
                case 4 -> "Thứ 5";
                case 5 -> "Thứ 6";
                case 6 -> "Thứ 7";
                case 7 -> "Chủ nhật";
                default -> "Không xác định";
            };
        }
    }
}
