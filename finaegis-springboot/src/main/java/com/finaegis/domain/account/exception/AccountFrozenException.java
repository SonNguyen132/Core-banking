package com.finaegis.domain.account.exception;

public class AccountFrozenException extends RuntimeException {
    public AccountFrozenException(String accountId) {
        super("Account " + accountId + " is frozen");
    }
}