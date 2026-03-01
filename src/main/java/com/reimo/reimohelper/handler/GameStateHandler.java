package com.reimo.reimohelper.handler;

import net.minecraft.client.Minecraft;

/**
 * Tracks game state for macro execution
 */
public class GameStateHandler {
    private static GameStateHandler instance;
    private static final Minecraft MC = Minecraft.getInstance();

    private boolean notMoving = false;
    private boolean updatedState = false;
    private Clock notMovingTimer = new Clock();

    public boolean isNotMoving() { return notMoving; }
    public void setNotMoving(boolean notMoving) { this.notMoving = notMoving; }
    public boolean isUpdatedState() { return updatedState; }
    public void setUpdatedState(boolean updated) { this.updatedState = updated; }
    public Clock getNotMovingTimer() { return notMovingTimer; }
    public void setNotMovingTimer(Clock timer) { this.notMovingTimer = timer; }

    private GameStateHandler() {
    }

    public static GameStateHandler getInstance() {
        if (instance == null) {
            instance = new GameStateHandler();
        }
        return instance;
    }

    /**
     * Check if player is not moving
     */
    public boolean notMoving() {
        if (MC.player == null) return true;
        return !MC.options.keyUp.isDown() && !MC.options.keyDown.isDown() && 
               !MC.options.keyLeft.isDown() && !MC.options.keyRight.isDown();
    }

    /**
     * Check if can change direction
     */
    public boolean canChangeDirection() {
        return notMoving();
    }

    /**
     * Schedule rewarp
     */
    public void scheduleRewarp() {
        // Implementation
    }

    /**
     * Schedule not moving state
     */
    public void scheduleNotMoving() {
        scheduleNotMoving(100);
    }

    /**
     * Schedule not moving state with delay
     */
    public void scheduleNotMoving(int delayMillis) {
        notMovingTimer.schedule(delayMillis);
    }

    /**
     * Check if can rewarp
     */
    public boolean canRewarp() {
        return notMoving();
    }

    /**
     * Simple clock for timing
     */
    public static class Clock {
        private long scheduledTime = 0;

        public void schedule(long delayMillis) {
            this.scheduledTime = System.currentTimeMillis() + delayMillis;
        }

        public boolean passed() {
            if (scheduledTime == 0) return true;
            return System.currentTimeMillis() >= scheduledTime;
        }

        public void reset() {
            scheduledTime = 0;
        }
    }
}
