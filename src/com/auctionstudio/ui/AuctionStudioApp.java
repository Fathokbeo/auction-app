package com.auctionstudio.ui;

import com.auctionstudio.model.AuctionLot;
import com.auctionstudio.model.BidRecord;
import com.auctionstudio.model.NewAuctionRequest;
import com.auctionstudio.service.AuctionRepository;
import com.auctionstudio.service.AuctionService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.io.InputStream;

public final class AuctionStudioApp extends Application {
    private static final Color APP_BG = Color.web("#F3ECE2");
    private static final Color SURFACE = Color.web("#FFFBEF");
    private static final Color SURFACE_ALT = Color.web("#F6F1EA");
    private static final Color SIDEBAR_TOP = Color.web("#2B3343");
    private static final Color SIDEBAR_BOTTOM = Color.web("#1A1F2B");
    private static final Color ACCENT = Color.web("#C75E3A");
    private static final Color TEAL = Color.web("#367074");
    private static final Color GOLD = Color.web("#AF8437");
    private static final Color SUCCESS = Color.web("#4A7E59");
    private static final Color TEXT = Color.web("#1F2226");
    private static final Color MUTED = Color.web("#6A6C73");
    private static final Color BORDER = Color.web("#DED6CC");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final StackPane pageHost = new StackPane();
    private final AuctionService service = new AuctionService(new AuctionRepository());

    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final ComboBox<String> sortCombo = new ComboBox<>();
    private final ListView<AuctionLot> lotList = new ListView<>();
    private final Label resultLabel = new Label();
    private final Label heroSubtitle = new Label();
    private final Label heroTopBid = new Label();
    private final Label heroEndingSoon = new Label();
    private final HBox statRow = new HBox(16);
    private final FlowPane featuredPane = new FlowPane(16, 16);
    private final VBox endingSoonBox = new VBox(10);
    private final Label detailBadge = new Label("No item selected");
    private final Label detailTitle = new Label("Select an auction");
    private final Label detailCurrentBid = new Label("-");
    private final Label detailMinimumBid = new Label("-");
    private final Label detailBuyNow = new Label("-");
    private final Label detailTime = new Label("-");
    private final Label detailSeller = new Label("-");
    private final Label detailLocation = new Label("-");
    private final Label detailStatus = new Label("-");
    private final TextArea detailDescription = new TextArea();
    private final TextField bidderField = new TextField("Guest 01");
    private final TextField bidField = new TextField();
    private final Button watchButton = secondaryButton("Save item");
    private final Button bidButton = primaryButton("Place bid");
    private final Button buyNowButton = secondaryButton("Buy now");
    private final VBox historyBox = new VBox(8);
    private final VBox activityBox = new VBox(10);
    private final VBox watchlistBox = new VBox(10);
    private final TextField sellerField = new TextField("Studio Seller");
    private final TextField titleField = new TextField();
    private final ComboBox<String> listingCategoryCombo = new ComboBox<>();
    private final TextField locationField = new TextField();
    private final TextField startPriceField = new TextField();
    private final TextField buyNowPriceField = new TextField();
    private final Spinner<Integer> durationSpinner = new Spinner<>();
    private final TextArea listingDescription = new TextArea();
    private final Label listingHint = new Label("Listings are saved locally and appear instantly.");

    private Timeline ticker;
    private AuctionLot selectedLot;
    private boolean suppressEvents;
    private int ticks;

    @Override
    public void start(Stage stage) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        BorderPane root = new BorderPane();
        root.setBackground(fill(APP_BG, 0));
        root.setPadding(new Insets(18));
        root.setLeft(buildSidebar());
        BorderPane.setMargin(pageHost, new Insets(0, 0, 0, 18));

        pages.put("dashboard", buildDashboardPage());
        pages.put("market", buildMarketPage());
        pages.put("sell", buildSellPage());
        pages.put("activity", buildActivityPage());
        pageHost.getChildren().addAll(pages.values());
        root.setCenter(pageHost);

        Scene scene = new Scene(root, visualBounds.getWidth(), visualBounds.getHeight(), APP_BG);
        stage.setTitle("Auction Studio");
        stage.setMinWidth(Math.min(1240, visualBounds.getWidth()));
        stage.setMinHeight(Math.min(780, visualBounds.getHeight()));
        stage.setScene(scene);
        try (InputStream iconStream = AuctionStudioApp.class.getResourceAsStream("/icons/app-icon.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {
        }
        stage.setX(visualBounds.getMinX());
        stage.setY(visualBounds.getMinY());
        stage.setWidth(visualBounds.getWidth());
        stage.setHeight(visualBounds.getHeight());
        stage.setMaximized(true);

        wireEvents();
        refreshAll(null);
        showPage("dashboard");

        ticker = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> refreshRealtime()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
        stage.setOnCloseRequest(event -> ticker.stop());
        stage.show();
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(18);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(26, 22, 22, 22));
        sidebar.setBackground(fill(verticalGradient(SIDEBAR_TOP, SIDEBAR_BOTTOM), 32));
        sidebar.setBorder(stroke(Color.color(1, 1, 1, 0.1), 1, 32));

