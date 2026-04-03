package com.auctionstudio.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class AppState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<AuctionLot> lots;

    public AppState() {
        this(new ArrayList<>());
    }

    public AppState(List<AuctionLot> lots) {
        this.lots = lots;
    }

    public List<AuctionLot> getLots() {
        return lots;
    }

    public void setLots(List<AuctionLot> lots) {
        this.lots = lots;
    }
}
