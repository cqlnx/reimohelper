package com.reimo.reimohelper.util;

/**
 * Angle utility functions for rotation calculations
 */
public class AngleUtils {
    /**
     * Normalize angle to 0-360 range
     */
    public static float normalize(float angle) {
        float result = angle % 360;
        if (result < 0) {
            result += 360;
        }
        return result;
    }

    /**
     * Get closest 90-degree angle
     */
    public static float getClosest(float yaw) {
        float normalized = normalize(yaw);
        float[] angles = {0, 90, 180, 270};
        float closest = angles[0];
        float minDiff = Math.abs(normalized - angles[0]);

        for (float angle : angles) {
            float diff = Math.abs(normalized - angle);
            if (diff < minDiff) {
                minDiff = diff;
                closest = angle;
            }
        }
        return closest;
    }

    /**
     * Get rotation as 360-degree value
     */
    public static float get360RotationYaw(float yaw) {
        float result = yaw % 360;
        if (result < 0) {
            result += 360;
        }
        return result;
    }

    /**
     * Check if two angles are approximately equal
     */
    public static boolean isApproxEqual(float angle1, float angle2, float tolerance) {
        float diff = Math.abs(normalize(angle1) - normalize(angle2));
        return diff < tolerance || (360 - diff) < tolerance;
    }

    /**
     * Get shortest rotation between two angles
     */
    public static float getShortestRotation(float from, float to) {
        float diff = normalize(to - from);
        if (diff > 180) {
            diff -= 360;
        }
        return diff;
    }
}
