package com.rentalapp.repository;

import com.rentalapp.model.Vehicle;
import com.rentalapp.model.VehicleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerId(Long ownerId);

    List<Vehicle> findByStatus(VehicleStatus status);

    boolean existsByLicensePlate(String licensePlate);

    @Query("SELECT v FROM Vehicle v WHERE v.status = 'ACTIVE' AND " +
           "(:make IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :make, '%'))) AND " +
           "(:location IS NULL OR LOWER(v.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:maxPrice IS NULL OR v.dailyRate <= :maxPrice)")
    List<Vehicle> search(@Param("make") String make,
                          @Param("location") String location,
                          @Param("maxPrice") java.math.BigDecimal maxPrice);

    // Pessimistic write lock so two concurrent bookings can't both pass the
    // conflict check for the same vehicle at the same time (row-level lock
    // held for the duration of the surrounding @Transactional method).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);
}
