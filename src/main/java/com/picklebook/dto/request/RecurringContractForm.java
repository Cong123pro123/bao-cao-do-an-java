package com.picklebook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

public class RecurringContractForm {

    @NotNull(message = "Vui lòng chọn sân.")
    private Long courtId;

    private int year;
    private int month;

    @NotEmpty(message = "Vui lòng chọn ít nhất một thứ trong tuần.")
    private List<String> daysOfWeek = new ArrayList<>();

    @NotBlank(message = "Vui lòng chọn giờ bắt đầu.")
    private String startTime;

    @NotBlank(message = "Vui lòng chọn giờ kết thúc.")
    private String endTime;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    private String customerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$", message = "Số điện thoại không hợp lệ (định dạng Việt Nam: 09xx, 03xx, 07xx, 08xx).")
    private String customerPhone;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Địa chỉ email không hợp lệ.")
    private String customerEmail;

    private String note;
    private String paymentMethod = "BANK_TRANSFER";

    public Long getCourtId() {
        return courtId;
    }

    public void setCourtId(Long courtId) {
        this.courtId = courtId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

