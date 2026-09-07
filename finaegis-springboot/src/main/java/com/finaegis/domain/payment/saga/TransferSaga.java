package com.finaegis.domain.payment.saga;

import com.finaegis.domain.account.command.DepositMoneyCommand;
import com.finaegis.domain.account.command.WithdrawMoneyCommand;
import com.finaegis.domain.account.event.MoneyDepositedEvent;
import com.finaegis.domain.account.event.MoneyWithdrawnEvent;
import com.finaegis.domain.payment.command.CompleteTransferCommand;
import com.finaegis.domain.payment.event.TransferCompletedEvent;
import com.finaegis.domain.payment.event.TransferInitiatedEvent;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
@Slf4j
public class TransferSaga {

    @Autowired
    private transient CommandGateway commandGateway;

    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private boolean debitConfirmed = false;

    @StartSaga
    @SagaEventHandler(associationProperty = "transferId")
    public void handle(TransferInitiatedEvent event) {
        this.fromAccountId = event.getFromAccountId();
        this.toAccountId = event.getToAccountId();
        this.amount = event.getAmount();
        this.currency = event.getCurrency();

        log.info("Saga started: transfer={}, {} -> {}, amount {} {}",
            event.getTransferId(), fromAccountId, toAccountId, amount, currency);

        // Step 1: Debit source
        commandGateway.send(new WithdrawMoneyCommand(
            fromAccountId, amount, currency, "TRANSFER-" + event.getTransferId()
        ));
    }

    @SagaEventHandler(associationProperty = "accountId")
    public void onDebitConfirmed(MoneyWithdrawnEvent event) {
        if (!fromAccountId.equals(event.getAccountId())) {
            return;
        }
        this.debitConfirmed = true;
        log.info("Debit confirmed for transfer: {} - {}, amount {}", 
            event.getAccountId(), event.getReference(), amount);

        // Step 2: Credit destination
        commandGateway.send(new DepositMoneyCommand(
            toAccountId, amount, currency, event.getReference()
        ));
    }

    @SagaEventHandler(associationProperty = "accountId")
    public void onCreditConfirmed(MoneyDepositedEvent event) {
        if (!toAccountId.equals(event.getAccountId())) {
            return;
        }
        log.info("Credit confirmed for transfer: reference={}, amount {}", 
            event.getReference(), amount);

        String ref = event.getReference();
        String transferId = ref != null && ref.startsWith("TRANSFER-")
            ? ref.substring("TRANSFER-".length()) : ref;
        commandGateway.send(new CompleteTransferCommand(transferId));
        SagaLifecycle.end();
    }

    @EndSaga
    public void onComplete(TransferCompletedEvent event) {
        log.info("Transfer saga completed: {}", event.getTransferId());
    }

    private void failTransfer(String cause, String transferId) {
        log.error("Transfer {} failed: {}", transferId, cause);
        if (debitConfirmed) {
            // Compensation: reverse the debit
            commandGateway.send(new DepositMoneyCommand(
                fromAccountId, amount, currency,
                "COMPENSATE-" + transferId
            ));
            log.info("Compensation executed for transfer {}", transferId);
        }
        SagaLifecycle.end();
    }
}
