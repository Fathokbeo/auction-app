package com.auctionstudio.service;

import com.auctionstudio.model.AppState;
import com.auctionstudio.model.AuctionLot;
import com.auctionstudio.model.NewAuctionRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class AuctionService {
    private final AuctionRepository repository;
    private final AppState state;

    public AuctionService(AuctionRepository repository) {
        this.repository = repository;
        this.state = repository.load();
        if (state.getLots() == null || state.getLots().isEmpty()) {
            state.setLots(new ArrayList<>(sampleLots()));
            repository.save(state);
        }
    }

    public List<AuctionLot> getAllLots() {
        return List.copyOf(state.getLots());
    }

    public List<AuctionLot> filterLots(String query, String category, SortMode sortMode) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "All" : category;
        return state.getLots().stream()
                .filter(lot -> matchesQuery(lot, normalizedQuery))
                .filter(lot -> "All".equalsIgnoreCase(normalizedCategory) || lot.getCategory().equalsIgnoreCase(normalizedCategory))
                .sorted(comparatorFor(sortMode))
                .toList();
    }

    public AuctionLot findById(String lotId) {
        return state.getLots().stream()
                .filter(lot -> lot.getId().equals(lotId))
                .findFirst()
                .orElse(null);
    }

    public void placeBid(String lotId, String bidderName, double amount) {
        AuctionLot lot = requireLot(lotId);
        if (!lot.isActive()) {
            throw new IllegalArgumentException("This auction is already closed.");
        }
        if (bidderName == null || bidderName.isBlank()) {
            throw new IllegalArgumentException("Bidder name is required.");
        }
        double minimumBid = lot.getMinimumNextBid();
        if (amount < minimumBid) {
            throw new IllegalArgumentException("Minimum next bid is " + formatCurrency(minimumBid) + ".");
        }
        if (lot.hasBuyNow() && amount >= lot.getBuyNowPrice()) {
            throw new IllegalArgumentException("Use Buy Now for this amount.");
        }
        lot.addBid(bidderName.trim(), amount, LocalDateTime.now());
        repository.save(state);
    }

    public void buyNow(String lotId, String bidderName) {
        AuctionLot lot = requireLot(lotId);
        if (!lot.isActive()) {
            throw new IllegalArgumentException("This auction is already closed.");
        }
        if (!lot.hasBuyNow()) {
            throw new IllegalArgumentException("This item does not support Buy Now.");
        }
        if (bidderName == null || bidderName.isBlank()) {
            throw new IllegalArgumentException("Bidder name is required.");
        }
        lot.closeAtBuyNow(bidderName.trim(), LocalDateTime.now());
        repository.save(state);
    }

    public AuctionLot createAuction(NewAuctionRequest request) {
        validate(request);
        LocalDateTime now = LocalDateTime.now();
        AuctionLot lot = new AuctionLot(
                UUID.randomUUID().toString(),
                request.title().trim(),
                request.category().trim(),
                request.sellerName().trim(),
                request.location().trim(),
                request.description().trim(),
                request.startingPrice(),
                request.buyNowPrice(),
                now,
                now.plusHours(request.durationHours())
        );
        state.getLots().add(lot);
        repository.save(state);
        return lot;
    }

    public void toggleWatch(String lotId) {
        AuctionLot lot = requireLot(lotId);
        lot.setWatched(!lot.isWatched());
        repository.save(state);
    }

    public List<String> getCategories() {
        Set<String> categories = new LinkedHashSet<>();
        categories.add("All");
        state.getLots().stream()
                .map(AuctionLot::getCategory)
                .sorted(String::compareToIgnoreCase)
                .forEach(categories::add);
        return List.copyOf(categories);
    }

    public List<AuctionLot> getWatchlist() {
        return state.getLots().stream()
                .filter(AuctionLot::isWatched)
                .sorted(Comparator.comparing(AuctionLot::getEndTime))
                .toList();
    }

    public List<AuctionLot> getFeaturedLots(int limit) {
        return state.getLots().stream()
                .filter(AuctionLot::isActive)
                .sorted(Comparator.comparingInt(AuctionLot::getBidCount).reversed()
                        .thenComparing(AuctionLot::getCurrentBid, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    public List<AuctionLot> getEndingSoon(int limit) {
        return state.getLots().stream()
                .filter(AuctionLot::isActive)
                .sorted(Comparator.comparing(AuctionLot::getEndTime))
                .limit(limit)
                .toList();
    }

    public DashboardMetrics getDashboardMetrics() {
        int activeLots = (int) state.getLots().stream().filter(AuctionLot::isActive).count();
        long totalBids = state.getLots().stream().mapToLong(AuctionLot::getBidCount).sum();
        long watchedLots = state.getLots().stream().filter(AuctionLot::isWatched).count();
        int endingSoon = (int) state.getLots().stream().filter(AuctionLot::isEndingSoon).count();
        double topBid = state.getLots().stream().mapToDouble(AuctionLot::getCurrentBid).max().orElse(0);
        return new DashboardMetrics(activeLots, totalBids, watchedLots, endingSoon, topBid);
    }

    public List<ActivityEntry> getRecentActivity(int limit) {
        List<ActivityEntry> activities = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM HH:mm");
        for (AuctionLot lot : state.getLots()) {
            activities.add(new ActivityEntry(
                    "New listing",
                    lot.getTitle() + " opened by " + lot.getSellerName() + " at " + formatter.format(lot.getCreatedAt()),
                    lot.getCreatedAt(),
                    "LISTING"
            ));
            lot.getBidHistory().stream()
                    .limit(4)
                    .forEach(bid -> activities.add(new ActivityEntry(
                            "Bid placed",
                            bid.getBidderName() + " bid " + formatCurrency(bid.getAmount()) + " on " + lot.getTitle(),
                            bid.getTimestamp(),
                            "BID"
                    )));
        }
        return activities.stream()
                .sorted(Comparator.comparing(ActivityEntry::time).reversed())
                .limit(limit)
                .toList();
    }

    public static String formatCurrency(double value) {
        return String.format(Locale.US, "%,.0f VND", value);
    }

    private static boolean matchesQuery(AuctionLot lot, String query) {
        if (query.isBlank()) {
            return true;
        }
        String searchable = String.join(" ",
                lot.getTitle(),
                lot.getCategory(),
                lot.getSellerName(),
                lot.getLocation(),
                lot.getDescription()
        ).toLowerCase(Locale.ROOT);
        return searchable.contains(query);
    }

    private static Comparator<AuctionLot> comparatorFor(SortMode sortMode) {
        SortMode effectiveSort = sortMode == null ? SortMode.ENDING_SOON : sortMode;
        return switch (effectiveSort) {
            case ENDING_SOON -> Comparator.comparing(AuctionLot::isActive).reversed()
                    .thenComparing(AuctionLot::getEndTime);
            case HIGHEST_BID -> Comparator.comparing(AuctionLot::getCurrentBid).reversed()
                    .thenComparing(AuctionLot::getEndTime);
            case MOST_BIDS -> Comparator.comparingInt(AuctionLot::getBidCount).reversed()
                    .thenComparing(AuctionLot::getEndTime);
            case NEWEST -> Comparator.comparing(AuctionLot::getCreatedAt).reversed();
        };
    }

    private AuctionLot requireLot(String lotId) {
        AuctionLot lot = findById(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Auction lot was not found.");
        }
        return lot;
    }

    private static void validate(NewAuctionRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (request.category() == null || request.category().isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (request.sellerName() == null || request.sellerName().isBlank()) {
            throw new IllegalArgumentException("Seller name is required.");
        }
        if (request.location() == null || request.location().isBlank()) {
            throw new IllegalArgumentException("Location is required.");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("Description is required.");
        }
        if (request.startingPrice() <= 0) {
            throw new IllegalArgumentException("Starting price must be greater than zero.");
        }
        if (request.buyNowPrice() > 0 && request.buyNowPrice() <= request.startingPrice()) {
            throw new IllegalArgumentException("Buy Now price must be greater than starting price.");
        }
        if (request.durationHours() < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 hour.");
        }
    }

    private static List<AuctionLot> sampleLots() {
        LocalDateTime now = LocalDateTime.now();

        AuctionLot laptop = new AuctionLot(
                UUID.randomUUID().toString(),
                "MacBook Pro 14 M4",
                "Tech",
                "Linh Tran",
                "Ho Chi Minh City",
                "16GB RAM, 1TB SSD, clean condition and boxed accessories included.",
                32_000_000,
                46_500_000,
                now.minusDays(2),
                now.plusHours(18)
        );
        laptop.addBid("An", 33_000_000, now.minusHours(20));
        laptop.addBid("Minh", 34_500_000, now.minusHours(12));
        laptop.addBid("Guest 08", 36_000_000, now.minusHours(3));

        AuctionLot camera = new AuctionLot(
                UUID.randomUUID().toString(),
                "Fujifilm X-T5 Kit",
                "Camera",
                "Studio South",
                "Da Nang",
                "40MP body with 18-55 lens, low shutter count and premium strap.",
                24_000_000,
                31_000_000,
                now.minusDays(1),
                now.plusHours(5)
        );
        camera.addBid("Bao", 24_500_000, now.minusHours(16));
        camera.addBid("Thanh", 25_500_000, now.minusHours(7));
        camera.setWatched(true);

        AuctionLot chair = new AuctionLot(
                UUID.randomUUID().toString(),
                "Herman Miller Aeron",
                "Furniture",
                "Workspace Lab",
                "Ha Noi",
                "Size B ergonomic chair, graphite finish, fully adjustable lumbar support.",
                17_000_000,
                23_000_000,
                now.minusDays(3),
                now.plusHours(30)
        );
        chair.addBid("Guest 03", 18_000_000, now.minusHours(28));
        chair.addBid("Duc", 19_500_000, now.minusHours(4));

        AuctionLot speaker = new AuctionLot(
                UUID.randomUUID().toString(),
                "Marshall Woburn III",
                "Audio",
                "Retro Audio",
                "Hai Phong",
                "Flagship wireless speaker with warm tuning and original packaging.",
                9_500_000,
                13_500_000,
                now.minusHours(18),
                now.plusHours(2)
        );
        speaker.addBid("Lan", 9_800_000, now.minusHours(10));
        speaker.addBid("Guest 15", 10_300_000, now.minusHours(1));
        speaker.setWatched(true);

        AuctionLot painting = new AuctionLot(
                UUID.randomUUID().toString(),
                "Original Oil Painting",
                "Art",
                "Maison Gallery",
                "Hue",
                "Hand-finished cityscape with custom wooden frame and provenance card.",
                12_000_000,
                0,
                now.minusDays(4),
                now.plusHours(42)
        );
        painting.addBid("Huy", 12_500_000, now.minusHours(22));
        painting.addBid("Guest 02", 13_000_000, now.minusHours(8));

        AuctionLot watch = new AuctionLot(
                UUID.randomUUID().toString(),
                "Seiko Presage Open Heart",
                "Collectibles",
                "Timekeeper House",
                "Can Tho",
                "Automatic watch with sapphire crystal and full service history.",
                8_000_000,
                11_000_000,
                now.minusDays(1),
                now.plusHours(11)
        );
        watch.addBid("Phong", 8_400_000, now.minusHours(11));
        watch.addBid("Guest 20", 8_900_000, now.minusHours(6));
        watch.addBid("Nam", 9_400_000, now.minusHours(2));

        return List.of(laptop, camera, chair, speaker, painting, watch);
    }

    public enum SortMode {
        ENDING_SOON("Ending soon"),
        HIGHEST_BID("Highest bid"),
        MOST_BIDS("Most bids"),
        NEWEST("Newest");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static SortMode fromLabel(String label) {
            for (SortMode mode : values()) {
                if (mode.label.equalsIgnoreCase(label)) {
                    return mode;
                }
            }
            return ENDING_SOON;
        }
    }

    public record DashboardMetrics(
            int activeLots,
            long totalBids,
            long watchedLots,
            int endingSoon,
            double topBid
    ) {
    }

    public record ActivityEntry(
            String title,
            String description,
            LocalDateTime time,
            String tag
    ) {
    }
}
