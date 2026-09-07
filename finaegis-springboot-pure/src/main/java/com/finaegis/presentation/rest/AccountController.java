package com.finaegis.presentation.rest;

import com.finaegis.domain.account.command.AccountCommandService;
import com.finaegis.domain.account.command.AccountQueryService;
import com.finaegis.domain.account.model.AccountView;
import com.finaegis.security.PermissionConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountCommandService commandService;
    private final AccountQueryService queryService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_VIEW + "')")
    public ResponseEntity<List<AccountView>> list(@RequestParam String userId) {
        return ResponseEntity.ok(queryService.listByUser(userId));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_VIEW + "')")
    public ResponseEntity<AccountView> get(@PathVariable String accountId) {
        return ResponseEntity.ok(queryService.get(accountId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_CREATE + "')")
    public ResponseEntity<AccountView> create(@RequestBody CreateAccountRequest request) {
        String accountId = commandService.create(
            request.name(),
            request.userId(),
            request.assetCode() != null ? request.assetCode() : "USD");
        return ResponseEntity.status(HttpStatus.CREATED).body(queryService.get(accountId));
    }

    @PostMapping("/{accountId}/deposit")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_DEPOSIT + "')")
    public ResponseEntity<Void> deposit(@PathVariable String accountId,
                                        @RequestBody MoneyRequest request) {
        commandService.deposit(accountId, request.amount(), request.currency(), request.reference());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/withdraw")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_WITHDRAW + "')")
    public ResponseEntity<Void> withdraw(@PathVariable String accountId,
                                         @RequestBody MoneyRequest request) {
        commandService.withdraw(accountId, request.amount(), request.currency(), request.reference());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/freeze")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_FREEZE + "')")
    public ResponseEntity<Void> freeze(@PathVariable String accountId) {
        commandService.freeze(accountId, "manual freeze");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/unfreeze")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_FREEZE + "')")
    public ResponseEntity<Void> unfreeze(@PathVariable String accountId) {
        commandService.unfreeze(accountId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/close")
    @PreAuthorize("hasAuthority('" + PermissionConstants.ACCOUNT_CLOSE + "')")
    public ResponseEntity<Void> close(@PathVariable String accountId) {
        commandService.close(accountId, "manual close");
        return ResponseEntity.ok().build();
    }

    public record CreateAccountRequest(String name, String userId, String assetCode) {}
    public record MoneyRequest(BigDecimal amount, String currency, String reference) {}
}
