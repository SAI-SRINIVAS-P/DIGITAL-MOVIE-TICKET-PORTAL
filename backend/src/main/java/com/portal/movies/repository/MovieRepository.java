package com.portal.movies.repository;

import com.portal.movies.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findAllByOrderByTitleAsc();
}
