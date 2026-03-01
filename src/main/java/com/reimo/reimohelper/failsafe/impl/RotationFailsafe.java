package com.reimo.reimohelper.failsafe.impl;

import net.minecraft.client.Minecraft;

public class RotationFailsafe {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final float ROT_DELTA = 5f;

    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private boolean primed = false;

    public String check() {
        if (MC.player == null) return null;
        if (!primed) {
            lastYaw = MC.player.getYRot();
            lastPitch = MC.player.getXRot();
            primed = true;
            return null;
        }

        float yaw = MC.player.getYRot();
        float pitch = MC.player.getXRot();
        float yawDelta = Math.abs(wrap180(yaw - lastYaw));
        float pitchDelta = Math.abs(pitch - lastPitch);

        lastYaw = yaw;
        lastPitch = pitch;
        if (yawDelta >= ROT_DELTA || pitchDelta >= ROT_DELTA) {
            return "ROTATION: sudden rotation";
        }
        return null;
    }

    public void reset() {
        primed = false;
    }

    private float wrap180(float angle) {
        float wrapped = angle % 360f;
        if (wrapped >= 180f) wrapped -= 360f;
        if (wrapped < -180f) wrapped += 360f;
        return wrapped;
    }
}
