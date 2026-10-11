INSERT INTO movies (title, genre, runtime_minutes, rating, language, synopsis, poster_theme) VALUES
('The Last Horizon', 'Sci-Fi / Adventure', 148, 'UA 13+', 'English', 'A rescue pilot discovers that the signal guiding humanity home may be coming from tomorrow.', 'violet'),
('Monsoon Letters', 'Romance / Drama', 126, 'U', 'Hindi', 'Two strangers exchange letters during a season of rain and uncover a story their families never finished.', 'rose'),
('Neon Run', 'Action / Thriller', 119, 'UA 16+', 'English', 'A courier has one night to deliver a package that can expose the city’s most powerful syndicate.', 'amber');

-- Fourteen days of sample showtimes keep a fresh checkout path available on later starts too.
INSERT INTO showtimes (movie_id, starts_at, venue, screen_name)
SELECT m.id,
       TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL days.day_offset DAY), slots.show_time),
       'Luma Cinemas · Hyderabad', slots.screen_name
FROM movies m
CROSS JOIN (SELECT 0 day_offset UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13) days
CROSS JOIN (
  SELECT 'The Last Horizon' movie_title, CAST('11:30:00' AS TIME) show_time, 'Screen 01' screen_name UNION ALL
  SELECT 'The Last Horizon', CAST('20:45:00' AS TIME), 'Screen 01' UNION ALL
  SELECT 'Monsoon Letters', CAST('13:00:00' AS TIME), 'Screen 03' UNION ALL
  SELECT 'Monsoon Letters', CAST('18:20:00' AS TIME), 'Screen 03' UNION ALL
  SELECT 'Neon Run', CAST('15:15:00' AS TIME), 'Screen 02' UNION ALL
  SELECT 'Neon Run', CAST('22:00:00' AS TIME), 'Screen 02'
) slots
WHERE m.title = slots.movie_title
  AND TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL days.day_offset DAY), slots.show_time) > CURRENT_TIMESTAMP();

INSERT INTO show_seats (showtime_id, seat_label, row_label, seat_number, price_amount, status)
SELECT st.id, CONCAT(r.row_label, n.seat_number), r.row_label, n.seat_number,
       CASE WHEN r.row_label IN ('A','B') THEN 320.00 WHEN r.row_label IN ('C','D','E','F') THEN 250.00 ELSE 200.00 END,
       'AVAILABLE'
FROM showtimes st
CROSS JOIN (SELECT 'A' row_label UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D' UNION ALL SELECT 'E' UNION ALL SELECT 'F' UNION ALL SELECT 'G' UNION ALL SELECT 'H') r
CROSS JOIN (SELECT 1 seat_number UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) n;
