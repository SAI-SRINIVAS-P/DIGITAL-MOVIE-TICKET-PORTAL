package com.portal.movies.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_seats", uniqueConstraints = @UniqueConstraint(name = "uk_booking_seat_label", columnNames = {"booking_id", "seat_label"}))
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @Column(name = "seat_label", nullable = false, length = 5) private String seatLabel;
    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2) private BigDecimal priceAmount;
    protected BookingSeat() {}
    public BookingSeat(String seatLabel, BigDecimal priceAmount){this.seatLabel=seatLabel;this.priceAmount=priceAmount;}
    void setBooking(Booking booking){this.booking=booking;}
    public Long getId(){return id;} public String getSeatLabel(){return seatLabel;} public BigDecimal getPriceAmount(){return priceAmount;}
}
