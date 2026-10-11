package com.portal.movies.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes",
       uniqueConstraints = @UniqueConstraint(name = "uk_showtimes_movie_start_screen", columnNames = {"movie_id", "starts_at", "screen_name"}),
       indexes = @Index(name = "idx_showtimes_movie_start", columnList = "movie_id, starts_at"))
public class MovieShowtime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "movie_id", nullable = false) private Movie movie;
    @Column(name = "starts_at", nullable = false) private LocalDateTime startsAt;
    @Column(nullable = false, length = 120) private String venue;
    @Column(name = "screen_name", nullable = false, length = 30) private String screenName;

    protected MovieShowtime() {}
    public MovieShowtime(Movie movie, LocalDateTime startsAt, String venue, String screenName) {
        this.movie=movie; this.startsAt=startsAt; this.venue=venue; this.screenName=screenName;
    }
    public Long getId(){return id;} public Movie getMovie(){return movie;} public LocalDateTime getStartsAt(){return startsAt;}
    public String getVenue(){return venue;} public String getScreenName(){return screenName;}
}
