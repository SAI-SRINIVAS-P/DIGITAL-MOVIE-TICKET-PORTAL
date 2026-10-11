CREATE TABLE app_users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  display_name VARCHAR(100) NOT NULL,
  email VARCHAR(190) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_app_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE movies (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(140) NOT NULL,
  genre VARCHAR(80) NOT NULL,
  runtime_minutes INT NOT NULL,
  rating VARCHAR(12) NOT NULL,
  language VARCHAR(30) NOT NULL,
  synopsis VARCHAR(800) NOT NULL,
  poster_theme VARCHAR(40) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE showtimes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  movie_id BIGINT NOT NULL,
  starts_at DATETIME(6) NOT NULL,
  venue VARCHAR(120) NOT NULL,
  screen_name VARCHAR(30) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_showtimes_movie_start (movie_id, starts_at),
  CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES movies(id)
) ENGINE=InnoDB;

CREATE TABLE bookings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_code VARCHAR(24) NOT NULL,
  user_id BIGINT NOT NULL,
  showtime_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  booked_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_bookings_code UNIQUE (booking_code),
  KEY idx_bookings_user_booked (user_id, booked_at),
  CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES app_users(id),
  CONSTRAINT fk_bookings_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id)
) ENGINE=InnoDB;

CREATE TABLE show_seats (
  id BIGINT NOT NULL AUTO_INCREMENT,
  showtime_id BIGINT NOT NULL,
  seat_label VARCHAR(5) NOT NULL,
  row_label VARCHAR(1) NOT NULL,
  seat_number INT NOT NULL,
  price_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'AVAILABLE',
  hold_token VARCHAR(36) NULL,
  held_by_user_id BIGINT NULL,
  hold_expires_at DATETIME(6) NULL,
  hold_size INT NOT NULL DEFAULT 0,
  booking_id BIGINT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_show_seats_show_label UNIQUE (showtime_id, seat_label),
  KEY idx_show_seats_hold_token (hold_token),
  KEY idx_show_seats_show_status (showtime_id, status),
  CONSTRAINT fk_show_seats_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id),
  CONSTRAINT fk_show_seats_user FOREIGN KEY (held_by_user_id) REFERENCES app_users(id),
  CONSTRAINT fk_show_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB;

CREATE TABLE booking_seats (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  seat_label VARCHAR(5) NOT NULL,
  price_amount DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_booking_seat_label UNIQUE (booking_id, seat_label),
  CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB;
