package com.reimo.reimohelper.hud;

import com.reimo.reimohelper.feature.RewarpManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import org.joml.Vector3fc;

public class RewarpHighlightHud {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int OUTLINE_COLOR = 0xFF00E5FF;

    public static void onRender(CustomizeGuiOverlayEvent.Chat event) {
        if (MC.player == null || MC.level == null || MC.gameRenderer == null || MC.gameRenderer.getMainCamera() == null) {
            return;
        }

        RewarpManager manager = RewarpManager.getInstance();
        if (!manager.isRewarpSet()) {
            return;
        }

        BlockPos pos = manager.getRewarpLocation();
        if (pos == null) {
            return;
        }

        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double dx = MC.player.getX() - cx;
        double dz = MC.player.getZ() - cz;
        if ((dx * dx + dz * dz) > (120.0 * 120.0)) {
            return;
        }

        int width = MC.getWindow().getGuiScaledWidth();
        int height = MC.getWindow().getGuiScaledHeight();

        Vec3[] corners = new Vec3[]{
                new Vec3(pos.getX(), pos.getY(), pos.getZ()),
                new Vec3(pos.getX() + 1, pos.getY(), pos.getZ()),
                new Vec3(pos.getX() + 1, pos.getY(), pos.getZ() + 1),
                new Vec3(pos.getX(), pos.getY(), pos.getZ() + 1),
                new Vec3(pos.getX(), pos.getY() + 1, pos.getZ()),
                new Vec3(pos.getX() + 1, pos.getY() + 1, pos.getZ()),
                new Vec3(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                new Vec3(pos.getX(), pos.getY() + 1, pos.getZ() + 1)
        };

        Vec3[] projected = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            projected[i] = projectToScreen(corners[i], width, height);
        }

        // bottom square
        drawEdge(event, projected[0], projected[1]);
        drawEdge(event, projected[1], projected[2]);
        drawEdge(event, projected[2], projected[3]);
        drawEdge(event, projected[3], projected[0]);
        // top square
        drawEdge(event, projected[4], projected[5]);
        drawEdge(event, projected[5], projected[6]);
        drawEdge(event, projected[6], projected[7]);
        drawEdge(event, projected[7], projected[4]);
        // verticals
        drawEdge(event, projected[0], projected[4]);
        drawEdge(event, projected[1], projected[5]);
        drawEdge(event, projected[2], projected[6]);
        drawEdge(event, projected[3], projected[7]);
    }

    private static Vec3 projectToScreen(Vec3 world, int width, int height) {
        Vec3 camPos = MC.gameRenderer.getMainCamera().position();
        Vec3 rel = world.subtract(camPos);
        Vector3fc forward = MC.gameRenderer.getMainCamera().forwardVector();
        double forwardDot = rel.x * forward.x() + rel.y * forward.y() + rel.z * forward.z();
        if (forwardDot <= 0.01) {
            return null;
        }

        Vec3 ndc = MC.gameRenderer.projectPointToScreen(world);
        double sx = (ndc.x * 0.5 + 0.5) * width;
        double sy = (1.0 - (ndc.y * 0.5 + 0.5)) * height;

        if (!Double.isFinite(sx) || !Double.isFinite(sy)) return null;
        if (sx < -64 || sy < -64 || sx > width + 64 || sy > height + 64) return null;
        return new Vec3(sx, sy, ndc.z);
    }

    private static void drawEdge(CustomizeGuiOverlayEvent.Chat event, Vec3 a, Vec3 b) {
        if (a == null || b == null) return;
        drawLine(event, (int) a.x, (int) a.y, (int) b.x, (int) b.y, OUTLINE_COLOR);
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
            event.getGuiGraphics().fill(x, y, x + 2, y + 2, color);
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
