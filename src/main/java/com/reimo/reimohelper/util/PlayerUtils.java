package com.reimo.reimohelper.util;

import com.reimo.reimohelper.feature.RewarpManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class PlayerUtils {
    private static final Minecraft MC = Minecraft.getInstance();

    public static boolean isRewarpLocationSet() {
        return RewarpManager.getInstance().isRewarpSet();
    }

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

    public static boolean isStandingOnSpawnPoint() {
        return false;
    }

    public static CropType getCropBasedOnMouseOver() {
        return CropType.WHEAT;
    }

    public static CropType getFarmingCrop() {
        return CropType.WHEAT;
    }

    public static void getTool() {
        // Implementation for getting tool
    }

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
