package com.dulo.coffeechat.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false, unique = true)
    private TimeSlot timeSlot;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 280)
    private String topic;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Booking() {
    }

    public Booking(TimeSlot timeSlot, String name, String email, String topic, LocalDateTime createdAt) {
        this.timeSlot = timeSlot;
        this.name = name;
        this.email = email;
        this.topic = topic;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTopic() {
        return topic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
