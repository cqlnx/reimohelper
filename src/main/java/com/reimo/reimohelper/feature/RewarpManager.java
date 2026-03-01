package com.reimo.reimohelper.feature;

import com.reimo.reimohelper.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import com.reimo.reimohelper.config.ReimoHelperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages rewarp location and teleport commands
 */
public class RewarpManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static RewarpManager instance;
    private static final Minecraft MC = Minecraft.getInstance();
    
    private BlockPos rewarpLocation;
    private boolean rewarpSet = false;
    
    private RewarpManager() {
        ReimoHelperConfig config = ReimoHelperConfig.getInstance();
        if (config.rewarpSet) {
            this.rewarpLocation = new BlockPos(config.rewarpX, config.rewarpY, config.rewarpZ);
            this.rewarpSet = true;
            LOGGER.info("Loaded rewarp location from config: {}", this.rewarpLocation);
        }
    }
    
    public static RewarpManager getInstance() {
        if (instance == null) {
            instance = new RewarpManager();
        }
        return instance;
    }
    
    /**
     * Set rewarp location
     */
    public void setRewarpLocation(BlockPos pos) {
        this.rewarpLocation = pos;
        this.rewarpSet = true;

        ReimoHelperConfig config = ReimoHelperConfig.getInstance();
        config.rewarpSet = true;
        config.rewarpX = pos.getX();
        config.rewarpY = pos.getY();
        config.rewarpZ = pos.getZ();
        config.save();

        LOGGER.info("Rewarp location set to: {}", pos);
    }
    
    /**
     * Get rewarp location
     */
    public BlockPos getRewarpLocation() {
        return rewarpLocation;
    }
    
    /**
     * Check if rewarp location is set
     */
    public boolean isRewarpSet() {
        return rewarpSet;
    }
    
    /**
     * Execute rewarp command
     */
    public void executeRewarp(String command) {
        if (MC.player == null) {
            LOGGER.warn("Player is null, cannot execute rewarp command");
            return;
        }
        
        try {
            MacroHandler.getInstance().markTeleportExpected(8000);
            // Send command to server
            MC.player.connection.sendCommand(command.replace("/", ""));
            LOGGER.info("Rewarp command executed: {}", command);
        } catch (Exception e) {
            LOGGER.error("Failed to execute rewarp command", e);
        }
    }
    
    /**
     * Execute rewarp with config command
     */
    public void executeReward() {
        com.reimo.reimohelper.config.ReimoHelperConfig config = 
            com.reimo.reimohelper.config.ReimoHelperConfig.getInstance();
        
        if (config.autoRewarp) {
            executeRewarp(config.rewarpCommand);
        }
    }
    
    /**
     * Teleport player to rewarp location (if set)
     */
    public void teleportToRewarp() {
        if (!rewarpSet || rewarpLocation == null || MC.player == null) {
            LOGGER.warn("Cannot teleport: rewarp not set or player null");
            return;
        }
        
        try {
            MC.player.teleportTo(
                rewarpLocation.getX() + 0.5,
                rewarpLocation.getY() + 1,
                rewarpLocation.getZ() + 0.5
            );
            LOGGER.info("Player teleported to rewarp location");
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player", e);
        }
    }
}
