package com.picklebook.dto.view;

import java.math.BigDecimal;

/**
 * Thông tin thanh toán chuyển khoản VietQR chuẩn Napas 24/7.
 */
public class VietQrPaymentInfo {

    private String bankName;
    private String bankBin;
    private String accountNo;
    private String accountName;
    private String orderCode;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private String fullQrUrl;
    private String depositQrUrl;
    private String transferContent;

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankBin() {
        return bankBin;
    }

    public void setBankBin(String bankBin) {
        this.bankBin = bankBin;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getFullQrUrl() {
        return fullQrUrl;
    }

    public void setFullQrUrl(String fullQrUrl) {
        this.fullQrUrl = fullQrUrl;
    }

    public String getDepositQrUrl() {
        return depositQrUrl;
    }

    public void setDepositQrUrl(String depositQrUrl) {
        this.depositQrUrl = depositQrUrl;
    }

    public String getTransferContent() {
        return transferContent;
    }

    public void setTransferContent(String transferContent) {
        this.transferContent = transferContent;
    }
}

