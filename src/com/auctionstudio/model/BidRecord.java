package com.auctionstudio.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public final class BidRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String bidderName;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidRecord(String bidderName, double amount, LocalDateTime timestamp) {
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
