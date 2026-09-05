package com.rentalapp.service;

import com.rentalapp.dto.EarningsDtos.EarningsSummary;
import com.rentalapp.dto.EarningsDtos.VehicleEarnings;
import com.rentalapp.model.Booking;
import com.rentalapp.model.BookingStatus;
import com.rentalapp.model.User;
import com.rentalapp.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EarningsService {

    private final BookingRepository bookingRepository;
    private final CurrentUserResolver currentUserResolver;

    public EarningsSummary getOwnerEarnings(String ownerEmail) {
        User owner = currentUserResolver.resolve(ownerEmail);

        // Confirmed + completed bookings count as revenue; pending/rejected/cancelled don't.
        List<Booking> revenueBookings = bookingRepository.findByVehicle_Owner_IdAndStatusIn(
                owner.getId(), List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

        Map<Long, VehicleEarnings> byVehicle = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Booking b : revenueBookings) {
            total = total.add(b.getTotalPrice());
            Long vehicleId = b.getVehicle().getId();

            VehicleEarnings existing = byVehicle.get(vehicleId);
            if (existing == null) {
                byVehicle.put(vehicleId, VehicleEarnings.builder()
                        .vehicleId(vehicleId)
                        .vehicleName(b.getVehicle().getMake() + " " + b.getVehicle().getModel())
                        .licensePlate(b.getVehicle().getLicensePlate())
                        .totalEarnings(b.getTotalPrice())
                        .completedOrConfirmedBookings(1)
                        .build());
            } else {
                existing.setTotalEarnings(existing.getTotalEarnings().add(b.getTotalPrice()));
                existing.setCompletedOrConfirmedBookings(existing.getCompletedOrConfirmedBookings() + 1);
            }
        }

        List<VehicleEarnings> sorted = byVehicle.values().stream()
                .sorted(Comparator.comparing(VehicleEarnings::getTotalEarnings).reversed())
                .toList();

        return EarningsSummary.builder()
                .totalEarnings(total)
                .totalBookings(revenueBookings.size())
                .byVehicle(sorted)
                .build();
    }
}
