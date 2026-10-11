package com.portal.movies.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "movies")
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 140) private String title;
    @Column(nullable = false, length = 80) private String genre;
    @Column(name = "runtime_minutes", nullable = false) private int runtimeMinutes;
    @Column(nullable = false, length = 12) private String rating;
    @Column(nullable = false, length = 30) private String language;
    @Column(nullable = false, length = 800) private String synopsis;
    @Column(name = "poster_theme", nullable = false, length = 40) private String posterTheme;

    protected Movie() {}
    public Movie(String title, String genre, int runtimeMinutes, String rating, String language, String synopsis, String posterTheme) {
        this.title=title; this.genre=genre; this.runtimeMinutes=runtimeMinutes; this.rating=rating;
        this.language=language; this.synopsis=synopsis; this.posterTheme=posterTheme;
    }
    public Long getId(){return id;} public String getTitle(){return title;} public String getGenre(){return genre;}
    public int getRuntimeMinutes(){return runtimeMinutes;} public String getRating(){return rating;}
    public String getLanguage(){return language;} public String getSynopsis(){return synopsis;} public String getPosterTheme(){return posterTheme;}
}
