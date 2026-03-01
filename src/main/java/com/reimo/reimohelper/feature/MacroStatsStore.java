package com.reimo.reimohelper.feature;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MacroStatsStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = Paths.get("./config/reimohelper");
    private static final Path FILE = DIR.resolve("stats.json");

    private static MacroStatsStore instance;

    public static final class StatsData {
        public long totalMacroSeconds = 0L;
        public int totalMacroSessions = 0;
        public String lastPlayerName = "";
    }

    private final StatsData data;

    private MacroStatsStore() {
        data = load();
    }

    public static synchronized MacroStatsStore getInstance() {
        if (instance == null) {
            instance = new MacroStatsStore();
        }
        return instance;
    }

    public synchronized void onMacroStart(String playerName) {
        data.totalMacroSessions += 1;
        if (playerName != null && !playerName.isBlank()) {
            data.lastPlayerName = playerName.trim();
        }
        save();
    }

    public synchronized void onMacroStop(long runtimeSeconds) {
        if (runtimeSeconds > 0L) {
            data.totalMacroSeconds += runtimeSeconds;
            save();
        }
    }

    public synchronized long getTotalMacroSeconds() {
        return data.totalMacroSeconds;
    }

    public synchronized int getTotalMacroSessions() {
        return data.totalMacroSessions;
    }

    public synchronized String getLastPlayerName() {
        return data.lastPlayerName == null ? "" : data.lastPlayerName;
    }

    private StatsData load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE);
                StatsData loaded = GSON.fromJson(json, StatsData.class);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load stats.json, using defaults", e);
        }
        return new StatsData();
    }

    private void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, GSON.toJson(data));
        } catch (Exception e) {
            LOGGER.warn("Failed to save stats.json", e);
        }
    }
}
