package de.trailmoney.api.transaction;

public enum TransactionResultCode {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    ACCOUNT_NOT_FOUND,
    INVALID_AMOUNT,
    LIMIT_EXCEEDED,
    CANCELLED,
    STORAGE_ERROR
}
