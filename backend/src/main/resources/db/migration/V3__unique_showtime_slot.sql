-- A movie can have only one showtime per screen at a given start time.
-- This keeps rolling demo-schedule seeding idempotent across application restarts.
ALTER TABLE showtimes
  ADD CONSTRAINT uk_showtimes_movie_start_screen UNIQUE (movie_id, starts_at, screen_name);
