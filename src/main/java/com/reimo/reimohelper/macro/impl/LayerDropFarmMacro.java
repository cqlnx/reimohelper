package com.reimo.reimohelper.macro.impl;

import com.reimo.reimohelper.macro.AbstractMacro;
import com.reimo.reimohelper.util.KeyBindUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class LayerDropFarmMacro extends AbstractMacro {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Minecraft MC = Minecraft.getInstance();

    // formerly limited; now infinite
    // private static final int TARGET_DROPS = 5;
    private static final double DESCENT_SPEED_THRESHOLD = 0.06;
    private static final double LAYER_DROP_THRESHOLD = 0.65;
    private static final int DIRECTION_SWITCH_COOLDOWN_MS = 450;
    private static final int STUCK_TIMEOUT_MS = 500;
    private static final int DROP_WAIT_TIMEOUT_MS = 2200;
    private static final double MOVE_THRESHOLD = 0.02;

    private int completedDrops = 0;
    private double currentLayerY = 0.0;
    private boolean waitingForLanding = false;
    private boolean movingLeft = true;
    private long directionSwitchCooldownUntil = 0L;
    private long dropWaitStartedAtMs = 0L;
    private long lastMoveTimeMs = 0L;
    private double lastX = 0.0;
    private double lastY = 0.0;
    private double lastZ = 0.0;
    // When set, automatic direction switches are suppressed until this timestamp
    private long suppressSwitchUntilMs = 0L;

    public LayerDropFarmMacro() {
        super();
        LOGGER.info("Layer Drop Farm Macro created");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (MC.player != null) {
            lastX = MC.player.getX();
            lastY = MC.player.getY();
            lastZ = MC.player.getZ();
            currentLayerY = MC.player.getY();
            movingLeft = chooseInitialDirection();
            MC.options.keyAttack.setDown(true);
        }
        completedDrops = 0;
        waitingForLanding = false;
        directionSwitchCooldownUntil = 0L;
        dropWaitStartedAtMs = 0L;
        lastMoveTimeMs = System.currentTimeMillis();
        changeState(movingLeft ? State.LEFT : State.RIGHT);
    }

    @Override
    public void updateState() {
        if (!isEnabledAndNoFeature() || MC.player == null || MC.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        double x = MC.player.getX();
        double y = MC.player.getY();
        double z = MC.player.getZ();
        double vSpeed = lastY - y;
        double distMoved = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ));

        if (vSpeed > DESCENT_SPEED_THRESHOLD && !MC.player.onGround() && !waitingForLanding) {
            waitingForLanding = true;
            dropWaitStartedAtMs = now;
            LOGGER.info("Detected descent, waiting for landing");
            changeState(State.DROPPING);
        }

        if (waitingForLanding) {
            if (MC.player.onGround()) {
                double layerDelta = Math.abs(y - currentLayerY);
                if (layerDelta > LAYER_DROP_THRESHOLD) {
                    completedDrops++;
                    currentLayerY = y;
                    switchDirection(now, "Layer drop completed");
                    LOGGER.info("Layer drop completed: {} -> Direction: {}", completedDrops, movingLeft ? "LEFT" : "RIGHT");
                    // no automatic stop; keep going forever
                } else {
                    LOGGER.info("Landing detected without layer change (delta={}), resuming lane movement", String.format("%.3f", layerDelta));
                }
                waitingForLanding = false;
                dropWaitStartedAtMs = 0L;
                lastMoveTimeMs = now;
            } else if (dropWaitStartedAtMs > 0L && now - dropWaitStartedAtMs > DROP_WAIT_TIMEOUT_MS) {
                LOGGER.warn("Drop wait timed out, forcing lane resume");
                waitingForLanding = false;
                dropWaitStartedAtMs = 0L;
            }
            changeState(State.DROPPING);
            return;
        }

        if (distMoved > MOVE_THRESHOLD || waitingForLanding) {
            lastMoveTimeMs = now;
        } else if (now - lastMoveTimeMs > STUCK_TIMEOUT_MS && now >= directionSwitchCooldownUntil) {
            switchDirection(now, "Stuck on ground");
            lastMoveTimeMs = now;
        }
        changeState(movingLeft ? State.LEFT : State.RIGHT);

        lastX = x;
        lastY = y;
        lastZ = z;
    }

    @Override
    public void invokeState() {
        switch (getCurrentState()) {
            case LEFT:
                moveLeft();
                applyBreakAction();
                break;
            case RIGHT:
                moveRight();
                applyBreakAction();
                break;
            case DROPPING:
                KeyBindUtils.stopMovement(false);
                MC.options.keyUse.setDown(false);
                MC.options.keyAttack.setDown(false);
                break;
            default:
                moveLeft();
                applyBreakAction();
                break;
        }
    }

    private void moveLeft() {
        if (MC.player != null) {
            MC.options.keyLeft.setDown(true);
            MC.options.keyRight.setDown(false);
            MC.options.keyUp.setDown(true);
            MC.options.keyDown.setDown(false);
            MC.options.keyUse.setDown(false);
            MC.options.keyAttack.setDown(true);
        }
    }

    private void moveRight() {
        if (MC.player != null) {
            MC.options.keyLeft.setDown(false);
            MC.options.keyRight.setDown(true);
            MC.options.keyUp.setDown(true);
            MC.options.keyDown.setDown(false);
            MC.options.keyUse.setDown(false);
            MC.options.keyAttack.setDown(true);
        }
    }

    private boolean chooseInitialDirection() {
        if (MC.player == null || MC.level == null) {
            return true;
        }

        Direction facing = MC.player.getDirection();
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        BlockPos base = MC.player.blockPosition();

        int leftBlocked = sideObstacleScore(base, left);
        int rightBlocked = sideObstacleScore(base, right);

        // If right side is more blocked, start LEFT. If left is more blocked, start RIGHT.
        if (rightBlocked > leftBlocked) return true;
        if (leftBlocked > rightBlocked) return false;

        return true;
    }

    private int sideObstacleScore(BlockPos base, Direction side) {
        int score = 0;
        BlockPos feet = base.relative(side);
        BlockPos head = feet.above();
        BlockPos feetAhead = feet.relative(MC.player.getDirection());
        BlockPos headAhead = feetAhead.above();
        if (MC.level.getBlockState(feet).blocksMotion()) score++;
        if (MC.level.getBlockState(head).blocksMotion()) score++;
        if (MC.level.getBlockState(feetAhead).blocksMotion()) score++;
        if (MC.level.getBlockState(headAhead).blocksMotion()) score++;
        return score;
    }

    private void switchDirection(long now, String reason) {
        if (now < suppressSwitchUntilMs) {
            LOGGER.info("Direction switch suppressed until {} ms (now={}), reason: {}", suppressSwitchUntilMs, now, reason);
            return;
        }
        movingLeft = !movingLeft;
        directionSwitchCooldownUntil = now + DIRECTION_SWITCH_COOLDOWN_MS
                + ThreadLocalRandom.current().nextInt(40, 120);
        LOGGER.info("{} -> switching direction to {}", reason, movingLeft ? "LEFT" : "RIGHT");
    }

    public void preventAutoSwitchFor(long durationMs) {
        suppressSwitchUntilMs = System.currentTimeMillis() + Math.max(0L, durationMs);
        LOGGER.info("Auto direction switching suppressed for {} ms (until {})", durationMs, suppressSwitchUntilMs);
    }

    private void applyBreakAction() {
        if (MC.player == null) return;
        MC.options.keyAttack.setDown(true);
    }

    @Override
    public void actionAfterTeleport() {
        changeState(State.NONE);
    }
}
