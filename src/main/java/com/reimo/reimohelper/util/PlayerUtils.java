package com.reimo.reimohelper.util;

import com.reimo.reimohelper.feature.RewarpManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Utility class for player-related operations
 */
public class PlayerUtils {
    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * Check if rewarp location is set
     */
    public static boolean isRewarpLocationSet() {
        return RewarpManager.getInstance().isRewarpSet();
    }

    /**
     * Check if standing on rewarp location
     */
    public static boolean isStandingOnRewarpLocation() {
        if (MC.player == null || MC.level == null) {
            return false;
        }

        BlockPos rewarp = RewarpManager.getInstance().getRewarpLocation();
        if (rewarp == null) {
            return false;
        }

        return MC.player.blockPosition().equals(rewarp);
    }

    /**
     * Check if player is within configurable zone around rewarp location.
     */
    public static boolean isInRewarpZone(double horizontalRadius, int verticalTolerance) {
        if (MC.player == null || MC.level == null) {
            return false;
        }

        BlockPos rewarp = RewarpManager.getInstance().getRewarpLocation();
        if (rewarp == null) {
            return false;
        }

        double centerX = rewarp.getX() + 0.5;
        double centerZ = rewarp.getZ() + 0.5;
        double dx = MC.player.getX() - centerX;
        double dz = MC.player.getZ() - centerZ;
        int dy = Math.abs(MC.player.getBlockY() - rewarp.getY());

        return (dx * dx + dz * dz) <= (horizontalRadius * horizontalRadius) && dy <= verticalTolerance;
    }

    /**
     * Check if standing on spawn point
     */
    public static boolean isStandingOnSpawnPoint() {
        return false; // Placeholder
    }

    /**
     * Get farming crop based on mouse over
     */
    public static CropType getCropBasedOnMouseOver() {
        return CropType.WHEAT;
    }

    /**
     * Get farming crop
     */
    public static CropType getFarmingCrop() {
        return CropType.WHEAT;
    }

    /**
     * Get tool player is holding
     */
    public static void getTool() {
        // Implementation for getting tool
    }

    /**
     * Enum for crop types
     */
    public enum CropType {
        WHEAT,
        SUGARCANE,
        MUSHROOM,
        COCOA,
        MELON,
        PUMPKIN,
        NONE
    }
}
