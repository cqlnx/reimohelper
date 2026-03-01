package com.reimo.reimohelper.failsafe.impl;

import net.minecraft.client.Minecraft;

public class TeleportFailsafe {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final double TELEPORT_DISTANCE = 3.0;

    private double lastX = 0.0;
    private double lastY = 0.0;
    private double lastZ = 0.0;
    private boolean primed = false;

    public String check(boolean knownTeleportFlow) {
        if (MC.player == null) return null;
        if (!primed) {
            lastX = MC.player.getX();
            lastY = MC.player.getY();
            lastZ = MC.player.getZ();
            primed = true;
            return null;
        }

        double x = MC.player.getX();
        double y = MC.player.getY();
        double z = MC.player.getZ();
        double dist = Math.sqrt((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ));

        lastX = x;
        lastY = y;
        lastZ = z;

        if (!knownTeleportFlow && dist > TELEPORT_DISTANCE) {
            return "TELEPORT: moved too far in one tick";
        }
        return null;
    }

    public void reset() {
        primed = false;
    }
}
