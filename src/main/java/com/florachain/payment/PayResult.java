package com.florachain.payment;

public record PayResult(boolean success, String channelTxnNo, String message) {
}
