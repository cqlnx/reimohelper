package com.reimo.reimohelper.util;

/**
 * Clock utility for timing operations
 */
public class Clock {
    private long scheduledTime = 0;

    /**
     * Schedule a delay
     */
    public void schedule(long delayMillis) {
        this.scheduledTime = System.currentTimeMillis() + delayMillis;
    }

    /**
     * Check if time has passed
     */
    public boolean passed() {
        if (!isScheduled()) return true;
        return System.currentTimeMillis() >= scheduledTime;
    }

    /**
     * Check if clock is scheduled
     */
    public boolean isScheduled() {
        return scheduledTime > 0;
    }

    /**
     * Get remaining time in milliseconds
     */
    public long getRemainingTime() {
        if (!isScheduled()) return 0;
        long remaining = scheduledTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Reset the clock
     */
    public void reset() {
        scheduledTime = 0;
    }

    /**
     * Get elapsed time since schedule
     */
    public long getElapsedTime() {
        if (scheduledTime == 0) return 0;
        return System.currentTimeMillis() - (scheduledTime - getScheduledDelay());
    }

    /**
     * Get scheduled delay (internal helper)
     */
    private long getScheduledDelay() {
        return Math.max(0, scheduledTime - System.currentTimeMillis());
    }
}
