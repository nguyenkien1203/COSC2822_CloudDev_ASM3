package com.restaurant.reservationservice.repository;

import com.restaurant.reservationservice.entity.ReservationEntity;
import com.restaurant.reservationservice.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
                extends JpaRepository<ReservationEntity, Long>, JpaSpecificationExecutor<ReservationEntity> {

        // Find by confirmation code
        Optional<ReservationEntity> findByConfirmationCode(String confirmationCode);

        // Find by user
        List<ReservationEntity> findByUserIdOrderByReservationDateDescStartTimeDesc(String userId);

        // Find by user with status filter
        List<ReservationEntity> findByUserIdAndStatusInOrderByReservationDateDesc(
                        String userId, List<ReservationStatus> statuses);

        // Find today's reservations
        @Query("SELECT r FROM ReservationEntity r WHERE r.reservationDate = :date " +
                        "AND r.status NOT IN :excludedStatuses ORDER BY r.startTime")
        List<ReservationEntity> findTodayReservations(
                        @Param("date") LocalDate date,
                        @Param("excludedStatuses") List<ReservationStatus> excludedStatuses);

        // Find upcoming reservations
        @Query("SELECT r FROM ReservationEntity r WHERE r.reservationDate >= :date " +
                        "AND r.status NOT IN :excludedStatuses " +
                        "ORDER BY r.reservationDate, r.startTime")
        List<ReservationEntity> findUpcomingReservations(
                        @Param("date") LocalDate date,
                        @Param("excludedStatuses") List<ReservationStatus> excludedStatuses);

        // Check for conflicting reservations on a table
        @Query("SELECT r FROM ReservationEntity r WHERE r.table.id = :tableId " +
                        "AND r.reservationDate = :date " +
                        "AND r.status NOT IN :excludedStatuses " +
                        "AND ((r.startTime < :endTime AND r.endTime > :startTime))")
        List<ReservationEntity> findConflictingReservations(
                        @Param("tableId") Long tableId,
                        @Param("date") LocalDate date,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime,
                        @Param("excludedStatuses") List<ReservationStatus> excludedStatuses);

        // Find reservations needing reminder (24 hours before)
        @Query("SELECT r FROM ReservationEntity r WHERE r.reservationDate = :reminderDate " +
                        "AND r.reminderSent = false AND r.status = :status")
        List<ReservationEntity> findReservationsNeedingReminder(
                        @Param("reminderDate") LocalDate reminderDate,
                        @Param("status") ReservationStatus status);

        // Check if confirmation code exists
        boolean existsByConfirmationCode(String confirmationCode);

        // Count reservations by status for date
        Long countByReservationDateAndStatus(LocalDate date, ReservationStatus status);
}
