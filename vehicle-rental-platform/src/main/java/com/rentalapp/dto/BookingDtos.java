package com.rentalapp.dto;

import com.rentalapp.model.BookingStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingRequest {
        @NotNull
        private Long vehicleId;
        @NotNull @FutureOrPresent
        private LocalDate startDate;
        @NotNull @FutureOrPresent
        private LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingResponse {
        private Long id;
        private Long vehicleId;
        private String vehicleMake;
        private String vehicleModel;
        private String vehicleLicensePlate;
        private Long renterId;
        private String renterName;
        private Long ownerId;
        private String ownerName;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalPrice;
        private BookingStatus status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnavailableRange {
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
