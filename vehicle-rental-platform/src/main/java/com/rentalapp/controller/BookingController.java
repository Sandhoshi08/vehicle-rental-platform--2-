package com.rentalapp.controller;

import com.rentalapp.dto.BookingDtos.BookingRequest;
import com.rentalapp.dto.BookingDtos.BookingResponse;
import com.rentalapp.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PreAuthorize("hasRole('RENTER')")
    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request, auth.getName()));
    }

    @PreAuthorize("hasRole('RENTER')")
    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponse>> myBookings(Authentication auth) {
        return ResponseEntity.ok(bookingService.getMyBookingsAsRenter(auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/incoming")
    public ResponseEntity<List<BookingResponse>> incomingBookings(Authentication auth) {
        return ResponseEntity.ok(bookingService.getMyBookingsAsOwner(auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirm(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<BookingResponse> reject(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(bookingService.rejectBooking(id, auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> complete(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(bookingService.completeBooking(id, auth.getName()));
    }

    // Either party (renter or owner) can cancel while a booking is still pending/confirmed.
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, auth.getName()));
    }
}
