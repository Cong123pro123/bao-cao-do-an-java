package com.picklebook.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AvailabilitySlot(
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal price,
        String status,
        boolean available) {
}
