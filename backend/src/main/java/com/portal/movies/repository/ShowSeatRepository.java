package com.portal.movies.repository;

import com.portal.movies.domain.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {
    List<ShowSeat> findByShowtime_IdOrderByRowLabelAscSeatNumberAsc(Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.showtime.id = :showtimeId and s.seatLabel in :labels order by s.id")
    List<ShowSeat> lockSeats(@Param("showtimeId") Long showtimeId, @Param("labels") Collection<String> labels);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.holdToken = :token and s.heldByUserId = :userId order by s.id")
    List<ShowSeat> lockSeatsByHold(@Param("token") String token, @Param("userId") Long userId);
}
