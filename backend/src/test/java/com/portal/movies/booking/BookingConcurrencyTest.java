package com.portal.movies.booking;

import com.portal.movies.domain.AppUser;
import com.portal.movies.domain.Movie;
import com.portal.movies.domain.MovieShowtime;
import com.portal.movies.domain.ShowSeat;
import com.portal.movies.repository.AppUserRepository;
import com.portal.movies.repository.MovieRepository;
import com.portal.movies.repository.MovieShowtimeRepository;
import com.portal.movies.repository.ShowSeatRepository;
import com.portal.movies.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.demo-schedule.enabled=true")
@Testcontainers(disabledWithoutDocker = true)
class BookingConcurrencyTest {
    // The Testcontainers JUnit extension owns this resource and stops it after the test class.
    @SuppressWarnings("resource")
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("movie_portal_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired BookingService bookingService;
    @Autowired AppUserRepository users;
    @Autowired MovieRepository movies;
    @Autowired MovieShowtimeRepository showtimes;
    @Autowired ShowSeatRepository seats;

    @Test
    void rollingDemoScheduleCreatesFutureShowtimesBeyondTheInitialMigrationWindow() {
        Movie demoMovie = movies.findAllByOrderByTitleAsc().stream()
                .filter(movie -> movie.getTitle().equals("The Last Horizon"))
                .findFirst().orElseThrow();
        LocalDateTime scheduledStart = LocalDate.now(ZoneId.of("Asia/Kolkata"))
                .plusDays(14).atTime(20, 45);

        MovieShowtime futureShow = showtimes
                .findByMovie_IdAndStartsAtAfterOrderByStartsAtAsc(demoMovie.getId(), scheduledStart.minusSeconds(1))
                .stream()
                .filter(show -> show.getStartsAt().equals(scheduledStart))
                .findFirst().orElseThrow(() -> new AssertionError("Rolling schedule should create a showtime 14 days ahead"));

        assertEquals(80, seats.findByShowtime_IdOrderByRowLabelAscSeatNumberAsc(futureShow.getId()).size(),
                "each new demo showtime should receive its 80-seat map");
    }

    @Test
    void twoUsersCannotHoldTheSameSeatAtTheSameTime() throws Exception {
        AppUser first = users.saveAndFlush(new AppUser("First Viewer", "first-viewer@example.test", "already-hashed"));
        AppUser second = users.saveAndFlush(new AppUser("Second Viewer", "second-viewer@example.test", "already-hashed"));
        Movie movie = movies.saveAndFlush(new Movie("Concurrency Test Film", "Test", 90, "U", "English", "Fixture for seat locking.", "violet"));
        MovieShowtime show = showtimes.saveAndFlush(new MovieShowtime(movie, LocalDateTime.now().plusHours(2), "Test Cinema", "Screen T"));
        seats.saveAndFlush(new ShowSeat(show, "C4", "C", 4, new BigDecimal("250.00")));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstAttempt = pool.submit(() -> attemptHold(start, first.getId(), show.getId()));
            Future<Boolean> secondAttempt = pool.submit(() -> attemptHold(start, second.getId(), show.getId()));
            start.countDown();
            boolean firstAccepted = firstAttempt.get();
            boolean secondAccepted = secondAttempt.get();
            assertTrue(firstAccepted || secondAccepted, "one contender must acquire the seat");
            assertEquals(1, (firstAccepted ? 1 : 0) + (secondAccepted ? 1 : 0), "exactly one contender may hold the seat");
            assertEquals(1, seats.findByShowtime_IdOrderByRowLabelAscSeatNumberAsc(show.getId()).stream()
                    .filter(s -> "HELD".equals(s.getStatus().name())).count());
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean attemptHold(CountDownLatch start, Long userId, Long showtimeId) throws Exception {
        start.await();
        try {
            bookingService.createHold(showtimeId, userId, new BookingDtos.HoldRequest(List.of("C4")));
            return true;
        } catch (ApiException expectedConflict) {
            return false;
        }
    }
}
