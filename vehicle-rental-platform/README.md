# ROADSHARE — Peer-to-Peer Vehicle Rental Platform

A full-stack web app where vehicle owners list cars/bikes for rent during their available periods, and renters browse, book, and manage rentals. Built with Java, Spring Boot, Spring Security (JWT), Spring Data JPA, H2, and a vanilla HTML/CSS/JS frontend.

## Features

- **Auth & RBAC** — JWT-based login/register. A user can hold `OWNER` and/or `RENTER` roles; endpoints are protected with `@PreAuthorize` accordingly.
- **Vehicle listings** — owners create, edit, and delete listings; can mark a vehicle `ACTIVE`/`INACTIVE` to temporarily pull it off the market without losing booking history.
- **Date-based availability & booking conflict prevention** — booking creation runs inside a transaction that takes a **pessimistic row lock** on the vehicle (`SELECT ... FOR UPDATE`) before checking for overlapping `PENDING`/`CONFIRMED` bookings. This closes the race condition where two renters could otherwise both pass the "is it free?" check for the same dates and double-book the same vehicle.
- **Booking workflow** — renter requests a booking (`PENDING`) → owner confirms or rejects → owner marks `COMPLETED` after the rental period ends. Either party can cancel while still pending/confirmed.
- **Owner earnings tracking** — aggregated revenue from confirmed/completed bookings, broken down per vehicle.
- **Booking history** — separate views for "my bookings" (renter) and "incoming requests" (owner).

## Tech stack

Java 21, Spring Boot 3.3, Spring Security 6 (JWT via `jjwt`), Spring Data JPA / Hibernate, H2 (file-based), Maven, HTML/CSS/JavaScript (no framework, no build step).

## Project structure

```
src/main/java/com/rentalapp/
  config/         SecurityConfig, DataSeeder (demo data)
  security/       JwtUtil, JwtAuthFilter, CustomUserDetailsService
  model/          User, Vehicle, Booking + enums (Role, VehicleStatus, BookingStatus)
  repository/     Spring Data JPA repositories (incl. the overlap-detection query)
  dto/            Request/response DTOs, kept separate from JPA entities
  service/        Business logic (conflict prevention lives in BookingService)
  controller/     REST controllers
  exception/      ApiException + @RestControllerAdvice global handler
src/main/resources/
  application.properties
  static/         index.html, login.html, register.html, vehicle.html,
                   owner.html, renter.html, css/, js/
```

## Running it locally

**Requirements:** Java 21+ and Maven (or use the included wrapper if you add one).

```bash
cd vehicle-rental-platform
mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser.

On first run, `DataSeeder` populates two demo accounts and a few vehicles:

| Role   | Email            | Password    |
|--------|------------------|-------------|
| Owner  | owner@demo.com   | password123 |
| Renter | renter@demo.com  | password123 |

The H2 database is a local file (`./data/rentaldb.mv.db`) — data persists between restarts. You can inspect it directly at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/rentaldb`, user `sa`, no password).

## API overview

| Method | Endpoint                          | Access        | Description |
|--------|------------------------------------|---------------|-------------|
| POST   | `/api/auth/register`               | Public        | Create account |
| POST   | `/api/auth/login`                  | Public        | Get a JWT |
| GET    | `/api/vehicles`                    | Public        | Search/browse (filters: make, location, maxPrice, startDate, endDate) |
| GET    | `/api/vehicles/{id}`               | Public        | Vehicle details |
| GET    | `/api/vehicles/{id}/unavailable-dates` | Public    | Booked date ranges |
| GET    | `/api/vehicles/mine`               | OWNER         | My listings |
| POST   | `/api/vehicles`                    | OWNER         | Create listing |
| PUT    | `/api/vehicles/{id}`               | OWNER (owns)  | Edit listing |
| PATCH  | `/api/vehicles/{id}/status`        | OWNER (owns)  | Toggle ACTIVE/INACTIVE |
| DELETE | `/api/vehicles/{id}`               | OWNER (owns)  | Delete (blocked if active bookings exist) |
| POST   | `/api/bookings`                    | RENTER        | Request a booking |
| GET    | `/api/bookings/mine`               | RENTER        | My bookings |
| GET    | `/api/bookings/incoming`           | OWNER         | Requests for my vehicles |
| PATCH  | `/api/bookings/{id}/confirm`       | OWNER (owns)  | Approve a pending request |
| PATCH  | `/api/bookings/{id}/reject`        | OWNER (owns)  | Reject a pending request |
| PATCH  | `/api/bookings/{id}/complete`      | OWNER (owns)  | Mark a finished rental complete |
| PATCH  | `/api/bookings/{id}/cancel`        | Renter or owner on that booking | Cancel |
| GET    | `/api/earnings/mine`               | OWNER         | Revenue summary |

## Notes on the interesting parts (useful for interviews)

- **Concurrency-safe booking:** `VehicleRepository.findByIdForUpdate` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` inside `BookingService.createBooking`'s `@Transactional` method, so the overlap check and the insert are atomic with respect to other booking attempts on the same vehicle.
- **Overlap detection:** two date ranges `[s1,e1]` and `[s2,e2]` overlap iff `s1 <= e2 AND e1 >= s2` — see `BookingRepository.findOverlappingBookings`.
- **RBAC:** roles are stored as a `Set<Role>` per user (not a single field), so one account can be both an owner and a renter, and endpoints are locked down per-role with `@PreAuthorize("hasRole('OWNER')")` / `hasRole('RENTER')`.
- **Stateless auth:** JWT is validated on every request via a custom `OncePerRequestFilter` (`JwtAuthFilter`); no server-side session state.

## Possible extensions

- Real payment integration (Stripe) instead of computed totals
- Owner-configurable cancellation policies
- Image uploads for vehicle listings
- Review/rating system affecting search ranking
- Email/SMS notifications on booking state changes
