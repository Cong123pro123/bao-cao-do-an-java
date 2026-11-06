package com.picklebook.dto.view;

import com.picklebook.entity.Booking;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardSummary(
        long todayBookings,
        long weekBookings,
        long monthBookings,
        BigDecimal monthRevenue,
        long activeCourts,
        List<Booking> todaySchedule,
        long todayCompleted,
        long todayConfirmed,
        long todayPending,
        BigDecimal todayRevenue) {

    public AdminDashboardSummary(
            long todayBookings,
            long weekBookings,
            long monthBookings,
            BigDecimal monthRevenue,
            long activeCourts,
            List<Booking> todaySchedule) {
        this(todayBookings, weekBookings, monthBookings, monthRevenue, activeCourts, todaySchedule,
                0, 0, 0, BigDecimal.ZERO);
    }
}
