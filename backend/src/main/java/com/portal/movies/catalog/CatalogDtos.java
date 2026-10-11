package com.portal.movies.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CatalogDtos {
    private CatalogDtos() {}
    public record MovieView(Long id, String title, String genre, int runtimeMinutes, String rating,
                            String language, String synopsis, String posterTheme) {}
    public record ShowtimeView(Long id, Long movieId, String movieTitle, LocalDateTime startsAt,
                               String venue, String screenName) {}
    public record SeatView(Long id, String label, String row, int number, String status, BigDecimal price) {}
}
