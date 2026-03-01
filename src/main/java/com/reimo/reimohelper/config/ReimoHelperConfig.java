package com.reimo.reimohelper.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReimoHelperConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "./config/reimohelper";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    
    private static ReimoHelperConfig instance;
    
    // Main macro config
    public String macroType = "LAYERDROP";
    public boolean macroEnabled = false;
    public float customYaw = 0;
    public float customPitch = 0;
    public boolean useCustomRotation = false;
    public long rotationSpeed = 500;
    public long breakTimeBetweenRows = 1000;
    public String rewarpCommand = "/warp garden";
    public boolean autoRewarp = true;
    public boolean autoUngrabMouse = true;
    public boolean rewarpSet = false;
    public int rewarpX = 0;
    public int rewarpY = 0;
    public int rewarpZ = 0;
    public boolean inventoryPreviewEnabled = false;
    public int inventoryPreviewX = 10;
    public int inventoryPreviewY = 10;
    public int inventoryPreviewScale = 1;
    public int gameVolumePercent = 35;
    public boolean autoEvacuateOnServerReboot = true;
    public boolean highlightPests = false;
    public boolean showStatusHud = true;
    public boolean showDebugHud = false;
    public boolean discordWebhookEnabled = false;
    public String discordWebhookUrl = "";
    public int discordWebhookIntervalSeconds = 60;
    public int statusHudX = 8;
    public int statusHudY = 56;
    public int statusHudScale = 1;
    public int hudLayoutRefWidth = 0;
    public int hudLayoutRefHeight = 0;
    
    private ReimoHelperConfig() {
    }
    
    public static ReimoHelperConfig getInstance() {
        if (instance == null) {
            instance = loadConfig();
        }
        return instance;
    }
    
    public static ReimoHelperConfig loadConfig() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.exists(configPath)) {
                String content = new String(Files.readAllBytes(configPath));
                ReimoHelperConfig config = GSON.fromJson(content, ReimoHelperConfig.class);
                if (config.macroType == null || !"LAYERDROP".equalsIgnoreCase(config.macroType)) {
                    config.macroType = "LAYERDROP";
                }
                LOGGER.info("Config loaded from {}", CONFIG_FILE);
                return config;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load config, using defaults", e);
        }
        
        return new ReimoHelperConfig();
    }
    
    public void save() {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            String json = GSON.toJson(this);
            Files.write(Paths.get(CONFIG_FILE), json.getBytes());
            LOGGER.info("Config saved to {}", CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }
    
    public enum MacroType {
        LAYERDROP("Layer Drop Pattern");
        
        private final String displayName;
        
        MacroType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
