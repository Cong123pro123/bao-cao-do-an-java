package com.picklebook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookingForm {

    @NotNull
    private Long courtId;

    @NotNull
    private LocalDate date;

    @NotEmpty(message = "Vui lòng chọn ít nhất một khung giờ.")
    private List<String> slotStarts = new ArrayList<>();

    @Size(max = 120)
    private String customerName;

    @Size(max = 20)
    private String customerPhone;

    @Email
    @Size(max = 160)
    private String customerEmail;

    @Min(1)
    @Max(20)
    private int playerCount = 2;

    @Size(max = 2000)
    private String note;

    @Size(max = 40)
    private String couponCode;

    @NotNull
    private String paymentMethod = "CASH";

    private List<Long> extraServiceIds = new ArrayList<>();
    private Map<Long, String> answers = new LinkedHashMap<>();

    public Long getCourtId() { return courtId; }
    public void setCourtId(Long courtId) { this.courtId = courtId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<String> getSlotStarts() { return slotStarts; }
    public void setSlotStarts(List<String> slotStarts) { this.slotStarts = slotStarts; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public List<Long> getExtraServiceIds() { return extraServiceIds; }
    public void setExtraServiceIds(List<Long> extraServiceIds) { this.extraServiceIds = extraServiceIds; }
    public Map<Long, String> getAnswers() { return answers; }
    public void setAnswers(Map<Long, String> answers) { this.answers = answers; }
}
