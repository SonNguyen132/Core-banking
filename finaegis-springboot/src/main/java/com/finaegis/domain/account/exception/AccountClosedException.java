package com.finaegis.domain.account.exception;

public class AccountClosedException extends RuntimeException {
    public AccountClosedException(String accountId) {
        super("Account " + accountId + " is closed");
    }
}