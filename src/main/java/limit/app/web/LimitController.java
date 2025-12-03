package limit.app.web;

import limit.app.service.UserService;
import limit.app.web.dto.LimitReservationResponse;
import limit.app.web.dto.ReserveLimitRequest;
import limit.app.web.mapper.LimitReservationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/limits")
public class LimitController {

    private final UserService userService;
    private final LimitReservationMapper reservationMapper;

    public LimitController(UserService userService, LimitReservationMapper reservationMapper) {
        this.userService = userService;
        this.reservationMapper = reservationMapper;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public LimitReservationResponse reserve(@RequestBody ReserveLimitRequest request) {
        var reservation = userService.reserveLimit(request.externalUserId(), request.amount(), request.requestId());
        return reservationMapper.toResponse(reservation);
    }

    @PostMapping("/reservations/{id}/confirm")
    public LimitReservationResponse confirm(@PathVariable("id") Long id) {
        var reservation = userService.confirmLimitAndDebit(id);
        return reservationMapper.toResponse(reservation);
    }

    @PostMapping("/reservations/{id}/cancel")
    public LimitReservationResponse cancel(@PathVariable("id") Long id) {
        var reservation = userService.cancelReservation(id);
        return reservationMapper.toResponse(reservation);
    }
}
