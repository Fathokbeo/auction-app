package com.auctionstudio.model;

public record NewAuctionRequest(
        String title,
        String category,
        String sellerName,
        String location,
        String description,
        double startingPrice,
        double buyNowPrice,
        int durationHours
) {
}
