package com.reimo.reimohelper.failsafe.impl;

import net.minecraft.client.Minecraft;

public class FullInventoryFailsafe {
    private static final Minecraft MC = Minecraft.getInstance();
    private int checkTick = 0;

    public String check() {
        if (MC.player == null) return null;

        checkTick++;
        if (checkTick < 40) return null;
        checkTick = 0;

        for (int i = 0; i < 36; i++) {
            if (MC.player.getInventory().getItem(i).isEmpty()) return null;
        }
        return "FULL_INVENTORY: inventory full";
    }
}
