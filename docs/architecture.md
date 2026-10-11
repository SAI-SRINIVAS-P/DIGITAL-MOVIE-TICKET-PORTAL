# Architecture notes

## Request path

```text
React + TypeScript browser
       │ JSON / Bearer JWT
       ▼
Nginx (Docker frontend) ────── serves SPA
       │ /api/*                         │ /v3/api-docs, Swagger UI
       ▼                                ▼
Spring Boot REST API ─────── springdoc OpenAPI
       │ Spring Security JWT filter
       ▼
Application services
  ├── CatalogService (read models)
  ├── AuthService (BCrypt + JWT)
  └── BookingService (@Transactional)
       │ Spring Data JPA / Hibernate
       ▼
MySQL 8.4 / InnoDB
  ├── Flyway-owned schema
  ├── show_seats row locks
  ├── foreign keys + unique constraints
  └── booking + booking_seats audit snapshot
```

## Booking state model

```text
AVAILABLE ── hold transaction ──▶ HELD
   ▲                                │
   │ expired lease reclaimed        │ confirm before expiry
   └────────────────────────────────┤
                                    ▼
                                  BOOKED
```

A hold consists of several `show_seats` rows sharing one UUID token, one user ID, one expiry timestamp, and one expected-seat count (`hold_size`). If a multi-seat hold fails, the whole transaction rolls back rather than leaving a partial hold. Confirmation only accepts the complete set of seats for a live token owned by the authenticated user; if a hold is incomplete or expired, the transaction is rejected.

## Important invariants

- `(movie_id, starts_at, screen_name)` is unique, so the rolling sample seeder cannot create duplicate showtimes.
- `(showtime_id, seat_label)` is unique.
- The demo seeder is idempotent and keeps missing sample showtimes/80-seat maps filled through 14 days ahead; set `DEMO_SCHEDULE_ENABLED=false` when an external schedule manager owns showtime data.
- A seat can have one state at a time: `AVAILABLE`, `HELD`, or `BOOKED`.
- Every hold or confirmation reads the rows under write locks and modifies those rows within the same transaction.
- A booking stores price snapshots in `booking_seats`, so a later ticket-price change does not rewrite historical totals.
- JWT identity is the user's immutable database ID, not a client-supplied user ID in the request body.
