package com.reimo.reimohelper.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class BlockUtils {
    private static final Minecraft MC = Minecraft.getInstance();

    public static Block getRelativeBlock(int dx, int dy, int dz) {
        if (MC.player == null || MC.level == null) {
            return Blocks.AIR;
        }

        BlockPos pos = MC.player.blockPosition()
                .offset(dx, dy, dz);
        return MC.level.getBlockState(pos).getBlock();
    }

    public static BlockPos getRelativeBlockPos(int dx, int dy, int dz, float yaw) {
        if (MC.player == null) {
            return BlockPos.ZERO;
        }

        int x = MC.player.getBlockX() + dx;
        int y = MC.player.getBlockY() + dy;
        int z = MC.player.getBlockZ() + dz;

        return new BlockPos(x, y, z);
    }

    public static boolean isAir(BlockPos pos) {
        if (MC.level == null) return true;
        return MC.level.getBlockState(pos).isAir();
    }

    public static Block getBlockAt(BlockPos pos) {
        if (MC.level == null) return Blocks.AIR;
        return MC.level.getBlockState(pos).getBlock();
    }

    public static boolean isReplaceable(BlockPos pos) {
        if (MC.level == null) return false;
        return MC.level.getBlockState(pos).isAir();
    }

    public static double getDistanceToBlock(BlockPos pos) {
        if (MC.player == null) return Double.MAX_VALUE;
        return MC.player.position().distanceTo(pos.getCenter());
    }
}
