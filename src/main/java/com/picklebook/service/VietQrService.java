package com.picklebook.service;

import com.picklebook.dto.view.VietQrPaymentInfo;

import java.math.BigDecimal;

public interface VietQrService {

    /**
     * Tạo thông tin VietQR đầy đủ (gồm mã QR thanh toán 100% và mã QR đặt cọc)
     *
     * @param orderCode Mã đơn đặt chỗ hoặc mã hợp đồng cố định
     * @param totalAmount Tổng số tiền cần thanh toán
     * @return Thông tin VietQR với các liên kết hình ảnh và dữ liệu sao chép
     */
    VietQrPaymentInfo generatePaymentInfo(String orderCode, BigDecimal totalAmount);

    /**
     * Lấy tỷ lệ phần trăm tiền cọc cấu hình (mặc định 30%)
     */
    int getDepositPercent();
}

