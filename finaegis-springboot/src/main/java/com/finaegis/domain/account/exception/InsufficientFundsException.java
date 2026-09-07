package com.finaegis.domain.account.exception;

public class InsufficientFundsException extends RuntimeException {
    private final java.math.BigDecimal balance;
    private final java.math.BigDecimal requested;

    public InsufficientFundsException(java.math.BigDecimal balance, java.math.BigDecimal requested) {
        super("Insufficient funds: balance=" + balance + ", requested=" + requested);
        this.balance = balance;
        this.requested = requested;
    }

    public java.math.BigDecimal getBalance() {
        return balance;
    }

    public java.math.BigDecimal getRequested() {
        return requested;
    }
}