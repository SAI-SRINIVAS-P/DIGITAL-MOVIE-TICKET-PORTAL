package com.portal.movies.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(name = "uk_bookings_code", columnNames = "booking_code"))
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "booking_code", nullable = false, length = 24, unique = true) private String bookingCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "showtime_id", nullable = false) private MovieShowtime showtime;
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2) private BigDecimal totalAmount;
    @Column(name = "booked_at", nullable = false, updatable = false) private Instant bookedAt;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();

    protected Booking() {}
    public Booking(String bookingCode, AppUser user, MovieShowtime showtime, BigDecimal totalAmount) {
        this.bookingCode=bookingCode; this.user=user; this.showtime=showtime; this.totalAmount=totalAmount;
    }
    @PrePersist void prePersist(){if(bookedAt==null)bookedAt=Instant.now();}
    public void addSeat(BookingSeat seat){seats.add(seat); seat.setBooking(this);}
    public Long getId(){return id;} public String getBookingCode(){return bookingCode;} public AppUser getUser(){return user;}
    public MovieShowtime getShowtime(){return showtime;} public BigDecimal getTotalAmount(){return totalAmount;}
    public Instant getBookedAt(){return bookedAt;} public List<BookingSeat> getSeats(){return seats;}
}
