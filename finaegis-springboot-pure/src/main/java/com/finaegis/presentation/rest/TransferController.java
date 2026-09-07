package com.finaegis.presentation.rest;

import com.finaegis.domain.payment.model.TransferApplicationService;
import com.finaegis.domain.payment.model.TransferView;
import com.finaegis.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferApplicationService transferService;

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.TRANSFER_INITIATE + "')")
    public ResponseEntity<Void> initiate(@RequestBody TransferRequest request) {
        transferService.initiate(
            request.fromAccountId(),
            request.toAccountId(),
            request.amount(),
            request.currency(),
            request.description(),
            request.initiatedBy());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{transferId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.TRANSFER_VIEW + "')")
    public ResponseEntity<TransferView> get(@PathVariable String transferId) {
        return ResponseEntity.ok(transferService.get(transferId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.TRANSFER_VIEW + "')")
    public ResponseEntity<List<TransferView>> byAccount(@RequestParam String accountId) {
        return ResponseEntity.ok(transferService.listByAccount(accountId));
    }

    public record TransferRequest(String fromAccountId, String toAccountId,
                                  BigDecimal amount, String currency,
                                  String description, String initiatedBy) {}
}
