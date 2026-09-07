package com.finaegis.presentation.rest;

import com.finaegis.domain.payment.command.InitiateTransferCommand;
import com.finaegis.domain.payment.model.TransferView;
import com.finaegis.domain.payment.repository.TransferViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final CommandGateway commandGateway;
    private final TransferViewRepository transferRepository;

    @PostMapping
    public ResponseEntity<Void> initiate(@RequestBody TransferRequest request) {
        String transferId = UUID.randomUUID().toString();
        commandGateway.sendAndWait(new InitiateTransferCommand(
            transferId,
            request.fromAccountId(),
            request.toAccountId(),
            request.amount(),
            request.currency(),
            request.description(),
            request.initiatedBy()
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<TransferView> get(@PathVariable String transferId) {
        return ResponseEntity.ok(transferRepository.findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId)));
    }

    public record TransferRequest(String fromAccountId, String toAccountId,
                                  BigDecimal amount, String currency,
                                  String description, String initiatedBy) {}
}
