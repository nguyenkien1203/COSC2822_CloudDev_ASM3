// TableController.java
package com.restaurant.reservationservice.controller;

import com.restaurant.factorymodule.exception.DataFactoryException;
import com.restaurant.redismodule.exception.CacheException;
import com.restaurant.reservationservice.dto.*;
import com.restaurant.reservationservice.enums.TableStatus;
import com.restaurant.reservationservice.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TableController {

    private final TableService tableService;

    @GetMapping("/all")
    public ResponseEntity<List<TableDto>> getAllTables() throws CacheException, DataFactoryException {
        log.info("GET /api/tables");
        List<TableDto> tables = tableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableDto> getTableById(@PathVariable Long id)
            throws CacheException, DataFactoryException {
        log.info("GET /api/tables/{}", id);
        TableDto table = tableService.getTableById(id);
        return ResponseEntity.ok(table);
    }

    @PostMapping
    public ResponseEntity<TableDto> createTable(
            @Valid @RequestBody CreateTableRequest request) throws DataFactoryException {
        log.info("POST /api/tables - Creating table: {}", request.getTableNumber());
        TableDto table = tableService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(table);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TableDto> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableRequest request) throws CacheException, DataFactoryException {
        log.info("PUT /api/tables/{}", id);
        TableDto table = tableService.updateTable(id, request);
        return ResponseEntity.ok(table);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) throws DataFactoryException {
        log.info("DELETE /api/tables/{}", id);
        tableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TableDto> updateTableStatus(
            @PathVariable Long id,
            @RequestParam TableStatus status) throws CacheException, DataFactoryException {
        log.info("PATCH /api/tables/{}/status - {}", id, status);
        TableDto table = tableService.updateTableStatus(id, status);
        return ResponseEntity.ok(table);
    }

    @GetMapping("/available")
    public ResponseEntity<List<TableDto>> getAvailableTables(
            @RequestParam LocalDate date,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime,
            @RequestParam Integer partySize) {
        log.info("GET /api/tables/available - {} {} to {} for {}", date, startTime, endTime, partySize);
        List<TableDto> tables = tableService.getAvailableTablesForSlot(date, startTime, endTime, partySize);
        return ResponseEntity.ok(tables);
    }
}