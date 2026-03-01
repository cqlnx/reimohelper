package com.reimo.reimohelper.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Utility class for block operations
 */
public class BlockUtils {
    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * Get relative block at offset positions
     */
    public static Block getRelativeBlock(int dx, int dy, int dz) {
        if (MC.player == null || MC.level == null) {
            return Blocks.AIR;
        }

        BlockPos pos = MC.player.blockPosition()
                .offset(dx, dy, dz);
        return MC.level.getBlockState(pos).getBlock();
    }

    /**
     * Get relative block position
     */
    public static BlockPos getRelativeBlockPos(int dx, int dy, int dz, float yaw) {
        if (MC.player == null) {
            return BlockPos.ZERO;
        }

        int x = MC.player.getBlockX() + dx;
        int y = MC.player.getBlockY() + dy;
        int z = MC.player.getBlockZ() + dz;

        return new BlockPos(x, y, z);
    }

    /**
     * Check if block is air
     */
    public static boolean isAir(BlockPos pos) {
        if (MC.level == null) return true;
        return MC.level.getBlockState(pos).isAir();
    }

    /**
     * Get block at position
     */
    public static Block getBlockAt(BlockPos pos) {
        if (MC.level == null) return Blocks.AIR;
        return MC.level.getBlockState(pos).getBlock();
    }

    /**
     * Check if block is replaceable (like tall grass, water)
     */
    public static boolean isReplaceable(BlockPos pos) {
        if (MC.level == null) return false;
        // Simplified for 1.21.11 - just check if block is air
        return MC.level.getBlockState(pos).isAir();
    }

    /**
     * Get distance to block
     */
    public static double getDistanceToBlock(BlockPos pos) {
        if (MC.player == null) return Double.MAX_VALUE;
        return MC.player.position().distanceTo(pos.getCenter());
    }
}
