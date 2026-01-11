// ReservationFilter.java
package com.restaurant.reservationservice.filter;

import com.restaurant.data.model.IFilter;
import com.restaurant.reservationservice.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationFilter implements IFilter {

    private Long id;
    private String userId;
    private Long tableId;
    private ReservationStatus status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String confirmationCode;
    private String guestEmail;
    private String guestPhone;
}