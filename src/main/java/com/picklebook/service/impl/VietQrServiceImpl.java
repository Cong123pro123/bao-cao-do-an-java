package com.picklebook.service.impl;

import com.picklebook.dto.view.VietQrPaymentInfo;
import com.picklebook.service.SettingService;
import com.picklebook.service.VietQrService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class VietQrServiceImpl implements VietQrService {

    private final SettingService settingService;

    public VietQrServiceImpl(SettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    public VietQrPaymentInfo generatePaymentInfo(String orderCode, BigDecimal totalAmount) {
        String bankName = settingService.get("payment.bank_name", "Vietcombank (Ngân hàng TMCP Ngoại thương Việt Nam)");
        String bankBin = settingService.get("payment.bank_bin", "VCB");
        String accountNo = settingService.get("payment.bank_account_no", "9342804412");
        String accountName = settingService.get("payment.bank_account_name", "DOAN NGUYEN THANH CONG");
        int depositPercent = getDepositPercent();

        BigDecimal amount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal depositAmount = amount.multiply(BigDecimal.valueOf(depositPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String encodedAddInfo = URLEncoder.encode(orderCode, StandardCharsets.UTF_8);

        String fullQrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                bankBin, accountNo, amount.toPlainString(), encodedAddInfo, encodedAccountName);

        String depositQrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                bankBin, accountNo, depositAmount.toPlainString(), encodedAddInfo, encodedAccountName);

        VietQrPaymentInfo info = new VietQrPaymentInfo();
        info.setBankName(bankName);
        info.setBankBin(bankBin);
        info.setAccountNo(accountNo);
        info.setAccountName(accountName);
        info.setOrderCode(orderCode);
        info.setTransferContent(orderCode);
        info.setTotalAmount(amount);
        info.setDepositAmount(depositAmount);
        info.setFullQrUrl(fullQrUrl);
        info.setDepositQrUrl(depositQrUrl);

        return info;
    }

    @Override
    public int getDepositPercent() {
        return settingService.getInt("payment.deposit_percent", 30);
    }
}

