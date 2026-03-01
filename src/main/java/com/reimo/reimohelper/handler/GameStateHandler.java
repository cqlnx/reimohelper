package com.reimo.reimohelper.handler;

import net.minecraft.client.Minecraft;

// stores player state used by macros
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

        // return true if no movement keys pressed
    public boolean notMoving() {
        if (MC.player == null) return true;
        return !MC.options.keyUp.isDown() && !MC.options.keyDown.isDown() && 
               !MC.options.keyLeft.isDown() && !MC.options.keyRight.isDown();
    }

        // alias for notMoving()
    public boolean canChangeDirection() {
        return notMoving();
    }

        // placeholder, actual logic elsewhere
    public void scheduleRewarp() {
        // Implementation
    }

        // schedule a not-moving timer with default delay
    public void scheduleNotMoving() {
        scheduleNotMoving(100);
    }

        // schedule not-moving with custom delay
    public void scheduleNotMoving(int delayMillis) {
        notMovingTimer.schedule(delayMillis);
    }

        // same as notMoving()
    public boolean canRewarp() {
        return notMoving();
    }

        // tiny helper for delayed actions
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
