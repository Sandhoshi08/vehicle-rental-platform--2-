package com.rentalapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class EarningsDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleEarnings {
        private Long vehicleId;
        private String vehicleName;
        private String licensePlate;
        private BigDecimal totalEarnings;
        private long completedOrConfirmedBookings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EarningsSummary {
        private BigDecimal totalEarnings;
        private long totalBookings;
        private List<VehicleEarnings> byVehicle;
    }
}
