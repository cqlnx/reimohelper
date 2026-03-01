package com.reimo.reimohelper.util.helper;

public class RotationConfiguration {
    private Rotation targetRotation;
    private long duration;
    private Object easeFunction;
    private boolean useEaseOutBack = false;

    public RotationConfiguration(Rotation targetRotation, long duration, Object easeFunction) {
        this.targetRotation = targetRotation;
        this.duration = duration;
        this.easeFunction = easeFunction;
        this.useEaseOutBack = false;
    }

    public Rotation getTargetRotation() { return targetRotation; }
    public void setTargetRotation(Rotation rotation) { this.targetRotation = rotation; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public Object getEaseFunction() { return easeFunction; }
    public void setEaseFunction(Object func) { this.easeFunction = func; }
    public boolean isUseEaseOutBack() { return useEaseOutBack; }
    public void setUseEaseOutBack(boolean use) { this.useEaseOutBack = use; }

    public RotationConfiguration easeOutBack(boolean enabled) {
        this.useEaseOutBack = enabled;
        return this;
    }

    public double getProgress(long elapsedTime) {
        if (duration <= 0) return 1.0;
        double progress = (double) elapsedTime / duration;
        return Math.min(1.0, progress);
    }

    public double applyEaseOutBack(double progress) {
        if (!useEaseOutBack) return progress;

        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(progress - 1, 3) + c1 * Math.pow(progress - 1, 2);
    }

    @Override
    public String toString() {
        return "RotationConfiguration{" +
                "targetRotation=" + targetRotation +
                ", duration=" + duration +
                ", useEaseOutBack=" + useEaseOutBack +
                '}';
    }
}