        VBox top = new VBox(10);
        top.getChildren().add(label("Auction", 29, FontWeight.BOLD, Color.WHITE));
        top.getChildren().add(label("Studio", 29, FontWeight.BOLD, Color.web("#F3CDBC")));
        Label subtitle = label("Desktop bidding workspace", 13, FontWeight.NORMAL, Color.web("#B9C0CC"));
        subtitle.setPadding(new Insets(0, 0, 0, 2));
        top.getChildren().add(subtitle);
        top.getChildren().add(spacer(18));
        top.getChildren().add(navButton("dashboard", "Dashboard", "Overview and featured lots"));
        top.getChildren().add(navButton("market", "Live Auctions", "Browse and place bids"));
        top.getChildren().add(navButton("sell", "Sell Item", "Create a new listing"));
        top.getChildren().add(navButton("activity", "Activity", "Recent bids and watchlist"));

        Region stretch = new Region();
        VBox.setVgrow(stretch, Priority.ALWAYS);

        VBox infoCard = cardBox(new Insets(18), new Color(1, 1, 1, 0.08), 24);
        infoCard.getChildren().add(label("Control center", 14, FontWeight.BOLD, Color.WHITE));
        infoCard.getChildren().add(spacer(10));
        infoCard.getChildren().add(wrapLabel("Pure JavaFX app with local storage and no FXML. Suitable for desktop auction demos.", 13, Color.web("#CACFD8")));
        infoCard.getChildren().add(spacer(14));
        infoCard.getChildren().add(label("Storage", 12, FontWeight.NORMAL, Color.web("#AAB1BC")));
        infoCard.getChildren().add(label("Local binary cache", 12, FontWeight.BOLD, Color.WHITE));
        infoCard.getChildren().add(spacer(10));
        infoCard.getChildren().add(label("Stack", 12, FontWeight.NORMAL, Color.web("#AAB1BC")));
        infoCard.getChildren().add(label("JDK 25 + JavaFX 25.0.2", 12, FontWeight.BOLD, Color.WHITE));

