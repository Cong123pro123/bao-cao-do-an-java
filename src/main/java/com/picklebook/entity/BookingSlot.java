package com.picklebook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một khung giờ thuộc đơn đặt sân.
 *
 * <p>UNIQUE(court_id, start_at, active_key) là lớp bảo vệ cuối cùng chống
 * hai người đặt cùng một khung giờ. Khi hủy/đổi lịch, activeKey được đặt
 * về null để nhường slot nhưng vẫn giữ lịch sử.</p>
 */
@Entity
@Table(name = "booking_slots")
public class BookingSlot {

    public static final int ACTIVE = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "price", nullable = false, precision = 12, scale = 0)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "active_key")
    private Integer activeKey = ACTIVE;

    public void release() {
        this.activeKey = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Court getCourt() {
        return court;
    }

    public void setCourt(Court court) {
        this.court = court;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getActiveKey() {
        return activeKey;
    }

    public void setActiveKey(Integer activeKey) {
        this.activeKey = activeKey;
    }
}
