package com.reimo.reimohelper.hud;

import com.reimo.reimohelper.config.ReimoHelperConfig;

public final class HudLayoutScaler {
    private HudLayoutScaler() {
    }

    public static void syncWithWindow(ReimoHelperConfig config, int winW, int winH) {
        if (winW <= 0 || winH <= 0) return;

        if (config.hudLayoutRefWidth <= 0 || config.hudLayoutRefHeight <= 0) {
            config.hudLayoutRefWidth = winW;
            config.hudLayoutRefHeight = winH;
            config.save();
            return;
        }

        if (config.hudLayoutRefWidth == winW && config.hudLayoutRefHeight == winH) {
            return;
        }

        double sx = (double) winW / (double) config.hudLayoutRefWidth;
        double sy = (double) winH / (double) config.hudLayoutRefHeight;
        double s = Math.sqrt(Math.max(0.01, sx * sy));

        config.inventoryPreviewX = (int) Math.round(config.inventoryPreviewX * sx);
        config.inventoryPreviewY = (int) Math.round(config.inventoryPreviewY * sy);
        config.statusHudX = (int) Math.round(config.statusHudX * sx);
        config.statusHudY = (int) Math.round(config.statusHudY * sy);
        config.inventoryPreviewScale = clamp((int) Math.round(config.inventoryPreviewScale * s), 1, 4);
        config.statusHudScale = clamp((int) Math.round(config.statusHudScale * s), 1, 4);

        config.hudLayoutRefWidth = winW;
        config.hudLayoutRefHeight = winH;
        config.save();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
