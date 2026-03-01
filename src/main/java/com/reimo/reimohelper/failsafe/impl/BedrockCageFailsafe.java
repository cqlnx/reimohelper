package com.reimo.reimohelper.failsafe.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public class BedrockCageFailsafe {
    private static final Minecraft MC = Minecraft.getInstance();

    public String check() {
        if (MC.player == null || MC.level == null) return null;

        BlockPos base = MC.player.blockPosition();
        int bedrock = 0;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos p = base.above(dy);
            if (MC.level.getBlockState(p.north()).is(Blocks.BEDROCK)) bedrock++;
            if (MC.level.getBlockState(p.south()).is(Blocks.BEDROCK)) bedrock++;
            if (MC.level.getBlockState(p.east()).is(Blocks.BEDROCK)) bedrock++;
            if (MC.level.getBlockState(p.west()).is(Blocks.BEDROCK)) bedrock++;
        }
        return bedrock >= 4 ? "BEDROCK_CAGE: bedrock around player" : null;
    }
}
