package com.reimo.reimohelper.handler;

import com.reimo.reimohelper.util.helper.Rotation;
import com.reimo.reimohelper.util.helper.RotationConfiguration;
import net.minecraft.client.Minecraft;

/**
 * Handles player rotation with easing
 */
public class RotationHandler {
    private static RotationHandler instance;
    private static final Minecraft MC = Minecraft.getInstance();

    private RotationConfiguration currentRotation;
    private long startTime;
    private boolean isRotating = false;

    public RotationConfiguration getCurrentRotation() { return currentRotation; }
    public boolean isRotating() { return isRotating; }

    private RotationHandler() {
    }

    public static RotationHandler getInstance() {
        if (instance == null) {
            instance = new RotationHandler();
        }
        return instance;
    }

    /**
     * Ease to a target rotation
     */
    public void easeTo(RotationConfiguration config) {
        this.currentRotation = config;
        this.startTime = System.currentTimeMillis();
        this.isRotating = true;
    }

    /**
     * Update rotation each tick
     */
    public void onTick() {
        if (!isRotating || currentRotation == null || MC.player == null) {
            return;
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        double progress = currentRotation.getProgress(elapsedTime);

        if (currentRotation.isUseEaseOutBack()) {
            progress = currentRotation.applyEaseOutBack(progress);
        }

        if (progress >= 1.0) {
            // Rotation complete
            MC.player.setYRot(currentRotation.getTargetRotation().getYaw());
            MC.player.setXRot(currentRotation.getTargetRotation().getPitch());
            isRotating = false;
            currentRotation = null;
            return;
        }

        // Interpolate current rotation
        float currentYaw = MC.player.getYRot();
        float currentPitch = MC.player.getXRot();

        float targetYaw = currentRotation.getTargetRotation().getYaw();
        float targetPitch = currentRotation.getTargetRotation().getPitch();

        float newYaw = currentYaw + (targetYaw - currentYaw) * (float) progress;
        float newPitch = currentPitch + (targetPitch - currentPitch) * (float) progress;

        MC.player.setYRot(newYaw);
        MC.player.setXRot(newPitch);
    }

    /**
     * Get needed rotation change between two rotations
     */
    public Rotation getNeededChange(Rotation target, Rotation current) {
        float yawDiff = target.getYaw() - current.getYaw();
        float pitchDiff = target.getPitch() - current.getPitch();

        // Normalize yaw difference
        if (yawDiff > 180) yawDiff -= 360;
        if (yawDiff < -180) yawDiff += 360;

        return new Rotation(yawDiff, pitchDiff);
    }

    /**
     * Reset rotation handler
     */
    public void reset() {
        isRotating = false;
        currentRotation = null;
    }
}
