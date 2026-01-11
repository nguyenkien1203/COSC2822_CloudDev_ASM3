package com.restaurant.reservationservice.service.impl;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.redismodule.exception.CacheException;
import com.restaurant.reservationservice.dto.*;
import com.restaurant.reservationservice.enums.ReservationStatus;
import com.restaurant.reservationservice.enums.TableStatus;
import com.restaurant.reservationservice.exception.InvalidReservationTimeException;
import com.restaurant.reservationservice.exception.TableNotAvailableException;
import com.restaurant.reservationservice.exception.UnauthorizedReservationAccessException;
import com.restaurant.reservationservice.factory.ReservationFactory;
import com.restaurant.reservationservice.filter.ReservationFilter;
import com.restaurant.reservationservice.service.AvailabilityService;
import com.restaurant.reservationservice.service.ReservationProducerService;
import com.restaurant.reservationservice.service.ReservationService;
import com.restaurant.reservationservice.service.TableService;
import com.restaurant.reservationservice.utils.ConfirmationCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    // Default reservation duration in minutes
    private static final int DEFAULT_DURATION_MINUTES = 120;
    private final ReservationFactory reservationFactory;
    private final ReservationProducerService reservationProducerService;
    private final AvailabilityService availabilityService;
    private final ConfirmationCodeGenerator confirmationCodeGenerator;
    private final TableService tableService;

    @Override
    @Transactional
    public ReservationDto createReservation(CreateReservationRequest request, String userId)
            throws DataFactoryException {
        log.info("Creating reservation for user: {} on {}", userId, request.getReservationDate());

        validateReservationRequest(request);

        // Find best available table
        LocalTime endTime = request.getStartTime().plusMinutes(DEFAULT_DURATION_MINUTES);

        TableDto table = availabilityService.findBestTable(
                request.getReservationDate(),
                request.getStartTime(),
                endTime,
                request.getPartySize());

        if (table == null) {
            throw new TableNotAvailableException("No tables available for the requested time slot");
        }

        String confirmationCode = confirmationCodeGenerator.generate();

        // Build reservation
        ReservationDto reservation = ReservationDto.builder()
                .confirmationCode(confirmationCode)
                .userId(userId)
                .table(table)
                .partySize(request.getPartySize())
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(ReservationStatus.PENDING)
                .specialRequests(request.getSpecialRequests())
                .build();

        ReservationDto created = reservationFactory.create(reservation);

        // Publish event
        // producerService.publishReservationCreatedEvent(created);

        return created;
    }

    @Override
    @Transactional
    public ReservationDto createGuestReservation(CreateReservationRequest request) throws DataFactoryException {
        log.info("Creating reservation for guest on {}", request.getReservationDate());

        validateReservationRequest(request);

        // Find best available table
        LocalTime endTime = request.getStartTime().plusMinutes(DEFAULT_DURATION_MINUTES);

        TableDto table = availabilityService.findBestTable(
                request.getReservationDate(),
                request.getStartTime(),
                endTime,
                request.getPartySize());

        if (table == null) {
            throw new TableNotAvailableException("No tables available for the requested time slot");
        }

        String confirmationCode = confirmationCodeGenerator.generate();

        // Build reservation
        ReservationDto reservation = ReservationDto.builder()
                .confirmationCode(confirmationCode)
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .table(table)
                .partySize(request.getPartySize())
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(ReservationStatus.PENDING)
                .specialRequests(request.getSpecialRequests())
                .build();

        ReservationDto created = reservationFactory.create(reservation);

        // Publish event
        // producerService.publishReservationCreatedEvent(created);

        return created;
    }

    @Override
    @Transactional
    public ReservationDto getReservationById(Long id) throws CacheException, DataFactoryException {
        log.info("Getting reservation by id: {}", id);
        return reservationFactory.getModel(id, null);
    }

    @Override
    @Transactional
    public ReservationDto getByConfirmationCode(String code) throws CacheException, DataFactoryException {
        log.info("Getting reservation by confirmation code: {}", code);
        return reservationFactory.findByConfirmationCode(code);
    }

    @Override
    @Transactional
    public List<ReservationDto> getMyReservations(String userId) throws CacheException, DataFactoryException {
        log.info("Getting reservations for user: {}", userId);
        ReservationFilter filter = ReservationFilter.builder().userId(userId).build();
        return reservationFactory.getList(filter);
    }

    @Override
    @Transactional
    public ReservationDto updateReservation(Long id, UpdateReservationRequest request, String userId)
            throws CacheException, DataFactoryException {
        log.info("Updating reservation: {} by user: {}", id, userId);

        ReservationDto existing = reservationFactory.getModel(id, null);

        // Validate ownership
        if (!existing.getUserId().equals(userId)) {
            throw new UnauthorizedReservationAccessException("You don't have permission to update this reservation");
        }

        // Can only update PENDING or CONFIRMED reservations
        if (existing.getStatus() != ReservationStatus.PENDING &&
                existing.getStatus() != ReservationStatus.CONFIRMED) {
            throw new DataFactoryException("Cannot update reservation with status: " + existing.getStatus());
        }

        TableDto availableTable = availabilityService.findBestTable(request.getReservationDate(),
                request.getStartTime(), request.getStartTime().plusMinutes(DEFAULT_DURATION_MINUTES),
                request.getPartySize());

        if (availableTable == null) {

            throw new InvalidReservationTimeException("The time slot has been occupied");
        } else {
            log.info(String.valueOf(availableTable));
            // Update fields
            if (request.getReservationDate() != null) {
                existing.setReservationDate(request.getReservationDate());
            }
            if (request.getStartTime() != null) {
                existing.setStartTime(request.getStartTime());
                existing.setEndTime(request.getStartTime().plusMinutes(DEFAULT_DURATION_MINUTES));
            }
            if (request.getPartySize() != null) {
                existing.setPartySize(request.getPartySize());
            }
            if (request.getSpecialRequests() != null) {
                existing.setSpecialRequests(request.getSpecialRequests());
            }

            existing.setTable(availableTable);
        }

        return reservationFactory.update(existing, null);
    }

    @Override
    @Transactional
    public void cancelReservation(Long id, String userId) throws CacheException, DataFactoryException {
        log.info("Cancelling reservation: {} by user: {}", id, userId);

        ReservationDto reservation = reservationFactory.getModel(id, null);

        // Validate ownership
        if (!reservation.getUserId().equals(userId)) {
            throw new UnauthorizedReservationAccessException("You don't have permission to cancel this reservation");
        }

        // Can only cancel PENDING or CONFIRMED reservations
        if (reservation.getStatus() != ReservationStatus.PENDING &&
                reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new DataFactoryException("Cannot cancel reservation with status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationFactory.update(reservation, null);

        // Note: We don't change table status here because canceling a future
        // reservation
        // doesn't affect the current physical state of the table

        // Publish cancellation event for order service to cancel pre-orders
        reservationProducerService.publishReservationCancelledEvent(reservation, "Cancelled by customer");
    }

    @Override
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        return availabilityService.checkAvailability(request);
    }

    @Override
    @Transactional
    public List<ReservationDto> getAllReservations(ReservationFilter filter)
            throws CacheException, DataFactoryException {
        log.info("Getting all reservations with filter");
        return reservationFactory.getList(filter);
    }

    @Override
    @Transactional
    public List<ReservationDto> getTodayReservations() {
        log.info("Getting today's reservations");
        return reservationFactory.getTodayReservations();
    }

    @Override
    @Transactional
    public List<ReservationDto> getUpcomingReservations() {
        log.info("Getting upcoming reservations");
        return reservationFactory.getUpcomingReservations();
    }

    @Override
    @Transactional
    public ReservationDto updateStatus(Long id, UpdateStatusRequest request)
            throws CacheException, DataFactoryException {
        log.info("Updating reservation {} status to {}", id, request.getNewStatus());

        ReservationDto reservation = reservationFactory.getModel(id, null);
        ReservationStatus oldStatus = reservation.getStatus();

        validateStatusTransition(oldStatus, request.getNewStatus());

        reservation.setStatus(request.getNewStatus());
        ReservationDto updated = reservationFactory.update(reservation, null);

        // Publish status change events for order service integration
        // Only update table status for real-time physical state changes:
        // - SEATED: customer physically arrives and sits down
        // - COMPLETED: customer physically leaves the table
        if (request.getNewStatus() == ReservationStatus.SEATED) {
            reservationProducerService.publishCustomerSeatedEvent(updated);
            if (updated.getTable() != null) {
                tableService.updateTableStatus(updated.getTable().getId(), TableStatus.OCCUPIED);
            }
        } else if (request.getNewStatus() == ReservationStatus.COMPLETED) {
            reservationProducerService.publishReservationCompletedEvent(updated);
            if (updated.getTable() != null) {
                tableService.updateTableStatus(updated.getTable().getId(), TableStatus.AVAILABLE);
            }
        } else if (request.getNewStatus() == ReservationStatus.CANCELLED) {
            reservationProducerService.publishReservationCancelledEvent(updated, "Cancelled by admin");
            // No table status change - canceling doesn't affect physical state
        }
        // NO_SHOW: No table status change needed - table was never occupied

        return updated;
    }

    @Override
    @Transactional
    public ReservationDto assignTable(Long reservationId, AssignTableRequest request)
            throws CacheException, DataFactoryException {
        log.info("Assigning table {} to reservation {}", request.getTableId(), reservationId);

        ReservationDto reservation = reservationFactory.getModel(reservationId, null);

        // Verify table is available
        boolean available = availabilityService.isTableAvailable(
                request.getTableId(),
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getEndTime());

        if (!available) {
            throw new TableNotAvailableException("Table is not available for the reservation time");
        }

        // Update table assignment
        TableDto table = TableDto.builder().id(request.getTableId()).build();
        reservation.setTable(table);

        return reservationFactory.update(reservation, null);
    }

    @Override
    @Transactional
    public void linkPreOrder(Long reservationId, Long preOrderId) throws CacheException, DataFactoryException {
        log.info("Linking pre-order {} to reservation {}", preOrderId, reservationId);

        ReservationDto reservation = reservationFactory.getModel(reservationId, null);
        reservation.setPreOrderId(preOrderId);
        reservationFactory.update(reservation, null);
    }

    /* ===================HELPER METHOD ====================== */

    private void validateReservationRequest(CreateReservationRequest request) throws DataFactoryException {
        // Add validation logic
        if (request.getPartySize() == null || request.getPartySize() < 1) {
            throw new DataFactoryException("Party size must be at least 1");
        }
        if (request.getReservationDate() == null) {
            throw new DataFactoryException("Reservation date is required");
        }
        if (request.getStartTime() == null) {
            throw new DataFactoryException("Start time is required");
        }
    }

    private void validateGuestReservationRequest(CreateReservationRequest request) throws DataFactoryException {
        validateReservationRequest(request);

        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new DataFactoryException("Guest name is required");
        }
        if (request.getGuestEmail() == null || request.getGuestEmail().isBlank()) {
            throw new DataFactoryException("Guest email is required");
        }
        if (request.getGuestPhone() == null || request.getGuestPhone().isBlank()) {
            throw new DataFactoryException("Guest phone is required");
        }
    }

    private void validateStatusTransition(ReservationStatus current, ReservationStatus next)
            throws DataFactoryException {

        boolean valid = switch (current) {
            case PENDING -> next == ReservationStatus.CONFIRMED || next == ReservationStatus.CANCELLED;
            case CONFIRMED ->
                next == ReservationStatus.SEATED || next == ReservationStatus.CANCELLED
                        || next == ReservationStatus.NO_SHOW;
            case SEATED -> next == ReservationStatus.COMPLETED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
        if (!valid) {
            throw new DataFactoryException("Invalid status transition from " + current + " to " + next);
        }
    }
}
