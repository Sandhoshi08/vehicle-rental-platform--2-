package com.rentalapp.controller;

import com.rentalapp.dto.BookingDtos.UnavailableRange;
import com.rentalapp.dto.VehicleDtos.StatusUpdateRequest;
import com.rentalapp.dto.VehicleDtos.VehicleRequest;
import com.rentalapp.dto.VehicleDtos.VehicleResponse;
import com.rentalapp.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> search(
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(vehicleService.search(make, location, maxPrice, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getById(id));
    }

    @GetMapping("/{id}/unavailable-dates")
    public ResponseEntity<List<UnavailableRange>> unavailableDates(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getUnavailableDates(id));
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/mine")
    public ResponseEntity<List<VehicleResponse>> myVehicles(Authentication auth) {
        return ResponseEntity.ok(vehicleService.getMyVehicles(auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.createVehicle(request, auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody VehicleRequest request,
                                                    Authentication auth) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request, auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody StatusUpdateRequest request,
                                                          Authentication auth) {
        return ResponseEntity.ok(vehicleService.updateStatus(id, request.getStatus(), auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        vehicleService.deleteVehicle(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
