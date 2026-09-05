package com.rentalapp.config;

import com.rentalapp.model.Role;
import com.rentalapp.model.User;
import com.rentalapp.model.Vehicle;
import com.rentalapp.model.VehicleStatus;
import com.rentalapp.repository.UserRepository;
import com.rentalapp.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded
        }

        User owner = userRepository.save(User.builder()
                .fullName("Priya Owner")
                .email("owner@demo.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.OWNER, Role.RENTER))
                .build());

        User renter = userRepository.save(User.builder()
                .fullName("Raj Renter")
                .email("renter@demo.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.RENTER))
                .build());

        vehicleRepository.save(Vehicle.builder()
                .owner(owner)
                .make("Toyota")
                .model("Corolla")
                .year(2022)
                .licensePlate("TN-01-AB-1234")
                .location("Chennai")
                .description("Fuel-efficient sedan, great for city driving.")
                .dailyRate(new BigDecimal("1500"))
                .status(VehicleStatus.ACTIVE)
                .build());

        vehicleRepository.save(Vehicle.builder()
                .owner(owner)
                .make("Mahindra")
                .model("Thar")
                .year(2023)
                .licensePlate("TN-02-CD-5678")
                .location("Madurai")
                .description("4x4 SUV, perfect for weekend off-road trips.")
                .dailyRate(new BigDecimal("3200"))
                .status(VehicleStatus.ACTIVE)
                .build());

        vehicleRepository.save(Vehicle.builder()
                .owner(owner)
                .make("Honda")
                .model("Activa")
                .year(2021)
                .licensePlate("TN-03-EF-9012")
                .location("Chennai")
                .description("Reliable scooter for quick errands around town.")
                .dailyRate(new BigDecimal("400"))
                .status(VehicleStatus.ACTIVE)
                .build());

        System.out.println("=======================================================");
        System.out.println(" Demo accounts seeded:");
        System.out.println("  Owner  -> owner@demo.com  / password123");
        System.out.println("  Renter -> renter@demo.com / password123");
        System.out.println("=======================================================");
    }
}