        sidebar.getChildren().addAll(top, stretch, infoCard);
        return sidebar;
    }

    private Node buildDashboardPage() {
        VBox body = new VBox(18);
        body.getChildren().add(buildHeroCard());
        body.getChildren().add(section("Auction pulse", "Live marketplace snapshot", statRow));
        body.getChildren().add(section("Featured lots", "Most active items on the floor", featuredPane));
        body.getChildren().add(section("Ending soon", "Auctions closing first", endingSoonBox));
        ScrollPane scrollPane = scroller(body);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private Node buildHeroCard() {
        BorderPane hero = new BorderPane();
        hero.setPadding(new Insets(28));
        hero.setBackground(fill(diagonalGradient(Color.web("#D67E52"), Color.web("#345866")), 34));
        hero.setBorder(stroke(Color.color(1, 1, 1, 0.2), 1, 34));

        VBox left = new VBox(8);
        left.getChildren().add(label("Live desktop marketplace", 13, FontWeight.BOLD, Color.web("#FFE9DD")));
        left.getChildren().add(label("Manage bids, listings, and auction momentum", 30, FontWeight.BOLD, Color.WHITE));
        heroSubtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 15));
        heroSubtitle.setTextFill(Color.web("#F4EDE8"));
        heroSubtitle.setWrapText(true);
        left.getChildren().add(heroSubtitle);
        left.getChildren().add(spacer(10));

        HBox actions = new HBox(12);
        Button marketButton = primaryButton("Open live auctions");
        marketButton.setOnAction(event -> showPage("market"));
        Button sellButton = secondaryButton("Create listing");
        sellButton.setOnAction(event -> showPage("sell"));
        actions.getChildren().addAll(marketButton, sellButton);
        left.getChildren().add(actions);

        VBox monitor = cardBox(new Insets(20), new Color(1, 1, 1, 0.12), 24);
        monitor.setPrefWidth(260);
        monitor.getChildren().add(label("Market monitor", 13, FontWeight.BOLD, Color.web("#FFE8DD")));
        monitor.getChildren().add(spacer(18));
        monitor.getChildren().add(metricBlock("Top live bid", heroTopBid));
        monitor.getChildren().add(spacer(14));
        monitor.getChildren().add(metricBlock("Lots ending in < 6h", heroEndingSoon));

        hero.setCenter(left);
        hero.setRight(monitor);
        return hero;
    }

    private Node buildMarketPage() {
        VBox page = new VBox(16);

        BorderPane toolbar = roundedPane(30, SURFACE);
        toolbar.setPadding(new Insets(18, 20, 18, 20));
        HBox filters = new HBox(12);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setPromptText("Search title, seller, city...");
        styleField(searchField);
        styleCombo(categoryCombo);
        styleCombo(sortCombo);
        filters.getChildren().addAll(searchField, categoryCombo, sortCombo);

        VBox help = new VBox(4);
        resultLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        resultLabel.setTextFill(TEXT);
        help.getChildren().addAll(resultLabel, label("Filter live inventory and open a lot to bid.", 12, FontWeight.NORMAL, MUTED));
        toolbar.setCenter(filters);
        toolbar.setRight(help);
        page.getChildren().add(toolbar);

        BorderPane listCard = roundedPane(30, SURFACE);
        listCard.setPadding(new Insets(20));
        listCard.setTop(header("Auction floor", "Select a lot to inspect and bid."));
        lotList.setCellFactory(listView -> new AuctionLotCell());
        lotList.setFixedCellSize(126);
        lotList.setBackground(Background.EMPTY);
        lotList.setPlaceholder(emptyState("No auction matches the current filters."));
        VBox.setVgrow(lotList, Priority.ALWAYS);
        listCard.setCenter(lotList);

        VBox detailContent = new VBox(12);
        detailContent.setPadding(new Insets(20, 22, 22, 22));
        detailContent.setBackground(fill(SURFACE, 30));
        detailContent.setBorder(stroke(Color.color(1, 1, 1, 0.6), 1, 30));

        detailBadge.setPadding(new Insets(8, 12, 8, 12));
        detailBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        detailBadge.setAlignment(Pos.CENTER);
        HBox badgeRow = new HBox(12);
        Region badgeSpacer = new Region();
        HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
        badgeRow.getChildren().addAll(detailBadge, badgeSpacer, watchButton);

        detailTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        detailTitle.setTextFill(TEXT);

        GridPane metrics = metricGrid();
        metrics.add(infoTile("Current bid", detailCurrentBid, ACCENT), 0, 0);
        metrics.add(infoTile("Minimum next bid", detailMinimumBid, TEAL), 1, 0);
        metrics.add(infoTile("Buy now", detailBuyNow, GOLD), 0, 1);
        metrics.add(infoTile("Time left", detailTime, SUCCESS), 1, 1);

        GridPane lotMeta = metricGrid(3);
        lotMeta.add(infoTile("Seller", detailSeller, TEAL), 0, 0);
        lotMeta.add(infoTile("Location", detailLocation, ACCENT), 1, 0);
        lotMeta.add(infoTile("Status", detailStatus, GOLD), 2, 0);

        detailDescription.setEditable(false);
        detailDescription.setWrapText(true);
        detailDescription.setFocusTraversable(false);
        styleArea(detailDescription);
        BorderPane descriptionCard = roundedPane(24, SURFACE_ALT);
        descriptionCard.setPadding(new Insets(16));
        descriptionCard.setTop(header("Description", "Lot notes and condition details"));
        descriptionCard.setCenter(detailDescription);

        styleField(bidderField);
        styleField(bidField);
        GridPane bidFields = new GridPane();
        bidFields.setHgap(12);
        bidFields.add(labeled("Bidder alias", bidderField), 0, 0);
        bidFields.add(labeled("Offer amount", bidField), 1, 0);

        HBox bidActions = new HBox(10, bidButton, buyNowButton);
        VBox bidBox = new VBox(14, bidFields, bidActions);
        BorderPane bidCard = roundedPane(24, SURFACE_ALT);
        bidCard.setPadding(new Insets(16));
        bidCard.setTop(header("Place a bid", "Enter your bidder alias and offer."));
        bidCard.setCenter(bidBox);

        BorderPane historyCard = roundedPane(24, SURFACE_ALT);
        historyCard.setPadding(new Insets(16));
        historyCard.setTop(header("Bid history", "Latest offers are shown first."));
        historyCard.setCenter(historyBox);

        detailContent.getChildren().addAll(badgeRow, detailTitle, metrics, lotMeta, descriptionCard, bidCard, historyCard);
        ScrollPane detailScroll = scroller(detailContent);

        SplitPane splitPane = new SplitPane(listCard, detailScroll);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.42);
        splitPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        page.getChildren().add(splitPane);
        return page;
    }

    private Node buildSellPage() {
        VBox body = new VBox(18);

        BorderPane intro = new BorderPane();
        intro.setPadding(new Insets(24));
        intro.setBackground(fill(diagonalGradient(Color.web("#2F646C"), Color.web("#C56844")), 32));
        intro.setBorder(stroke(Color.color(1, 1, 1, 0.2), 1, 32));
        intro.setTop(header("Create a listing", "Launch a new desktop auction in seconds."));
        Label note = wrapLabel("All fields below are stored locally. New lots appear immediately in the marketplace and activity feed.", 14, Color.WHITE);
        note.setPadding(new Insets(10, 0, 0, 0));
        intro.setCenter(note);
        body.getChildren().add(intro);

        HBox grid = new HBox(18);
        VBox tips = cardBox(new Insets(22), SURFACE, 30);
        tips.getChildren().add(header("What converts", "Quick checklist for stronger bids."));
        tips.getChildren().add(spacer(12));
        tips.getChildren().add(tipCard("Lead with condition", "State item quality, accessories, and service history clearly."));
        tips.getChildren().add(tipCard("Price with spread", "Keep Buy Now above the opening bid to preserve auction tension."));
        tips.getChildren().add(tipCard("Use realistic timing", "8 to 48 hours works well for desktop demos and internal workflows."));
        HBox.setHgrow(tips, Priority.ALWAYS);

        BorderPane form = roundedPane(30, SURFACE);
        form.setPadding(new Insets(22));
        form.setTop(header("Listing details", "Publish an auction lot"));
        HBox.setHgrow(form, Priority.ALWAYS);

        styleField(sellerField);
        styleField(titleField);
        styleField(locationField);
        styleField(startPriceField);
        styleField(buyNowPriceField);
        styleCombo(listingCategoryCombo);
        listingCategoryCombo.setEditable(true);
        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 720, 24, 1));
        styleSpinner(durationSpinner);
        styleArea(listingDescription);
        listingDescription.setPrefRowCount(8);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(14);
        formGrid.setVgap(14);
        formGrid.add(labeled("Seller name", sellerField), 0, 0);
        formGrid.add(labeled("Title", titleField), 1, 0);
        formGrid.add(labeled("Category", listingCategoryCombo), 0, 1);
        formGrid.add(labeled("Location", locationField), 1, 1);
        formGrid.add(labeled("Starting price", startPriceField), 0, 2);
        formGrid.add(labeled("Buy now price", buyNowPriceField), 1, 2);
        formGrid.add(labeled("Duration (hours)", durationSpinner), 0, 3);
        formGrid.add(labeled("Description", listingDescription), 1, 3);

        VBox footer = new VBox(12);
        listingHint.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        listingHint.setTextFill(MUTED);
        Button publish = primaryButton("Publish auction");
        publish.setOnAction(event -> createAuction());
        footer.getChildren().addAll(listingHint, publish);

        VBox formBody = new VBox(16, formGrid, footer);
        form.setCenter(formBody);

        grid.getChildren().addAll(tips, form);
        body.getChildren().add(grid);
        return scroller(body);
    }

    private Node buildActivityPage() {
        VBox body = new VBox(18);
        body.getChildren().add(section("Recent activity", "Listings and bids across the marketplace", activityBox));
        body.getChildren().add(section("Watchlist", "Items you flagged for follow-up", watchlistBox));
        return scroller(body);
    }

    private void wireEvents() {
        for (AuctionService.SortMode mode : AuctionService.SortMode.values()) {
            sortCombo.getItems().add(mode.label());
        }
        sortCombo.getSelectionModel().select(AuctionService.SortMode.ENDING_SOON.label());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshFromFilters());
        categoryCombo.valueProperty().addListener((observable, oldValue, newValue) -> refreshFromFilters());
        sortCombo.valueProperty().addListener((observable, oldValue, newValue) -> refreshFromFilters());
        lotList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedLot = newValue;
            updateDetail(newValue);
            if (newValue != null) {
                bidField.setText(formatPlain(newValue.getMinimumNextBid()));
            }
        });
        watchButton.setOnAction(event -> toggleWatch());
        bidButton.setOnAction(event -> placeBid());
        buyNowButton.setOnAction(event -> buyNow());
    }

    private void refreshAll(String preferredLotId) {
        refreshCategories();
        refreshListingCategories();
        refreshDashboard();
        refreshMarket(preferredLotId);
        refreshActivity();
    }

    private void refreshRealtime() {
        ticks++;
        lotList.refresh();
        if (selectedLot != null) {
            selectedLot = service.findById(selectedLot.getId());
        }
        updateDetail(selectedLot);
        refreshDashboard();
        if (ticks % 10 == 0) {
            refreshActivity();
        }
    }

    private void showPage(String pageKey) {
        pages.forEach((key, node) -> {
            boolean active = key.equals(pageKey);
            node.setVisible(active);
            node.setManaged(active);
        });
        navButtons.forEach((key, button) -> button.setActive(key.equals(pageKey)));
    }

    private void showMessage(Alert.AlertType type, String text) {
        Alert alert = new Alert(type, text, ButtonType.OK);
        alert.setHeaderText("Auction Studio");
        alert.showAndWait();
    }

    private NavButton navButton(String key, String title, String subtitle) {
        NavButton button = new NavButton(title, subtitle);
        button.setOnAction(event -> showPage(key));
        navButtons.put(key, button);
        return button;
    }

    private Node section(String title, String subtitle, Node content) {
        BorderPane pane = roundedPane(30, SURFACE);
        pane.setPadding(new Insets(20));
        pane.setTop(header(title, subtitle));
        pane.setCenter(content);
        return pane;
    }

    private VBox header(String title, String subtitle) {
        VBox box = new VBox(4);
        box.getChildren().add(label(title, 18, FontWeight.BOLD, TEXT));
        box.getChildren().add(label(subtitle, 13, FontWeight.NORMAL, MUTED));
        return box;
    }

    private Node metricBlock(String title, Label valueLabel) {
        VBox box = new VBox(4);
        box.getChildren().add(label(title, 12, FontWeight.NORMAL, Color.web("#D9DDE4")));
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        valueLabel.setTextFill(Color.WHITE);
        box.getChildren().add(valueLabel);
        return box;
    }

    private void refreshFromFilters() {
        if (!suppressEvents) {
            refreshMarket(selectedLot == null ? null : selectedLot.getId());
        }
    }

    private void refreshCategories() {
        String current = Objects.toString(categoryCombo.getValue(), "All");
        suppressEvents = true;
        categoryCombo.getItems().setAll(service.getCategories());
        if (service.getCategories().contains(current)) {
            categoryCombo.setValue(current);
        } else {
            categoryCombo.setValue("All");
        }
        suppressEvents = false;
    }

    private void refreshListingCategories() {
        String current = Objects.toString(listingCategoryCombo.getValue(), "Tech");
        suppressEvents = true;
        listingCategoryCombo.getItems().setAll(service.getCategories().stream().filter(item -> !"All".equals(item)).toList());
        if (listingCategoryCombo.getItems().isEmpty()) {
            listingCategoryCombo.getItems().add("Tech");
        }
        if (listingCategoryCombo.getItems().contains(current)) {
            listingCategoryCombo.setValue(current);
        } else {
            listingCategoryCombo.setValue(listingCategoryCombo.getItems().get(0));
        }
        suppressEvents = false;
    }

    private void refreshDashboard() {
        AuctionService.DashboardMetrics metrics = service.getDashboardMetrics();
        heroSubtitle.setText(metrics.activeLots() + " active lots, " + metrics.totalBids() + " bids, " + metrics.watchedLots() + " watched items.");
        heroTopBid.setText(AuctionService.formatCurrency(metrics.topBid()));
        heroEndingSoon.setText(String.valueOf(metrics.endingSoon()));

        statRow.getChildren().setAll(
                statCard("Active lots", String.valueOf(metrics.activeLots()), "Marketplace inventory live right now", ACCENT),
                statCard("Total bids", String.valueOf(metrics.totalBids()), "Confirmed offers recorded locally", TEAL),
                statCard("Watched lots", String.valueOf(metrics.watchedLots()), "Saved for follow-up or review", GOLD),
                statCard("Ending soon", String.valueOf(metrics.endingSoon()), "Closing inside the next 6 hours", SUCCESS)
        );

        featuredPane.getChildren().clear();
        service.getFeaturedLots(3).forEach(lot -> featuredPane.getChildren().add(featuredCard(lot)));

        endingSoonBox.getChildren().clear();
        List<AuctionLot> ending = service.getEndingSoon(5);
        if (ending.isEmpty()) {
            endingSoonBox.getChildren().add(emptyState("No live auctions are ending soon."));
        } else {
            ending.forEach(lot -> endingSoonBox.getChildren().add(auctionRow(lot, "Open lot")));
        }
    }

    private void refreshMarket(String preferredLotId) {
        AuctionService.SortMode mode = AuctionService.SortMode.fromLabel(Objects.toString(sortCombo.getValue(), "Ending soon"));
        List<AuctionLot> lots = service.filterLots(searchField.getText(), Objects.toString(categoryCombo.getValue(), "All"), mode);
        resultLabel.setText(lots.size() + " results in view");
        lotList.getItems().setAll(lots);

        if (lots.isEmpty()) {
            selectedLot = null;
            updateDetail(null);
            return;
        }

        AuctionLot preferred = preferredLotId == null ? null : service.findById(preferredLotId);
        if (preferred != null && lots.contains(preferred)) {
            lotList.getSelectionModel().select(preferred);
        } else if (selectedLot != null && lots.contains(selectedLot)) {
            lotList.getSelectionModel().select(selectedLot);
        } else {
            lotList.getSelectionModel().selectFirst();
        }
    }

    private void refreshActivity() {
        activityBox.getChildren().clear();
        List<AuctionService.ActivityEntry> entries = service.getRecentActivity(12);
        if (entries.isEmpty()) {
            activityBox.getChildren().add(emptyState("No activity recorded yet."));
        } else {
            entries.forEach(entry -> activityBox.getChildren().add(activityCard(entry)));
        }

        watchlistBox.getChildren().clear();
        List<AuctionLot> watched = service.getWatchlist();
        if (watched.isEmpty()) {
            watchlistBox.getChildren().add(emptyState("Your watchlist is empty. Save an item from the detail panel."));
        } else {
            watched.forEach(lot -> watchlistBox.getChildren().add(auctionRow(lot, "Review")));
        }
    }

    private void updateDetail(AuctionLot lot) {
        if (lot == null) {
            detailBadge.setText("No live selection");
            detailBadge.setBackground(fill(Color.web("#ECCDC1"), 16));
            detailBadge.setTextFill(ACCENT.darker());
            detailTitle.setText("Select an auction lot from the left panel");
            detailCurrentBid.setText("-");
            detailMinimumBid.setText("-");
            detailBuyNow.setText("-");
            detailTime.setText("-");
            detailSeller.setText("-");
            detailLocation.setText("-");
            detailStatus.setText("-");
            detailDescription.setText("Use the list to inspect auction details, review bid history, and place offers.");
            watchButton.setDisable(true);
            bidButton.setDisable(true);
            buyNowButton.setDisable(true);
            historyBox.getChildren().setAll(emptyState("Bid activity will appear here once an auction is selected."));
            return;
        }

        Color tone = lotColor(lot);
        detailBadge.setText(lot.getCategory());
        detailBadge.setBackground(fill(bright(tone, 0.74), 16));
        detailBadge.setTextFill(tone.darker());
        detailTitle.setText(lot.getTitle());
        detailCurrentBid.setText(AuctionService.formatCurrency(lot.getCurrentBid()));
        detailMinimumBid.setText(AuctionService.formatCurrency(lot.getMinimumNextBid()));
        detailBuyNow.setText(lot.hasBuyNow() ? AuctionService.formatCurrency(lot.getBuyNowPrice()) : "Not set");
        detailTime.setText(remaining(lot));
        detailSeller.setText(lot.getSellerName());
        detailLocation.setText(lot.getLocation());
        detailStatus.setText(lot.isActive() ? "Live until " + DATE_FORMAT.format(lot.getEndTime()) : "Closed");
        detailDescription.setText(lot.getDescription());
        watchButton.setDisable(false);
        watchButton.setText(lot.isWatched() ? "Remove from watchlist" : "Save item");
        bidButton.setDisable(!lot.isActive());
        buyNowButton.setDisable(!(lot.isActive() && lot.hasBuyNow()));

        historyBox.getChildren().clear();
        if (lot.getBidHistory().isEmpty()) {
            historyBox.getChildren().add(emptyState("No bids yet. Be the first bidder on this lot."));
        } else {
            lot.getBidHistory().forEach(bid -> historyBox.getChildren().add(historyRow(bid)));
        }
    }

    private void toggleWatch() {
        if (selectedLot == null) {
            return;
        }
        service.toggleWatch(selectedLot.getId());
        refreshAll(selectedLot.getId());
    }

    private void placeBid() {
        if (selectedLot == null) {
            return;
        }
        try {
            service.placeBid(selectedLot.getId(), bidderField.getText(), parseAmount(bidField.getText()));
            refreshAll(selectedLot.getId());
            AuctionLot current = service.findById(selectedLot.getId());
            if (current != null) {
                bidField.setText(formatPlain(current.getMinimumNextBid()));
            }
            showMessage(Alert.AlertType.INFORMATION, "Bid accepted for " + selectedLot.getTitle() + ".");
        } catch (IllegalArgumentException exception) {
            showMessage(Alert.AlertType.WARNING, exception.getMessage());
        }
    }

    private void buyNow() {
        if (selectedLot == null || !selectedLot.hasBuyNow()) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Close the auction instantly at " +
                AuctionService.formatCurrency(selectedLot.getBuyNowPrice()) + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Buy Now");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            service.buyNow(selectedLot.getId(), bidderField.getText());
            refreshAll(selectedLot.getId());
            showMessage(Alert.AlertType.INFORMATION, "Auction closed at Buy Now price.");
        } catch (IllegalArgumentException exception) {
            showMessage(Alert.AlertType.WARNING, exception.getMessage());
        }
    }

    private void createAuction() {
        try {
            String category = listingCategoryCombo.isEditable()
                    ? listingCategoryCombo.getEditor().getText().trim()
                    : Objects.toString(listingCategoryCombo.getValue(), "").trim();
            AuctionLot created = service.createAuction(new NewAuctionRequest(
                    titleField.getText(),
                    category,
                    sellerField.getText(),
                    locationField.getText(),
                    listingDescription.getText(),
                    parseAmount(startPriceField.getText()),
                    buyNowPriceField.getText().isBlank() ? 0 : parseAmount(buyNowPriceField.getText()),
                    durationSpinner.getValue()
            ));
            clearListingForm();
            refreshAll(created.getId());
            openLot(created);
            showMessage(Alert.AlertType.INFORMATION, "Auction created successfully.");
        } catch (IllegalArgumentException exception) {
            showMessage(Alert.AlertType.WARNING, exception.getMessage());
        }
    }

    private void clearListingForm() {
        titleField.clear();
        locationField.clear();
        startPriceField.clear();
        buyNowPriceField.clear();
        listingDescription.clear();
        durationSpinner.getValueFactory().setValue(24);
        listingHint.setText("Auction published. You can review it in Live Auctions.");
    }

    private void openLot(AuctionLot lot) {
        showPage("market");
        refreshMarket(lot.getId());
        lotList.getSelectionModel().select(lot);
    }

    private GridPane metricGrid() {
        return metricGrid(2);
    }

    private GridPane metricGrid(int columns) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int i = 0; i < columns; i++) {
            javafx.scene.layout.ColumnConstraints constraints = new javafx.scene.layout.ColumnConstraints();
            constraints.setPercentWidth(100.0 / columns);
            constraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(constraints);
        }
        return grid;
    }

    private Node infoTile(String title, Label valueLabel, Color accentColor) {
        VBox tile = cardBox(new Insets(16), SURFACE_ALT, 22);
        tile.setBorder(new Border(new BorderStroke(
                accentColor,
                BorderStrokeStyle.SOLID,
                new CornerRadii(22),
                new BorderWidths(0, 0, 0, 4)
        )));
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        valueLabel.setTextFill(TEXT);
        valueLabel.setWrapText(true);
        tile.getChildren().add(label(title, 12, FontWeight.NORMAL, MUTED));
        tile.getChildren().add(spacer(8));
        tile.getChildren().add(valueLabel);
        return tile;
    }

    private Node labeled(String title, Node field) {
        VBox box = new VBox(6);
        box.getChildren().add(label(title, 12, FontWeight.BOLD, TEXT));
        box.getChildren().add(field);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private Node tipCard(String title, String text) {
        VBox card = cardBox(new Insets(16), SURFACE_ALT, 22);
        card.getChildren().add(label(title, 14, FontWeight.BOLD, TEXT));
        card.getChildren().add(spacer(6));
        card.getChildren().add(wrapLabel(text, 13, MUTED));
        return card;
    }

    private Node statCard(String title, String value, String subtitle, Color accentColor) {
        VBox card = cardBox(new Insets(18), SURFACE_ALT, 24);
        card.setPrefWidth(260);
        card.setBorder(new Border(new BorderStroke(
                accentColor,
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(0, 0, 0, 4)
        )));
        card.getChildren().add(label(title, 13, FontWeight.BOLD, MUTED));
        card.getChildren().add(spacer(10));
        card.getChildren().add(label(value, 28, FontWeight.BOLD, TEXT));
        card.getChildren().add(spacer(8));
        card.getChildren().add(wrapLabel(subtitle, 12, MUTED));
        return card;
    }

    private Node featuredCard(AuctionLot lot) {
        VBox card = cardBox(new Insets(18), SURFACE_ALT, 26);
        card.setPrefWidth(320);
        card.getChildren().add(pill(lot.getCategory(), bright(lotColor(lot), 0.78), lotColor(lot).darker()));
        card.getChildren().add(spacer(14));
        card.getChildren().add(label(lot.getTitle(), 18, FontWeight.BOLD, TEXT));
        card.getChildren().add(spacer(8));
        card.getChildren().add(wrapLabel(shorten(lot.getDescription(), 92), 12, MUTED));
        Region stretch = new Region();
        VBox.setVgrow(stretch, Priority.ALWAYS);
        card.getChildren().add(stretch);
        card.getChildren().add(label(AuctionService.formatCurrency(lot.getCurrentBid()), 20, FontWeight.BOLD, TEXT));
        card.getChildren().add(spacer(4));
        card.getChildren().add(label(remaining(lot) + "  |  " + lot.getBidCount() + " bids", 12, FontWeight.NORMAL, MUTED));
        card.getChildren().add(spacer(14));
        Button inspect = primaryButton("Inspect");
        inspect.setOnAction(event -> openLot(lot));
        card.getChildren().add(inspect);
        return card;
    }

    private Node auctionRow(AuctionLot lot, String buttonText) {
        BorderPane row = roundedPane(22, SURFACE_ALT);
        row.setPadding(new Insets(16));
        VBox info = new VBox(4);
        info.getChildren().add(label(lot.getTitle(), 15, FontWeight.BOLD, TEXT));
        info.getChildren().add(label(lot.getCategory() + "  |  " + lot.getLocation(), 12, FontWeight.NORMAL, MUTED));
        info.getChildren().add(spacer(2));
        info.getChildren().add(label(AuctionService.formatCurrency(lot.getCurrentBid()) + "  |  " + remaining(lot), 13, FontWeight.BOLD, lotColor(lot).darker()));
        Button button = secondaryButton(buttonText);
        button.setOnAction(event -> openLot(lot));
        row.setCenter(info);
        row.setRight(button);
        return row;
    }

    private Node activityCard(AuctionService.ActivityEntry entry) {
        BorderPane card = roundedPane(22, SURFACE_ALT);
        card.setPadding(new Insets(14, 16, 14, 16));
        Label tag = pill(entry.tag(), entry.tag().equals("BID") ? bright(ACCENT, 0.78) : bright(TEAL, 0.78),
                entry.tag().equals("BID") ? ACCENT.darker() : TEAL.darker());
        tag.setPrefWidth(80);
        VBox center = new VBox(4);
        center.getChildren().add(label(entry.title(), 14, FontWeight.BOLD, TEXT));
        center.getChildren().add(wrapLabel(entry.description(), 12, MUTED));
        card.setLeft(tag);
        BorderPane.setMargin(tag, new Insets(0, 12, 0, 0));
        card.setCenter(center);
        card.setRight(label(DATE_FORMAT.format(entry.time()), 12, FontWeight.NORMAL, MUTED));
        return card;
    }

    private Node historyRow(BidRecord bid) {
        BorderPane row = roundedPane(20, Color.WHITE);
        row.setPadding(new Insets(12));
        VBox left = new VBox(4);
        left.getChildren().add(label(bid.getBidderName(), 13, FontWeight.BOLD, TEXT));
        left.getChildren().add(label(DATE_FORMAT.format(bid.getTimestamp()), 12, FontWeight.NORMAL, MUTED));
        row.setLeft(left);
        row.setRight(label(AuctionService.formatCurrency(bid.getAmount()), 13, FontWeight.BOLD, ACCENT.darker()));
        return row;
    }

    private Label emptyState(String text) {
        Label label = wrapLabel(text, 13, MUTED);
        label.setPadding(new Insets(16));
        label.setBackground(fill(Color.WHITE, 22));
        label.setBorder(stroke(Color.color(0, 0, 0, 0.03), 1, 22));
        return label;
    }

    private static VBox cardBox(Insets padding, Paint fill, double radius) {
        VBox box = new VBox();
        box.setPadding(padding);
        box.setBackground(background(fill, radius));
        box.setBorder(border(Color.color(1, 1, 1, 0.6), 1, radius));
        return box;
    }

    private static BorderPane roundedPane(double radius, Paint fill) {
        BorderPane pane = new BorderPane();
        pane.setBackground(background(fill, radius));
        pane.setBorder(border(Color.color(1, 1, 1, 0.6), 1, radius));
        return pane;
    }

    private static Label label(String text, int size, FontWeight weight, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", weight, size));
        label.setTextFill(color);
        return label;
    }

    private static Label wrapLabel(String text, int size, Color color) {
        Label label = label(text, size, FontWeight.NORMAL, color);
        label.setWrapText(true);
        return label;
    }

    private static Label pill(String text, Paint background, Color foreground) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        label.setTextFill(foreground);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(8, 12, 8, 12));
        label.setBackground(background(background, 16));
        return label;
    }

    private static Region spacer(double height) {
        Region region = new Region();
        region.setMinHeight(height);
        region.setPrefHeight(height);
        return region;
    }

    private static ScrollPane scroller(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private static void styleField(TextField field) {
        styleInput(field);
        field.setBackground(fill(Color.WHITE, 16));
        field.setBorder(border(BORDER, 1, 16));
        field.setPadding(new Insets(12, 14, 12, 14));
        field.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
    }

    private static void styleArea(TextArea area) {
        styleInput(area);
        area.setBackground(fill(Color.WHITE, 16));
        area.setBorder(border(BORDER, 1, 16));
        area.setPadding(new Insets(12, 14, 12, 14));
        area.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
    }

    private static void styleInput(TextInputControl control) {
        control.setStyle("-fx-highlight-fill: #EED2C7; -fx-highlight-text-fill: #1F2226;");
    }

    private static void styleCombo(ComboBox<String> comboBox) {
        comboBox.setBackground(fill(Color.WHITE, 16));
        comboBox.setBorder(border(BORDER, 1, 16));
        comboBox.setPadding(new Insets(6, 10, 6, 10));
        comboBox.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
        comboBox.setPrefWidth(180);
    }

    private static void styleSpinner(Spinner<Integer> spinner) {
        spinner.setBackground(fill(Color.WHITE, 16));
        spinner.setBorder(border(BORDER, 1, 16));
        spinner.setPadding(new Insets(4, 8, 4, 8));
        spinner.getEditor().setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
    }

    private static Button primaryButton(String text) {
        Button button = baseButton(text);
        button.setTextFill(Color.WHITE);
        button.setBackground(fill(ACCENT, 14));
        button.setBorder(border(Color.web("#AA4826"), 1, 14));
        return button;
    }

    private static Button secondaryButton(String text) {
        Button button = baseButton(text);
        button.setTextFill(TEXT);
        button.setBackground(fill(Color.web("#EFE3DA"), 14));
        button.setBorder(border(BORDER, 1, 14));
        return button;
    }

    private static Button baseButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setFocusTraversable(false);
        return button;
    }

    private static Background fill(Paint paint, double radius) {
        return background(paint, radius);
    }

    private static Background background(Paint paint, double radius) {
        return new Background(new BackgroundFill(paint, new CornerRadii(radius), Insets.EMPTY));
    }

    private static Border stroke(Paint paint, double width, double radius) {
        return border(paint, width, radius);
    }

    private static Border border(Paint paint, double width, double radius) {
        return new Border(new BorderStroke(paint, BorderStrokeStyle.SOLID, new CornerRadii(radius), new BorderWidths(width)));
    }

    private static LinearGradient diagonalGradient(Color start, Color end) {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, start), new Stop(1, end));
    }

    private static LinearGradient verticalGradient(Color start, Color end) {
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, start), new Stop(1, end));
    }

    private static double parseAmount(String text) {
        String cleaned = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Enter a numeric amount.");
        }
        return Double.parseDouble(cleaned);
    }

    private static String formatPlain(double value) {
        return String.format(Locale.US, "%,.0f", value);
    }

    private static Color bright(Color base, double ratio) {
        return new Color(
                Math.min(1, base.getRed() + (1 - base.getRed()) * ratio),
                Math.min(1, base.getGreen() + (1 - base.getGreen()) * ratio),
                Math.min(1, base.getBlue() + (1 - base.getBlue()) * ratio),
                1
        );
    }

    private static Color lotColor(AuctionLot lot) {
        return switch (lot.getCategory().toLowerCase(Locale.ROOT)) {
            case "tech", "gaming" -> ACCENT;
            case "camera", "audio" -> TEAL;
            case "furniture" -> GOLD;
            case "art" -> Color.web("#795395");
            case "collectibles", "fashion" -> SUCCESS;
            default -> ACCENT;
        };
    }

    private static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String remaining(AuctionLot lot) {
        if (!lot.isActive()) {
            return "Closed";
        }
        Duration duration = Duration.between(LocalDateTime.now(), lot.getEndTime());
        long totalMinutes = Math.max(duration.toMinutes(), 0);
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            return days + "d " + hours + "h left";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m left";
        }
        return minutes + "m left";
    }

    private final class AuctionLotCell extends ListCell<AuctionLot> {
        @Override
        protected void updateItem(AuctionLot lot, boolean empty) {
            super.updateItem(lot, empty);
            if (empty || lot == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            HBox root = new HBox(12);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(12, 14, 12, 14));
            root.setBackground(fill(isSelected() ? bright(lotColor(lot), 0.82) : SURFACE_ALT, 24));
            root.setBorder(border(isSelected() ? lotColor(lot) : Color.web("#E4DCD4"), 1, 24));

            VBox center = new VBox(6);
            Label title = label(lot.getTitle(), 16, FontWeight.BOLD, TEXT);
            Label meta = label(lot.getCategory() + "  |  " + lot.getLocation() + "  |  " + lot.getBidCount() + " bids", 12, FontWeight.NORMAL, MUTED);
            Label price = label(AuctionService.formatCurrency(lot.getCurrentBid()), 14, FontWeight.BOLD, TEXT);
            center.getChildren().addAll(title, meta, price);
            HBox.setHgrow(center, Priority.ALWAYS);

            VBox right = new VBox(10);
            right.setAlignment(Pos.CENTER_RIGHT);
            Label status = label(remaining(lot), 12, FontWeight.BOLD, lot.isActive() ? lotColor(lot).darker() : MUTED);
            Label marker = label(lot.isWatched() ? "WATCHED" : "LIVE", 11, FontWeight.BOLD, lot.isWatched() ? GOLD.darker() : TEAL.darker());
            right.getChildren().addAll(status, marker);

            root.getChildren().addAll(center, right);
            setGraphic(root);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setBackground(Background.EMPTY);
        }
    }

    private static final class NavButton extends Button {
        private final Label titleLabel;
        private final Label subtitleLabel;

        private NavButton(String title, String subtitle) {
            titleLabel = label(title, 15, FontWeight.BOLD, Color.web("#DBDFE6"));
            subtitleLabel = label(subtitle, 12, FontWeight.NORMAL, Color.web("#959DAA"));
            VBox content = new VBox(2, titleLabel, subtitleLabel);
            content.setAlignment(Pos.CENTER_LEFT);
            setGraphic(content);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setMaxWidth(Double.MAX_VALUE);
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(false);
            setPadding(new Insets(14));
            setBackground(fill(Color.TRANSPARENT, 18));
            setBorder(border(Color.color(1, 1, 1, 0.06), 1, 18));
        }

        private void setActive(boolean active) {
            setBackground(fill(active ? Color.color(1, 1, 1, 0.12) : Color.TRANSPARENT, 18));
            setBorder(border(active ? Color.color(1, 1, 1, 0.14) : Color.color(1, 1, 1, 0.06), 1, 18));
            titleLabel.setTextFill(active ? Color.WHITE : Color.web("#DBDFE6"));
            subtitleLabel.setTextFill(active ? Color.web("#E9CDC2") : Color.web("#959DAA"));
        }
    }
}
