// ReservationEntity.java
package com.restaurant.reservationservice.entity;

import com.restaurant.data.entity.IBaseEntity;
import com.restaurant.reservationservice.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEntity implements IBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "confirmation_code", unique = true, nullable = false)
    private String confirmationCode; // e.g., "RES-20251201-A3B5"

    // Customer info (for registered users)
    @Column(name = "user_id")
    private String userId;

    // Guest info (for non-registered guests)
    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_phone")
    private String guestPhone;

    // Table assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private TableEntity table;

    // Reservation details
    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "special_requests", length = 1000)
    private String specialRequests;

    // Pre-order link
    @Column(name = "pre_order_id")
    private Long preOrderId;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}