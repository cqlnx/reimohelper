package com.reimo.reimohelper.feature;

import com.reimo.reimohelper.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks approximate block breaks-per-second (BPS) from the active block target.
 */
public final class BpsTracker {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Deque<Long> BREAK_TIMESTAMPS = new ArrayDeque<>();

    private static BlockPos trackedPos = null;
    private static boolean trackedWasSolid = false;

    private BpsTracker() {
    }

    public static void onClientTick() {
        long now = System.currentTimeMillis();
        cleanup(now);

        if (MC.player == null || MC.level == null || !MacroHandler.getInstance().isMacroActive()) {
            trackedPos = null;
            trackedWasSolid = false;
            return;
        }

        HitResult hit = MC.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) {
            trackedPos = null;
            trackedWasSolid = false;
            return;
        }

        BlockPos currentPos = bhr.getBlockPos();
        boolean currentSolid = !MC.level.isEmptyBlock(currentPos);

        if (trackedPos != null && trackedPos.equals(currentPos)) {
            if (trackedWasSolid && !currentSolid) {
                BREAK_TIMESTAMPS.addLast(now);
            }
            trackedWasSolid = currentSolid;
            return;
        }

        trackedPos = currentPos;
        trackedWasSolid = currentSolid;
    }

    public static double getBps() {
        long now = System.currentTimeMillis();
        cleanup(now);
        return BREAK_TIMESTAMPS.size();
    }

    private static void cleanup(long now) {
        while (!BREAK_TIMESTAMPS.isEmpty() && now - BREAK_TIMESTAMPS.peekFirst() > 1000L) {
            BREAK_TIMESTAMPS.removeFirst();
        }
    }
}
