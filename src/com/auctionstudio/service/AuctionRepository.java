package com.auctionstudio.service;

import com.auctionstudio.model.AppState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AuctionRepository {
    private final Path dataFile;

    public AuctionRepository() {
        this(Paths.get(System.getProperty("user.home"), ".auction-studio", "auction-state.bin"));
    }

    public AuctionRepository(Path dataFile) {
        this.dataFile = dataFile;
    }

    public AppState load() {
        if (Files.notExists(dataFile)) {
            return new AppState();
        }
        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(dataFile))) {
            Object state = inputStream.readObject();
            if (state instanceof AppState appState) {
                return appState;
            }
        } catch (IOException | ClassNotFoundException ignored) {
            return new AppState();
        }
        return new AppState();
    }

    public void save(AppState state) {
        try {
            Files.createDirectories(dataFile.getParent());
            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(dataFile))) {
                outputStream.writeObject(state);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save auction data", exception);
        }
    }
}
