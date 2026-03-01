package com.reimo.reimohelper.util;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// helper methods for key states and movement
public class KeyBindUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Minecraft MC = Minecraft.getInstance();

        // release all movement inputs
    public static void stopMovement() {
        stopMovement(false);
    }

        // stop movement; optionally hold attack
    public static void stopMovement(boolean holdLeftClick) {
        if (MC.player == null) return;

        // Release movement keys - 1.21.11 compatible
        MC.options.keyUp.setDown(false);
        MC.options.keyDown.setDown(false);
        MC.options.keyLeft.setDown(false);
        MC.options.keyRight.setDown(false);
        MC.options.keyJump.setDown(false);

        if (holdLeftClick) {
            // Hold left click - would be implemented with block breaking
        }
    }

        // return which WASD keys are down
    public static boolean[] getHoldingKeybinds() {
        if (MC.player == null) return new boolean[0];
        boolean[] binds = new boolean[4];
        binds[0] = MC.options.keyUp.isDown();
        binds[1] = MC.options.keyDown.isDown();
        binds[2] = MC.options.keyLeft.isDown();
        binds[3] = MC.options.keyRight.isDown();
        return binds;
    }

        // press given key codes
    public static void holdThese(int... keyCodes) {
        if (MC.player == null) return;
        // Implementation for holding specific keys
    }

        // true if player non-null and alive
    public static boolean canMove() {
        if (MC.player == null) return false;
        return !MC.player.isDeadOrDying();
    }

        // directly set yaw/pitch
    public static void setRotation(float yaw, float pitch) {
        if (MC.player == null) return;
        MC.player.setYRot(yaw);
        MC.player.setXRot(pitch);
    }
}
