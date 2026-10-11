package com.portal.movies.booking;

import com.portal.movies.domain.AppUser;
import com.portal.movies.repository.AppUserRepository;
import com.portal.movies.repository.BookingRepository;
import com.portal.movies.repository.MovieShowtimeRepository;
import com.portal.movies.repository.ShowSeatRepository;
import com.portal.movies.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceValidationTest {
    @Mock ShowSeatRepository seats;
    @Mock MovieShowtimeRepository showtimes;
    @Mock AppUserRepository users;
    @Mock BookingRepository bookings;
    @InjectMocks BookingService service;

    @Test
    void duplicateSeatLabelsAreRejectedBeforeTakingLocks() {
        when(showtimes.existsById(12L)).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(new AppUser("Test Viewer", "viewer@example.test", "not-a-real-hash")));

        ApiException exception = assertThrows(ApiException.class, () -> service.createHold(12L, 7L,
                new BookingDtos.HoldRequest(List.of("c4", " C4 "))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(seats, never()).lockSeats(org.mockito.ArgumentMatchers.eq(12L), org.mockito.ArgumentMatchers.anyCollection());
    }
}
