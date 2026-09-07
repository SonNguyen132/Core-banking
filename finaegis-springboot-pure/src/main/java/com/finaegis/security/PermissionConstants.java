package com.finaegis.security;

public final class PermissionConstants {

    private PermissionConstants() {
    }

    public static final String ACCOUNT_VIEW = "ACCOUNT_VIEW";
    public static final String ACCOUNT_CREATE = "ACCOUNT_CREATE";
    public static final String ACCOUNT_DEPOSIT = "ACCOUNT_DEPOSIT";
    public static final String ACCOUNT_WITHDRAW = "ACCOUNT_WITHDRAW";
    public static final String ACCOUNT_FREEZE = "ACCOUNT_FREEZE";
    public static final String ACCOUNT_CLOSE = "ACCOUNT_CLOSE";

    public static final String TRANSFER_INITIATE = "TRANSFER_INITIATE";
    public static final String TRANSFER_VIEW = "TRANSFER_VIEW";

    public static final String LOAN_APPLY = "LOAN_APPLY";
    public static final String LOAN_APPROVE = "LOAN_APPROVE";
    public static final String LOAN_VIEW = "LOAN_VIEW";

    public static final String EXCHANGE_ORDER_PLACE = "EXCHANGE_ORDER_PLACE";
    public static final String EXCHANGE_ORDER_CANCEL = "EXCHANGE_ORDER_CANCEL";

    public static final String STABLECOIN_MINT = "STABLECOIN_MINT";
    public static final String STABLECOIN_BURN = "STABLECOIN_BURN";
    public static final String STABLECOIN_VIEW = "STABLECOIN_VIEW";

    public static final String ASSET_VIEW = "ASSET_VIEW";
    public static final String ASSET_RATE_VIEW = "ASSET_RATE_VIEW";
    public static final String ASSET_CONVERT = "ASSET_CONVERT";

    public static final String GOVERNANCE_POLL_CREATE = "GOVERNANCE_POLL_CREATE";
    public static final String GOVERNANCE_VOTE = "GOVERNANCE_VOTE";
    public static final String GOVERNANCE_POLL_VIEW = "GOVERNANCE_POLL_VIEW";
}
