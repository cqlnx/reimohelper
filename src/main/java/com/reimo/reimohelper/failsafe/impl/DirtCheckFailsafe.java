package com.reimo.reimohelper.failsafe.impl;

import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.macro.AbstractMacro;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class DirtCheckFailsafe {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final double LOW_MOVE_THRESHOLD = 0.03;
    private static final long BLOCKED_TIMEOUT_MS = 700L;

    private long blockedSinceMs = 0L;
    private double lastX = 0.0;
    private double lastY = 0.0;
    private double lastZ = 0.0;
    private boolean primed = false;

    public String check() {
        if (MC.player == null) return null;
        if (!primed) {
            prime();
            return null;
        }

        AbstractMacro.State state = MacroHandler.getInstance()
                .getCurrentMacro()
                .map(AbstractMacro::getCurrentState)
                .orElse(AbstractMacro.State.NONE);

        boolean sideMoveState = state == AbstractMacro.State.LEFT || state == AbstractMacro.State.RIGHT;
        // Determine intended side based on macro state
        Direction intendedSide = state == AbstractMacro.State.LEFT ? MC.player.getDirection().getCounterClockWise() : MC.player.getDirection().getClockWise();
        boolean tryingToMove = MC.options.keyUp.isDown() && ((state == AbstractMacro.State.LEFT && MC.options.keyLeft.isDown()) || (state == AbstractMacro.State.RIGHT && MC.options.keyRight.isDown()));
        double dx = MC.player.getX() - lastX;
        double dz = MC.player.getZ() - lastZ;
        double horizontalMove = Math.hypot(dx, dz);
        boolean sameLayer = Math.abs(MC.player.getY() - lastY) < 0.12;
        boolean sideBlocked = isAnySideBlocked();
        boolean intendedSideBlocked = wouldCollideWhenMovingSide(intendedSide);
        boolean notDropping = state != AbstractMacro.State.DROPPING;

        long now = System.currentTimeMillis();
        if (sideMoveState && notDropping && tryingToMove && sameLayer && MC.player.onGround()) {
            // If the intended side is physically blocked, trigger immediately
            if (intendedSideBlocked) {
                updateLast();
                return "DIRT_CHECK: blocked on same layer (side blocked)";
            }
            // Otherwise, wait for low-movement timeout
            if (horizontalMove < LOW_MOVE_THRESHOLD || sideBlocked) {
                if (blockedSinceMs == 0L) blockedSinceMs = now;
                if (now - blockedSinceMs >= BLOCKED_TIMEOUT_MS) {
                    blockedSinceMs = 0L;
                    updateLast();
                    return "DIRT_CHECK: blocked on same layer" + (sideBlocked ? " (side blocked)" : " (low movement)");
                }
            } else {
                blockedSinceMs = 0L;
            }
        } else {
            blockedSinceMs = 0L;
        }

        updateLast();
        return null;
    }

    public void reset() {
        blockedSinceMs = 0L;
        primed = false;
    }

    private void prime() {
        blockedSinceMs = 0L;
        primed = true;
        updateLast();
    }

    private void updateLast() {
        if (MC.player == null) return;
        lastX = MC.player.getX();
        lastY = MC.player.getY();
        lastZ = MC.player.getZ();
    }

    private boolean isAnySideBlocked() {
        if (MC.player == null || MC.level == null) return false;
        Direction facing = MC.player.getDirection();

        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        boolean leftBlocked = wouldCollideWhenMovingSide(left);
        boolean rightBlocked = wouldCollideWhenMovingSide(right);

        return leftBlocked || rightBlocked;
    }

    private boolean wouldCollideWhenMovingSide(Direction side) {
        if (MC.player == null || MC.level == null) return false;
        double step = 0.18;
        AABB movedBox = MC.player.getBoundingBox().move(side.getStepX() * step, 0.0, side.getStepZ() * step);
        return !MC.level.noCollision(MC.player, movedBox);
    }
}
