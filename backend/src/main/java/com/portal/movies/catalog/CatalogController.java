package com.portal.movies.catalog;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service){this.service=service;}
    @GetMapping("/movies") public List<CatalogDtos.MovieView> movies(){return service.movies();}
    @GetMapping("/shows") public List<CatalogDtos.ShowtimeView> shows(){return service.allUpcomingShows();}
    @GetMapping("/movies/{movieId}/shows") public List<CatalogDtos.ShowtimeView> showsForMovie(@PathVariable Long movieId){return service.showsForMovie(movieId);}
    @GetMapping("/shows/{showtimeId}/seats") public List<CatalogDtos.SeatView> seats(@PathVariable Long showtimeId){return service.seatsForShow(showtimeId);}
}
