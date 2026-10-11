package com.portal.movies.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "show_seats", uniqueConstraints = @UniqueConstraint(name = "uk_show_seats_show_label", columnNames = {"showtime_id", "seat_label"}),
       indexes = {@Index(name="idx_show_seats_hold_token", columnList="hold_token"), @Index(name="idx_show_seats_show_status", columnList="showtime_id, status")})
public class ShowSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "showtime_id", nullable = false) private MovieShowtime showtime;
    @Column(name = "seat_label", nullable = false, length = 5) private String seatLabel;
    @Column(name = "row_label", nullable = false, length = 1) private String rowLabel;
    @Column(name = "seat_number", nullable = false) private int seatNumber;
    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2) private BigDecimal priceAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private SeatStatus status = SeatStatus.AVAILABLE;
    @Column(name = "hold_token", length = 36) private String holdToken;
    @Column(name = "held_by_user_id") private Long heldByUserId;
    @Column(name = "hold_expires_at") private LocalDateTime holdExpiresAt;
    @Column(name = "hold_size", nullable = false) private int holdSize;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "booking_id") private Booking booking;

    protected ShowSeat() {}
    public ShowSeat(MovieShowtime showtime, String seatLabel, String rowLabel, int seatNumber, BigDecimal priceAmount){
        this.showtime=showtime;this.seatLabel=seatLabel;this.rowLabel=rowLabel;this.seatNumber=seatNumber;this.priceAmount=priceAmount;
    }
    public void hold(String token, Long userId, LocalDateTime expiresAt, int expectedSize){status=SeatStatus.HELD;holdToken=token;heldByUserId=userId;holdExpiresAt=expiresAt;holdSize=expectedSize;}
    public void book(Booking booking){status=SeatStatus.BOOKED;this.booking=booking;clearHold();}
    public void clearExpiredHold(){status=SeatStatus.AVAILABLE;clearHold();}
    public void release(){status=SeatStatus.AVAILABLE;clearHold();}
    private void clearHold(){holdToken=null;heldByUserId=null;holdExpiresAt=null;holdSize=0;}
    public boolean holdHasExpired(LocalDateTime now){return status==SeatStatus.HELD && holdExpiresAt!=null && !holdExpiresAt.isAfter(now);}
    public Long getId(){return id;} public MovieShowtime getShowtime(){return showtime;} public String getSeatLabel(){return seatLabel;}
    public String getRowLabel(){return rowLabel;} public int getSeatNumber(){return seatNumber;} public BigDecimal getPriceAmount(){return priceAmount;}
    public SeatStatus getStatus(){return status;} public String getHoldToken(){return holdToken;} public Long getHeldByUserId(){return heldByUserId;}
    public LocalDateTime getHoldExpiresAt(){return holdExpiresAt;} public int getHoldSize(){return holdSize;} public Booking getBooking(){return booking;}
}
