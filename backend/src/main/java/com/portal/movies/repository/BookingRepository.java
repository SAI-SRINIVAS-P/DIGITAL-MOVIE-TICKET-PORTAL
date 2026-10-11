package com.portal.movies.repository;

import com.portal.movies.domain.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "seats"})
    List<Booking> findByUser_IdOrderByBookedAtDesc(Long userId);
}
