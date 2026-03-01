package com.reimo.reimohelper.util.helper;

/**
 * Rotation class to represent player rotation (yaw, pitch)
 */
public class Rotation {
    private float yaw;
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    /**
     * Add rotation values
     */
    public Rotation add(Rotation other) {
        return new Rotation(yaw + other.yaw, pitch + other.pitch);
    }

    /**
     * Subtract rotation values
     */
    public Rotation subtract(Rotation other) {
        return new Rotation(yaw - other.yaw, pitch - other.pitch);
    }

    /**
     * Normalize rotation values to 0-360 range
     */
    public Rotation normalize() {
        float normalizedYaw = yaw % 360;
        if (normalizedYaw < 0) normalizedYaw += 360;

        float normalizedPitch = pitch;
        while (normalizedPitch > 90) normalizedPitch = 90;
        while (normalizedPitch < -90) normalizedPitch = -90;

        return new Rotation(normalizedYaw, normalizedPitch);
    }

    /**
     * Get magnitude of rotation change needed
     */
    public double getMagnitude() {
        return Math.sqrt(yaw * yaw + pitch * pitch);
    }

    @Override
    public String toString() {
        return String.format("Rotation(yaw=%.1f, pitch=%.1f)", yaw, pitch);
    }
}
