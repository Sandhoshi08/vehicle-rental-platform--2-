package com.rentalapp.repository;

import com.rentalapp.model.Booking;
import com.rentalapp.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRenterIdOrderByCreatedAtDesc(Long renterId);

    List<Booking> findByVehicle_Owner_IdOrderByCreatedAtDesc(Long ownerId);

    List<Booking> findByVehicleIdAndStatusIn(Long vehicleId, List<BookingStatus> statuses);

    // Two date ranges [start1,end1] and [start2,end2] overlap iff
    // start1 <= end2 AND end1 >= start2. Only PENDING/CONFIRMED bookings
    // count as "blocking" a date range.
    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Booking> findOverlappingBookings(@Param("vehicleId") Long vehicleId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    List<Booking> findByVehicle_Owner_IdAndStatusIn(Long ownerId, List<BookingStatus> statuses);
}
