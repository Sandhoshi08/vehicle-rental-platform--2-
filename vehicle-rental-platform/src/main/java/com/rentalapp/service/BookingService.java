package com.rentalapp.service;

import com.rentalapp.dto.BookingDtos.BookingRequest;
import com.rentalapp.dto.BookingDtos.BookingResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserResolver currentUserResolver;

    /**
     * Creates a booking request (status PENDING, awaiting owner confirmation).
     *
     * Conflict prevention: we take a pessimistic write lock on the vehicle row
     * for the duration of this transaction (findByIdForUpdate), then check for
     * overlapping PENDING/CONFIRMED bookings. Holding the lock while we check
     * AND insert closes the race window where two renters could otherwise both
     * pass the overlap check for the same dates before either commit.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request, String renterEmail) {
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }

        User renter = currentUserResolver.resolve(renterEmail);

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(request.getVehicleId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "This vehicle is not currently available for rent");
        }

        if (vehicle.getOwner().getId().equals(renter.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot book your own vehicle");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                vehicle.getId(), request.getStartDate(), request.getEndDate());

        if (!overlaps.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This vehicle is already booked for part of the selected date range");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal totalPrice = vehicle.getDailyRate().multiply(BigDecimal.valueOf(days));

        Booking booking = Booking.builder()
                .vehicle(vehicle)
                .renter(renter)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING)
                .build();

        return toResponse(bookingRepository.save(booking));
    }

    public List<BookingResponse> getMyBookingsAsRenter(String renterEmail) {
        User renter = currentUserResolver.resolve(renterEmail);
        return bookingRepository.findByRenterIdOrderByCreatedAtDesc(renter.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<BookingResponse> getMyBookingsAsOwner(String ownerEmail) {
        User owner = currentUserResolver.resolve(ownerEmail);
        return bookingRepository.findByVehicle_Owner_IdOrderByCreatedAtDesc(owner.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String ownerEmail) {
        Booking booking = findOrThrow(bookingId);
        assertIsOwner(booking, ownerEmail);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only pending bookings can be confirmed");
        }

        // Re-check for conflicts in case another overlapping booking was
        // confirmed in the meantime (e.g. two pending requests for the same dates).
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                booking.getVehicle().getId(), booking.getStartDate(), booking.getEndDate());
        boolean conflictExists = overlaps.stream()
                .anyMatch(b -> !b.getId().equals(booking.getId()) && b.getStatus() == BookingStatus.CONFIRMED);
        if (conflictExists) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Another booking for overlapping dates was already confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    public BookingResponse rejectBooking(Long bookingId, String ownerEmail) {
        Booking booking = findOrThrow(bookingId);
        assertIsOwner(booking, ownerEmail);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only pending bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        return toResponse(bookingRepository.save(booking));
    }

    public BookingResponse cancelBooking(Long bookingId, String requesterEmail) {
        Booking booking = findOrThrow(bookingId);

        boolean isRenter = booking.getRenter().getEmail().equalsIgnoreCase(requesterEmail);
        boolean isOwner = booking.getVehicle().getOwner().getEmail().equalsIgnoreCase(requesterEmail);
        if (!isRenter && !isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "This booking can no longer be cancelled");
        }
        if (booking.getStartDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot cancel a booking that has already started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    public BookingResponse completeBooking(Long bookingId, String ownerEmail) {
        Booking booking = findOrThrow(bookingId);
        assertIsOwner(booking, ownerEmail);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only confirmed bookings can be marked complete");
        }
        if (booking.getEndDate().isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "This rental period hasn't ended yet");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        return toResponse(bookingRepository.save(booking));
    }

    private Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private void assertIsOwner(Booking booking, String requesterEmail) {
        if (!booking.getVehicle().getOwner().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own the vehicle for this booking");
        }
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .vehicleId(b.getVehicle().getId())
                .vehicleMake(b.getVehicle().getMake())
                .vehicleModel(b.getVehicle().getModel())
                .vehicleLicensePlate(b.getVehicle().getLicensePlate())
                .renterId(b.getRenter().getId())
                .renterName(b.getRenter().getFullName())
                .ownerId(b.getVehicle().getOwner().getId())
                .ownerName(b.getVehicle().getOwner().getFullName())
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .totalPrice(b.getTotalPrice())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
