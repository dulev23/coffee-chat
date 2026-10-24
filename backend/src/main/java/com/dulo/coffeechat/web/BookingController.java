package com.dulo.coffeechat.web;

import com.dulo.coffeechat.dto.BookingResponse;
import com.dulo.coffeechat.dto.CreateBookingRequest;
import com.dulo.coffeechat.dto.HostBookingResponse;
import com.dulo.coffeechat.service.BookingService;
import com.dulo.coffeechat.service.ConfirmationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ConfirmationService confirmationService;

    public BookingController(BookingService bookingService, ConfirmationService confirmationService) {
        this.bookingService = bookingService;
        this.confirmationService = confirmationService;
    }

    @GetMapping
    public List<HostBookingResponse> upcoming() {
        return bookingService.findUpcoming().stream().map(HostBookingResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody CreateBookingRequest request) {
        var booking = bookingService.book(request);
        return BookingResponse.from(booking, confirmationService.generate(booking));
    }
}
