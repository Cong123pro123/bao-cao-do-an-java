package com.picklebook.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Định dạng tiền VNĐ và các tiện ích số.
 */
public final class MoneyUtils {

    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private MoneyUtils() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "0 ₫";
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(VIETNAM);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(amount) + " ₫";
    }

    public static String format(long amount) {
        return format(BigDecimal.valueOf(amount));
    }

    public static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
