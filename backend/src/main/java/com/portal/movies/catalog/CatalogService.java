package com.portal.movies.catalog;

import com.portal.movies.domain.Movie;
import com.portal.movies.domain.MovieShowtime;
import com.portal.movies.domain.SeatStatus;
import com.portal.movies.repository.MovieRepository;
import com.portal.movies.repository.MovieShowtimeRepository;
import com.portal.movies.repository.ShowSeatRepository;
import com.portal.movies.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CatalogService {
    private final MovieRepository movies; private final MovieShowtimeRepository showtimes; private final ShowSeatRepository seats;
    public CatalogService(MovieRepository movies, MovieShowtimeRepository showtimes, ShowSeatRepository seats) {
        this.movies=movies; this.showtimes=showtimes; this.seats=seats;
    }
    @Transactional(readOnly = true)
    public List<CatalogDtos.MovieView> movies() {
        return movies.findAllByOrderByTitleAsc().stream().map(this::movieView).toList();
    }
    @Transactional(readOnly = true)
    public List<CatalogDtos.ShowtimeView> allUpcomingShows() {
        return showtimes.findByStartsAtAfterOrderByStartsAtAsc(LocalDateTime.now()).stream().map(this::showView).toList();
    }
    @Transactional(readOnly = true)
    public List<CatalogDtos.ShowtimeView> showsForMovie(Long movieId) {
        if (!movies.existsById(movieId)) throw ApiException.notFound("Movie not found.");
        return showtimes.findByMovie_IdAndStartsAtAfterOrderByStartsAtAsc(movieId, LocalDateTime.now()).stream().map(this::showView).toList();
    }
    @Transactional(readOnly = true)
    public List<CatalogDtos.SeatView> seatsForShow(Long showtimeId) {
        if (!showtimes.existsById(showtimeId)) throw ApiException.notFound("Showtime not found.");
        LocalDateTime now=LocalDateTime.now();
        return seats.findByShowtime_IdOrderByRowLabelAscSeatNumberAsc(showtimeId).stream().map(seat -> {
            String status = seat.getStatus()==SeatStatus.HELD && seat.holdHasExpired(now) ? "AVAILABLE" : seat.getStatus().name();
            return new CatalogDtos.SeatView(seat.getId(), seat.getSeatLabel(), seat.getRowLabel(), seat.getSeatNumber(), status, seat.getPriceAmount());
        }).toList();
    }
    private CatalogDtos.MovieView movieView(Movie m){return new CatalogDtos.MovieView(m.getId(),m.getTitle(),m.getGenre(),m.getRuntimeMinutes(),m.getRating(),m.getLanguage(),m.getSynopsis(),m.getPosterTheme());}
    private CatalogDtos.ShowtimeView showView(MovieShowtime s){return new CatalogDtos.ShowtimeView(s.getId(),s.getMovie().getId(),s.getMovie().getTitle(),s.getStartsAt(),s.getVenue(),s.getScreenName());}
}
