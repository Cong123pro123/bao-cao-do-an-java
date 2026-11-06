package com.picklebook.dto.view;

import com.picklebook.entity.Court;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CourtTimelineView(
        Court court,
        List<Item> items,
        int totalSlots,
        int bookedSlots,
        int availableSlots) {

    public record Item(
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal price,
            String status,
            boolean available,
            String customerName,
            String customerPhone,
            String bookingCode,
            String bookingStatus) {
    }
}

