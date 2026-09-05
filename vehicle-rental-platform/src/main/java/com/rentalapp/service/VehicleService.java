package com.rentalapp.service;

import com.rentalapp.dto.BookingDtos.UnavailableRange;
import com.rentalapp.dto.VehicleDtos.VehicleRequest;
import com.rentalapp.dto.VehicleDtos.VehicleResponse;
import com.rentalapp.exception.ApiException;
import com.rentalapp.model.Booking;
import com.rentalapp.model.BookingStatus;
import com.rentalapp.model.User;
import com.rentalapp.model.Vehicle;
import com.rentalapp.model.VehicleStatus;
import com.rentalapp.repository.BookingRepository;
import com.rentalapp.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserResolver currentUserResolver;

    public VehicleResponse createVehicle(VehicleRequest request, String ownerEmail) {
        User owner = currentUserResolver.resolve(ownerEmail);

        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new ApiException(HttpStatus.CONFLICT, "A vehicle with this license plate is already listed");
        }

        Vehicle vehicle = Vehicle.builder()
                .owner(owner)
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .licensePlate(request.getLicensePlate())
                .location(request.getLocation())
                .description(request.getDescription())
                .dailyRate(request.getDailyRate())
                .status(VehicleStatus.ACTIVE)
                .build();

        return toResponse(vehicleRepository.save(vehicle));
    }

    public List<VehicleResponse> search(String make, String location, BigDecimal maxPrice,
                                         LocalDate startDate, LocalDate endDate) {
        List<Vehicle> vehicles = vehicleRepository.search(
                blankToNull(make), blankToNull(location), maxPrice);

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
            }
            vehicles = vehicles.stream()
                    .filter(v -> bookingRepository.findOverlappingBookings(v.getId(), startDate, endDate).isEmpty())
                    .toList();
        }

        return vehicles.stream().map(this::toResponse).toList();
    }

    public VehicleResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<VehicleResponse> getMyVehicles(String ownerEmail) {
        User owner = currentUserResolver.resolve(ownerEmail);
        return vehicleRepository.findByOwnerId(owner.getId()).stream().map(this::toResponse).toList();
    }

    public VehicleResponse updateVehicle(Long id, VehicleRequest request, String ownerEmail) {
        Vehicle vehicle = findOrThrow(id);
        assertOwnership(vehicle, ownerEmail);

        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setLocation(request.getLocation());
        vehicle.setDescription(request.getDescription());
        vehicle.setDailyRate(request.getDailyRate());
        // license plate intentionally not editable post-creation to avoid identity confusion

        return toResponse(vehicleRepository.save(vehicle));
    }

    /**
     * Owners can temporarily take a vehicle off the market (INACTIVE) or
     * relist it (ACTIVE) without deleting its booking history.
     */
    public VehicleResponse updateStatus(Long id, VehicleStatus status, String ownerEmail) {
        Vehicle vehicle = findOrThrow(id);
        assertOwnership(vehicle, ownerEmail);
        vehicle.setStatus(status);
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long id, String ownerEmail) {
        Vehicle vehicle = findOrThrow(id);
        assertOwnership(vehicle, ownerEmail);

        boolean hasActiveBookings = !bookingRepository
                .findByVehicleIdAndStatusIn(id, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .isEmpty();
        if (hasActiveBookings) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot delete a vehicle with pending or confirmed bookings. Mark it inactive instead.");
        }

        vehicleRepository.delete(vehicle);
    }

    public List<UnavailableRange> getUnavailableDates(Long vehicleId) {
        findOrThrow(vehicleId);
        List<Booking> blocking = bookingRepository.findByVehicleIdAndStatusIn(
                vehicleId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

        return blocking.stream()
                .map(b -> UnavailableRange.builder().startDate(b.getStartDate()).endDate(b.getEndDate()).build())
                .toList();
    }

    private Vehicle findOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    private void assertOwnership(Vehicle vehicle, String requesterEmail) {
        if (!vehicle.getOwner().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this vehicle");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private VehicleResponse toResponse(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .ownerId(v.getOwner().getId())
                .ownerName(v.getOwner().getFullName())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .licensePlate(v.getLicensePlate())
                .location(v.getLocation())
                .description(v.getDescription())
                .dailyRate(v.getDailyRate())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
