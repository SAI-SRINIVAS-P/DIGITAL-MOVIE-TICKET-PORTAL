package com.portal.movies.booking;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService service;
    public BookingController(BookingService service){this.service=service;}

    @PostMapping("/shows/{showtimeId}/holds") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name = "bearerAuth")
    public BookingDtos.HoldResponse hold(@PathVariable Long showtimeId, @AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody BookingDtos.HoldRequest request) {
        return service.createHold(showtimeId, userId, request);
    }
    @DeleteMapping("/holds/{holdToken}") @ResponseStatus(HttpStatus.NO_CONTENT) @SecurityRequirement(name = "bearerAuth")
    public void release(@PathVariable String holdToken, @AuthenticationPrincipal Long userId) {
        service.releaseHold(userId, holdToken);
    }

    @PostMapping("/holds/confirm") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name = "bearerAuth")
    public BookingDtos.BookingView confirm(@AuthenticationPrincipal Long userId, @Valid @RequestBody BookingDtos.ConfirmRequest request) {
        return service.confirmHold(userId, request);
    }
    @GetMapping("/bookings/me") @SecurityRequirement(name = "bearerAuth") public List<BookingDtos.BookingView> myBookings(@AuthenticationPrincipal Long userId) {
        return service.myBookings(userId);
    }
}
