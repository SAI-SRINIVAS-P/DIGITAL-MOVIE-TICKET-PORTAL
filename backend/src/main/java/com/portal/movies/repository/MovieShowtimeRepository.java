package com.portal.movies.repository;

import com.portal.movies.domain.MovieShowtime;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface MovieShowtimeRepository extends JpaRepository<MovieShowtime, Long> {
    List<MovieShowtime> findByMovie_IdAndStartsAtAfterOrderByStartsAtAsc(Long movieId, LocalDateTime from);
    List<MovieShowtime> findByStartsAtAfterOrderByStartsAtAsc(LocalDateTime from);
}
