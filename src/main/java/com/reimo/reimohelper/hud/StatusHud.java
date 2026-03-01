package com.reimo.reimohelper.hud;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.failsafe.impl.EvacuateFailsafe;
import com.reimo.reimohelper.feature.BpsTracker;
import com.reimo.reimohelper.feature.RewarpManager;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.macro.AbstractMacro;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

import java.util.ArrayList;
import java.util.List;

public class StatusHud {
    private static final Minecraft MC = Minecraft.getInstance();

    public static int getHudWidth(ReimoHelperConfig config) {
        int scale = Math.max(1, Math.min(config.statusHudScale, 4));
        return 170 + (scale - 1) * 24;
    }

    public static int getHudLineStep(ReimoHelperConfig config) {
        int scale = Math.max(1, Math.min(config.statusHudScale, 4));
        return 10 + (scale - 1) * 2;
    }

    public static int getHudHeight(ReimoHelperConfig config, int lineCount) {
        return 10 + lineCount * getHudLineStep(config);
    }

    public static List<String> getStatusLines(ReimoHelperConfig config, boolean showDebugHud) {
        MacroHandler mh = MacroHandler.getInstance();
        List<String> lines = new ArrayList<>();
        lines.add("ReimoHelper Status");
        lines.add("Macro: " + (mh.isMacroActive() ? "RUNNING" : "STOPPED"));
        lines.add("Runtime: " + formatTime(mh.getMacroRuntimeSeconds()));
        lines.add("BPS: " + String.format("%.2f", BpsTracker.getBps()));
        lines.add("Rewarp: " + (RewarpManager.getInstance().isRewarpSet() ? "Set" : "Not Set"));
        lines.add("Ungrab: " + (config.autoUngrabMouse ? "ON" : "OFF"));

        if (mh.getCurrentMacro().isPresent()) {
            AbstractMacro macro = mh.getCurrentMacro().get();
            lines.add("State: " + macro.getCurrentState().name());
        }

        if (showDebugHud && MC.player != null) {
            Vec3 d = MC.player.getDeltaMovement();
            double speed = Math.sqrt(d.x * d.x + d.z * d.z);
            lines.add("XYZ: " + MC.player.getBlockX() + ", " + MC.player.getBlockY() + ", " + MC.player.getBlockZ());
            lines.add(String.format("Speed: %.3f", speed));
            if (EvacuateFailsafe.getInstance().isAlertActive()) {
                lines.add("Alert: " + EvacuateFailsafe.getInstance().getActiveReason());
            }
        }
        return lines;
    }

    public static void onRender(CustomizeGuiOverlayEvent.Chat event) {
        if (MC.player == null) return;
        ReimoHelperConfig config = ReimoHelperConfig.getInstance();
        if (!config.showStatusHud) return;
        HudLayoutScaler.syncWithWindow(config, MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight());

        List<String> lines = getStatusLines(config, config.showDebugHud);

        GuiGraphics gg = event.getGuiGraphics();
        int x = config.statusHudX;
        int y = config.statusHudY;
        int h = getHudHeight(config, lines.size());
        int w = getHudWidth(config);
        int lineStep = getHudLineStep(config);

        gg.fill(x, y, x + w, y + h, 0xCC101722);
        gg.fill(x, y, x + w, y + 2, 0xFF2EC4B6);
        gg.fill(x, y + h - 2, x + w, y + h, 0xFF2EC4B6);

        int ty = y + 5;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFFFFFFFF : 0xFFBFD1DE;
            gg.drawString(MC.font, lines.get(i), x + 6, ty, color, false);
            ty += lineStep;
        }
    }

    private static String formatTime(long totalSeconds) {
        long h = totalSeconds / 3600L;
        long m = (totalSeconds % 3600L) / 60L;
        long s = totalSeconds % 60L;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%02dm %02ds", m, s);
    }
}
