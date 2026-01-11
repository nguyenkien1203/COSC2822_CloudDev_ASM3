// ReservationDto.java
package com.restaurant.reservationservice.dto;

import com.restaurant.data.model.IBaseModel;
import com.restaurant.reservationservice.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto implements IBaseModel<Long> {

    private Long id;
    private String confirmationCode;
    private String userId;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private TableDto table;
    private Integer partySize;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ReservationStatus status;
    private String specialRequests;
    private Long preOrderId;
    private Boolean reminderSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}