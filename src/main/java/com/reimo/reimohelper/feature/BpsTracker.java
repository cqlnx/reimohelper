package com.reimo.reimohelper.feature;

import com.reimo.reimohelper.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks block breaks-per-second (BPS) by monitoring all blocks in vicinity.
 */
public final class BpsTracker {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Deque<Long> BREAK_TIMESTAMPS = new ArrayDeque<>();
    private static final Map<BlockPos, Boolean> previousBlockStates = new HashMap<>();

    private BpsTracker() {
    }

    public static void onClientTick() {
        long now = System.currentTimeMillis();
        cleanup(now);

        if (MC.player == null || MC.level == null || !MacroHandler.getInstance().isMacroActive()) {
            previousBlockStates.clear();
            return;
        }

        HitResult hit = MC.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) {
            previousBlockStates.clear();
            return;
        }

        BlockPos hitPos = bhr.getBlockPos();
        
        // Check a 5x5x5 cube around the hit position for block changes
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = hitPos.offset(dx, dy, dz);
                    boolean currentSolid = !MC.level.isEmptyBlock(checkPos);
                    
                    Boolean prevState = previousBlockStates.get(checkPos);
                    if (prevState != null && prevState && !currentSolid) {
                        // Block was solid, now broken
                        BREAK_TIMESTAMPS.addLast(now);
                    }
                    
                    previousBlockStates.put(checkPos, currentSolid);
                }
            }
        }
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
