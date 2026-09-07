package com.finaegis.presentation.rest;

import com.finaegis.domain.exchange.service.ExchangeService;
import com.finaegis.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('" + PermissionConstants.EXCHANGE_ORDER_PLACE + "')")
    public ResponseEntity<Void> placeOrder(@RequestBody PlaceOrderRequest request) {
        exchangeService.placeOrder(
            request.accountId(),
            request.symbol(),
            request.side(),
            request.type(),
            request.quantity(),
            request.price());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.EXCHANGE_ORDER_CANCEL + "')")
    public ResponseEntity<Void> cancel(@PathVariable String orderId) {
        exchangeService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    public record PlaceOrderRequest(String accountId, String symbol, String side,
                                    String type, BigDecimal quantity, BigDecimal price) {}
}
