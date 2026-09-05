package com.rentalapp.dto;

import com.rentalapp.model.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VehicleDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleRequest {
        @NotBlank
        private String make;
        @NotBlank
        private String model;
        @NotNull
        private Integer year;
        @NotBlank
        private String licensePlate;
        private String location;
        private String description;
        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal dailyRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleResponse {
        private Long id;
        private Long ownerId;
        private String ownerName;
        private String make;
        private String model;
        private Integer year;
        private String licensePlate;
        private String location;
        private String description;
        private BigDecimal dailyRate;
        private VehicleStatus status;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        @NotNull
        private VehicleStatus status;
    }
}
