package com.finaegis.domain.payment.saga;

import com.finaegis.domain.account.command.AccountCommandService;
import com.finaegis.domain.payment.aggregate.Transfer;
import com.finaegis.domain.payment.event.TransferInitiatedEvent;
import com.finaegis.eventstore.AggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Hand-written Saga (distributed transaction coordinator) - pure Spring, no Axon.
 *
 * Flow (orchestrated):
 *   1. On TransferInitiatedEvent -> debit source account (withdraw).
 *   2. If debit ok  -> credit destination (deposit).
 *   3. If credit ok -> complete the transfer.
 *   4. If any step fails -> COMPENSATE: refund the debited amount back to source,
 *      then mark the transfer FAILED.
 *
 * The saga reads state from the Transfer aggregate (event-sourced) so it is
 * durable across restarts, unlike holding state only in memory.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferSaga {

    private final AccountCommandService accountCommandService;
    private final AggregateRepository aggregateRepository;

    @EventListener
    @Transactional
    public void on(TransferInitiatedEvent event) {
        String transferId = event.getAggregateId();
        try {
            log.info("Saga step 1: debit {} from account {} (transfer {})",
                event.amount, event.fromAccountId, transferId);

            // Step 1: Debit source
            accountCommandService.withdraw(
                event.fromAccountId, event.amount, event.currency, reference("DEBIT", transferId));

            log.info("Saga step 2: credit {} to account {} (transfer {})",
                event.amount, event.toAccountId, transferId);

            // Step 2: Credit destination
            accountCommandService.deposit(
                event.toAccountId, event.amount, event.currency, reference("CREDIT", transferId));

            log.info("Saga step 3: complete transfer {}", transferId);
            // Step 3: Complete the transfer
            Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new);
            transfer.complete();
            aggregateRepository.save(transfer, transfer.getVersion());

        } catch (Exception ex) {
            // Compensation: reverse whatever already happened
            compensate(event, transferId, ex);
            throw ex;
        }
    }

    private void compensate(TransferInitiatedEvent event, String transferId, Exception cause) {
        log.error("Transfer {} failed ({}). Executing compensation...", transferId, cause.getMessage());
        try {
            // Reverse the debit: deposit the amount back to the source account.
            accountCommandService.deposit(
                event.fromAccountId, event.amount, event.currency, reference("COMPENSATE", transferId));
            log.info("Compensation: refunded {} back to {} (transfer {})",
                event.amount, event.fromAccountId, transferId);
        } catch (Exception compEx) {
            log.error("Compensation ALSO failed for transfer {}: {}", transferId, compEx.getMessage());
        }

        // Mark the transfer as failed (even if compensation partially failed).
        try {
            Transfer transfer = aggregateRepository.load(transferId, Transfer.TYPE, Transfer::new);
            transfer.fail(cause.getMessage());
            aggregateRepository.save(transfer, transfer.getVersion());
        } catch (Exception failEx) {
            log.error("Failed to mark transfer {} as FAILED: {}", transferId, failEx.getMessage());
        }
    }

    private String reference(String step, String transferId) {
        return "TRANSFER-" + step + "-" + transferId;
    }
}
