package com.reimo.reimohelper.util;

public class Clock {
    private long scheduledTime = 0;

    public void schedule(long delayMillis) {
        this.scheduledTime = System.currentTimeMillis() + delayMillis;
    }

    public boolean passed() {
        if (!isScheduled()) return true;
        return System.currentTimeMillis() >= scheduledTime;
    }

    public boolean isScheduled() {
        return scheduledTime > 0;
    }

    public long getRemainingTime() {
        if (!isScheduled()) return 0;
        long remaining = scheduledTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void reset() {
        scheduledTime = 0;
    }

    public long getElapsedTime() {
        if (scheduledTime == 0) return 0;
        return System.currentTimeMillis() - (scheduledTime - getScheduledDelay());
    }

    private long getScheduledDelay() {
        return Math.max(0, scheduledTime - System.currentTimeMillis());
    }
}
