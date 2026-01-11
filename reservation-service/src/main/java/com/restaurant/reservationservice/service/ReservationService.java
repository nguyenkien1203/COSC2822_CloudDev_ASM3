// ReservationService.java
package com.restaurant.reservationservice.service;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.redismodule.exception.CacheException;
import com.restaurant.reservationservice.dto.*;
import com.restaurant.reservationservice.enums.ReservationStatus;
import com.restaurant.reservationservice.filter.ReservationFilter;

import java.util.List;

public interface ReservationService {

    // ========== CUSTOMER OPERATIONS ==========
    ReservationDto createReservation(CreateReservationRequest request, String userId) throws DataFactoryException;

    ReservationDto createGuestReservation(CreateReservationRequest request) throws DataFactoryException;

    ReservationDto getReservationById(Long id) throws CacheException, DataFactoryException;

    ReservationDto getByConfirmationCode(String code) throws CacheException, DataFactoryException;

    List<ReservationDto> getMyReservations(String userId) throws CacheException, DataFactoryException;

    ReservationDto updateReservation(Long id, UpdateReservationRequest request, String userId)
            throws CacheException, DataFactoryException;

    void cancelReservation(Long id, String userId) throws CacheException, DataFactoryException;

    // ========== AVAILABILITY ==========
    AvailabilityResponse checkAvailability(AvailabilityRequest request);

    // ========== ADMIN OPERATIONS ==========
    List<ReservationDto> getAllReservations(ReservationFilter filter) throws CacheException, DataFactoryException;

    List<ReservationDto> getTodayReservations();

    List<ReservationDto> getUpcomingReservations();

    ReservationDto updateStatus(Long id, UpdateStatusRequest request) throws CacheException, DataFactoryException;

    ReservationDto assignTable(Long reservationId, AssignTableRequest request)
            throws CacheException, DataFactoryException;

    // ========== PRE-ORDER INTEGRATION ==========
    void linkPreOrder(Long reservationId, Long preOrderId) throws CacheException, DataFactoryException;
}