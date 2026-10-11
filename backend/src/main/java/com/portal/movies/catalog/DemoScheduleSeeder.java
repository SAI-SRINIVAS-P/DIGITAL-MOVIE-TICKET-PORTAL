package com.portal.movies.catalog;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import com.portal.movies.repository.MovieRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Keeps the demo catalog usable for long-running local installations. Flyway creates the initial
 * sample data; this runner then fills any missing slots in a rolling 14-day window on each start.
 * Disable with DEMO_SCHEDULE_ENABLED=false when replacing demo scheduling with managed data.
 */
@Component
@ConditionalOnProperty(name = "app.demo-schedule.enabled", havingValue = "true", matchIfMissing = true)
public class DemoScheduleSeeder implements ApplicationRunner {
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DAYS_AHEAD = 14;
    private static final String VENUE = "Luma Cinemas · Hyderabad";
    private static final String INSERT_SHOWTIME = """
            INSERT IGNORE INTO showtimes (movie_id, starts_at, venue, screen_name)
            VALUES (?, ?, ?, ?)
            """;
    private static final String FIND_SHOWTIME = """
            SELECT id FROM showtimes
            WHERE movie_id = ? AND starts_at = ? AND screen_name = ?
            """;
    private static final String INSERT_SEAT = """
            INSERT IGNORE INTO show_seats
                (showtime_id, seat_label, row_label, seat_number, price_amount, status)
            VALUES (?, ?, ?, ?, ?, 'AVAILABLE')
            """;

    private static final List<Slot> SLOTS = List.of(
            new Slot("The Last Horizon", LocalTime.of(11, 30), "Screen 01"),
            new Slot("The Last Horizon", LocalTime.of(20, 45), "Screen 01"),
            new Slot("Monsoon Letters", LocalTime.of(13, 0), "Screen 03"),
            new Slot("Monsoon Letters", LocalTime.of(18, 20), "Screen 03"),
            new Slot("Neon Run", LocalTime.of(15, 15), "Screen 02"),
            new Slot("Neon Run", LocalTime.of(22, 0), "Screen 02")
    );

    private final JdbcTemplate jdbc;
    private final MovieRepository movies;

    public DemoScheduleSeeder(JdbcTemplate jdbc, MovieRepository movies) {
        this.jdbc = jdbc;
        this.movies = movies;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var movieIds = movies.findAllByOrderByTitleAsc().stream()
                .collect(Collectors.toMap(movie -> movie.getTitle(), movie -> movie.getId()));
        if (movieIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(CINEMA_ZONE);
        LocalDate firstDay = now.toLocalDate();
        for (int dayOffset = 0; dayOffset <= DAYS_AHEAD; dayOffset++) {
            LocalDate date = firstDay.plusDays(dayOffset);
            for (Slot slot : SLOTS) {
                Long movieId = movieIds.get(slot.movieTitle());
                if (movieId == null) {
                    continue;
                }
                LocalDateTime startsAt = date.atTime(slot.startTime());
                if (!startsAt.isAfter(now)) {
                    continue;
                }

                int inserted = jdbc.update(INSERT_SHOWTIME, movieId, startsAt, VENUE, slot.screenName());
                Long showtimeId = jdbc.queryForObject(FIND_SHOWTIME, Long.class,
                        movieId, startsAt, slot.screenName());
                if (inserted > 0 && showtimeId != null) {
                    seedSeats(showtimeId);
                }
            }
        }
    }

    private void seedSeats(Long showtimeId) {
        List<Object[]> seatRows = new ArrayList<>(80);
        for (char row = 'A'; row <= 'H'; row++) {
            BigDecimal price = row <= 'B' ? new BigDecimal("320.00")
                    : row <= 'F' ? new BigDecimal("250.00") : new BigDecimal("200.00");
            for (int number = 1; number <= 10; number++) {
                String rowLabel = Character.toString(row);
                seatRows.add(new Object[]{showtimeId, rowLabel + number, rowLabel, number, price});
            }
        }
        jdbc.batchUpdate(INSERT_SEAT, seatRows);
    }

    private record Slot(String movieTitle, LocalTime startTime, String screenName) {}
}
