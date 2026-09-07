package com.finaegis.presentation.rest;

import com.finaegis.domain.account.command.CreateAccountCommand;
import com.finaegis.domain.account.command.DepositMoneyCommand;
import com.finaegis.domain.account.command.FreezeAccountCommand;
import com.finaegis.domain.account.command.UnfreezeAccountCommand;
import com.finaegis.domain.account.command.WithdrawMoneyCommand;
import com.finaegis.domain.account.model.AccountView;
import com.finaegis.domain.account.query.GetAccountQuery;
import com.finaegis.domain.account.query.GetAccountsByUserQuery;
import com.finaegis.domain.account.repository.AccountViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final AccountViewRepository repository;

    @GetMapping
    public ResponseEntity<List<AccountView>> list(@RequestParam String userId) {
        return ResponseEntity.ok(repository.findByUserId(userId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountView> get(@PathVariable String accountId) {
        return ResponseEntity.ok(repository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateAccountRequest request) {
        String accountId = UUID.randomUUID().toString();
        commandGateway.sendAndWait(new CreateAccountCommand(
            accountId,
            request.name(),
            request.userId(),
            request.assetCode() != null ? request.assetCode() : "USD"
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(repository.findById(accountId).orElse(null));
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Void> deposit(@PathVariable String accountId,
                                        @RequestBody DepositRequest request) {
        commandGateway.sendAndWait(new DepositMoneyCommand(
            accountId, request.amount(), request.currency(), request.reference()
        ));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String accountId,
                                         @RequestBody DepositRequest request) {
        commandGateway.sendAndWait(new WithdrawMoneyCommand(
            accountId, request.amount(), request.currency(), request.reference()
        ));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/freeze")
    public ResponseEntity<Void> freeze(@PathVariable String accountId,
                                       @RequestBody(required = false) String reason) {
        commandGateway.sendAndWait(new FreezeAccountCommand(accountId, reason));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/unfreeze")
    public ResponseEntity<Void> unfreeze(@PathVariable String accountId) {
        commandGateway.sendAndWait(new UnfreezeAccountCommand(accountId));
        return ResponseEntity.ok().build();
    }

    public record CreateAccountRequest(String name, String userId, String assetCode) {}
    public record DepositRequest(BigDecimal amount, String currency, String reference) {}
}
