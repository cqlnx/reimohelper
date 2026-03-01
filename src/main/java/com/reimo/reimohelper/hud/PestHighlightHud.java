package com.reimo.reimohelper.hud;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import org.joml.Vector3fc;

public class PestHighlightHud {
    private static final Minecraft MC = Minecraft.getInstance();

    public static void onRender(CustomizeGuiOverlayEvent.Chat event) {
        if (MC.player == null || MC.level == null) return;
        if (!ReimoHelperConfig.getInstance().highlightPests) return;

        int width = MC.getWindow().getGuiScaledWidth();
        int height = MC.getWindow().getGuiScaledHeight();
        int cx = width / 2;
        int cy = height / 2;
        float partialTick = event.getPartialTick();

        AABB box = MC.player.getBoundingBox().inflate(32);
        for (Entity entity : MC.level.getEntities(MC.player, box)) {
            if (!(entity instanceof Bat) && !(entity instanceof Silverfish)) continue;

            Vec3 worldPos = entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.5, 0);
            Vec3 screen = projectToScreen(worldPos, width, height, partialTick);
            if (screen == null) continue;

            int sx = (int) screen.x;
            int sy = (int) screen.y;
            int color = dynamicColor(entity.getId());

            drawBox(event, sx - 8, sy - 8, sx + 8, sy + 8, color);
            drawLine(event, cx, cy, sx, sy, color);
        }
    }

    private static Vec3 projectToScreen(Vec3 world, int width, int height, float partialTick) {
        if (MC.player == null) return null;
        if (MC.gameRenderer == null || MC.gameRenderer.getMainCamera() == null) return null;

        Vec3 camPos = MC.gameRenderer.getMainCamera().position();
        Vec3 rel = world.subtract(camPos);
        Vector3fc forward = MC.gameRenderer.getMainCamera().forwardVector();

        double forwardDot = rel.x * forward.x() + rel.y * forward.y() + rel.z * forward.z();
        if (forwardDot <= 0.01) return null;

        Vec3 ndc = MC.gameRenderer.projectPointToScreen(world);
        double sx = (ndc.x * 0.5 + 0.5) * width;
        double sy = (1.0 - (ndc.y * 0.5 + 0.5)) * height;

        if (!Double.isFinite(sx) || !Double.isFinite(sy)) return null;
        if (sx < -64 || sy < -64 || sx > width + 64 || sy > height + 64) return null;
        return new Vec3(sx, sy, ndc.z);
    }

    private static int dynamicColor(int seed) {
        float hue = ((System.currentTimeMillis() / 12L + seed * 31L) % 360L) / 360f;
        int rgb = Mth.hsvToRgb(hue, 0.9f, 1.0f);
        return 0xFF000000 | rgb;
    }

    private static void drawBox(CustomizeGuiOverlayEvent.Chat event, int x1, int y1, int x2, int y2, int color) {
        int fill = (color & 0x00FFFFFF) | 0x44000000;
        event.getGuiGraphics().fill(x1, y1, x2, y2, fill);
        event.getGuiGraphics().fill(x1, y1, x2, y1 + 2, color);
        event.getGuiGraphics().fill(x1, y2 - 2, x2, y2, color);
        event.getGuiGraphics().fill(x1, y1, x1 + 2, y2, color);
        event.getGuiGraphics().fill(x2 - 2, y1, x2, y2, color);
    }

    private static void drawLine(CustomizeGuiOverlayEvent.Chat event, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        while (true) {
            event.getGuiGraphics().fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
