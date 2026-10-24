package com.dulo.coffeechat.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class TimeSlot {

    public static final int MIN_DURATION_MINUTES = 10;
    public static final int MAX_DURATION_MINUTES = 240;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private boolean booked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TimeSlot() {
    }

    public TimeSlot(LocalDateTime startTime, int durationMinutes, LocalDateTime createdAt) {
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isBooked() {
        return booked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return startTime.isBefore(end) && getEndTime().isAfter(start);
    }

    public void markBooked() {
        this.booked = true;
    }
}
