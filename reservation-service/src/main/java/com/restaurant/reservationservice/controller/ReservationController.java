// ReservationController.java
package com.restaurant.reservationservice.controller;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.redismodule.exception.CacheException;
import com.restaurant.reservationservice.dto.*;
import com.restaurant.securitymodule.model.UserPrincipal;
import com.restaurant.reservationservice.filter.ReservationFilter;
import com.restaurant.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Slf4j
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // ========== CUSTOMER ENDPOINTS ==========

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationDto> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) throws DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("POST /api/reservations - Creating reservation for user: {}", userId);

        ReservationDto reservation = reservationService.createReservation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @PostMapping("/guest")
    public ResponseEntity<ReservationDto> createGuestReservation(
            @Valid @RequestBody CreateReservationRequest request) throws DataFactoryException {

        log.info("POST /api/reservations/guest - Creating guest reservation");
        ReservationDto reservation = reservationService.createGuestReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationDto> getReservationById(
            @PathVariable Long id) throws CacheException, DataFactoryException {

        log.info("GET /api/reservations/{}", id);
        ReservationDto reservation = reservationService.getReservationById(id);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping("/confirmation/{code}")
    public ResponseEntity<ReservationDto> getByConfirmationCode(
            @PathVariable String code) throws CacheException, DataFactoryException {

        log.info("GET /api/reservations/confirmation/{}", code);
        ReservationDto reservation = reservationService.getByConfirmationCode(code);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping("/my-reservations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservationDto>> getMyReservations(
            Authentication authentication) throws CacheException, DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("GET /api/reservations/my-reservations - user: {}", userId);

        List<ReservationDto> reservations = reservationService.getMyReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationDto> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationRequest request,
            Authentication authentication) throws CacheException, DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("PUT /api/reservations/{} - user: {}", id, userId);

        ReservationDto reservation = reservationService.updateReservation(id, request, userId);
        return ResponseEntity.ok(reservation);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            Authentication authentication) throws CacheException, DataFactoryException {

        String userId = extractUserId(authentication);
        log.info("DELETE /api/reservations/{} - user: {}", id, userId);

        reservationService.cancelReservation(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @Valid AvailabilityRequest request) {

        log.info("GET /api/reservations/availability - date: {}, partySize: {}",
                request.getDate(), request.getPartySize());
        AvailabilityResponse response = reservationService.checkAvailability(request);
        return ResponseEntity.ok(response);
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationDto>> getAllReservations()
            throws CacheException, DataFactoryException {

        log.info("GET /api/reservations - Admin access");
        List<ReservationDto> reservations = reservationService.getAllReservations(null);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationDto>> getTodayReservations() {

        log.info("GET /api/reservations/today");
        List<ReservationDto> reservations = reservationService.getTodayReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationDto>> getUpcomingReservations() {

        log.info("GET /api/reservations/upcoming");
        List<ReservationDto> reservations = reservationService.getUpcomingReservations();
        return ResponseEntity.ok(reservations);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) throws CacheException, DataFactoryException {

        log.info("PATCH /api/reservations/{}/status - new status: {}", id, request.getNewStatus());
        ReservationDto reservation = reservationService.updateStatus(id, request);
        return ResponseEntity.ok(reservation);
    }

    @PatchMapping("/{id}/assign-table")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationDto> assignTable(
            @PathVariable Long id,
            @Valid @RequestBody AssignTableRequest request) throws CacheException, DataFactoryException {

        log.info("PATCH /api/reservations/{}/assign-table - table: {}", id, request.getTableId());
        ReservationDto reservation = reservationService.assignTable(id, request);
        return ResponseEntity.ok(reservation);
    }

    // ========== HELPER METHODS ==========

    private String extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return principal.getUserId();
        }
        throw new IllegalStateException("User ID not found in authentication");
    }
}