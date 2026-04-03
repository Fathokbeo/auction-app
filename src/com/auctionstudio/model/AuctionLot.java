package com.auctionstudio.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AuctionLot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime createdAt;
    private final List<BidRecord> bidHistory;

    private String title;
    private String category;
    private String sellerName;
    private String location;
    private String description;
    private double startingPrice;
    private double currentBid;
    private double buyNowPrice;
    private LocalDateTime endTime;
    private boolean watched;

    public AuctionLot(
            String id,
            String title,
            String category,
            String sellerName,
            String location,
            String description,
            double startingPrice,
            double buyNowPrice,
            LocalDateTime createdAt,
            LocalDateTime endTime
    ) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.sellerName = sellerName;
        this.location = location;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.buyNowPrice = buyNowPrice;
        this.createdAt = createdAt;
        this.endTime = endTime;
        this.bidHistory = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public double getBuyNowPrice() {
        return buyNowPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isWatched() {
        return watched;
    }

    public List<BidRecord> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public int getBidCount() {
        return bidHistory.size();
    }

    public boolean isActive() {
        return endTime.isAfter(LocalDateTime.now());
    }

    public boolean isEndingSoon() {
        return isActive() && Duration.between(LocalDateTime.now(), endTime).toHours() < 6;
    }

    public boolean hasBuyNow() {
        return buyNowPrice > 0;
    }

    public double getMinimumNextBid() {
        double base = Math.max(startingPrice, currentBid);
        return round(base + nextIncrement(base));
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public void addBid(String bidderName, double amount, LocalDateTime timestamp) {
        bidHistory.add(0, new BidRecord(bidderName, round(amount), timestamp));
        currentBid = round(amount);
    }

    public void closeAtBuyNow(String bidderName, LocalDateTime timestamp) {
        addBid(bidderName, buyNowPrice, timestamp);
        endTime = timestamp;
    }

    private static double nextIncrement(double base) {
        if (base < 5_000_000) {
            return 100_000;
        }
        if (base < 20_000_000) {
            return 250_000;
        }
        return 500_000;
    }

    private static double round(double value) {
        return Math.round(value);
    }
}
