package com.reimo.reimohelper.macro;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.feature.RewarpManager;
import com.reimo.reimohelper.handler.GameStateHandler;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.handler.RotationHandler;
import com.reimo.reimohelper.util.*;
import com.reimo.reimohelper.util.helper.Rotation;
import com.reimo.reimohelper.util.helper.RotationConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Abstract base class for macros in ReimoHelper
 * Handles core macro state management, rotation, and farming logic
 */
@SuppressWarnings("ALL")
public abstract class AbstractMacro {
    public static final Minecraft MC = Minecraft.getInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    
    protected final RotationHandler rotation = RotationHandler.getInstance();
    protected final MacroHandler macroHandler = MacroHandler.getInstance();
    protected final Clock rewarpDelay = new Clock();
    protected final Clock rewarpCommandCooldown = new Clock();
    protected final Clock rewarpStopDelay = new Clock();
    protected final Clock rewarpResumeDelay = new Clock();
    protected final Clock delayBeforeBreakTime = new Clock();
    protected final Clock breakTime = new Clock();

    protected State currentState = State.NONE;
    protected State previousState = State.NONE;
    protected boolean enabled = false;
    protected Optional<SavedState> savedState = Optional.empty();
    protected boolean restoredState = false;
    protected int layerY = 0;
    protected Optional<Float> yaw = Optional.empty();
    protected Optional<Float> pitch = Optional.empty();
    protected Optional<Float> closest90Deg = Optional.empty();
    protected boolean rotated = false;
    protected RewarpState rewarpState = RewarpState.NONE;
    protected WalkingDirection walkingDirection = WalkingDirection.X;
    protected int previousWalkingCoord = 0;
    protected boolean rewarpPending = false;
    protected boolean rewarpSent = false;

    // Getters
    public State getCurrentState() { return currentState; }
    public State getPreviousState() { return previousState; }
    public boolean isEnabled() { return enabled; }
    public Optional<SavedState> getSavedState() { return savedState; }
    public boolean isRestoredState() { return restoredState; }
    public int getLayerY() { return layerY; }
    public Optional<Float> getClosest90Deg() { return closest90Deg; }
    public boolean isRotated() { return rotated; }
    public RewarpState getRewarpState() { return rewarpState; }
    public WalkingDirection getWalkingDirection() { return walkingDirection; }
    public int getPreviousWalkingCoord() { return previousWalkingCoord; }

    // Setters
    public void setCurrentState(State state) { this.currentState = state; }
    public void setPreviousState(State state) { this.previousState = state; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setSavedState(Optional<SavedState> state) { this.savedState = state; }
    public void setRestoredState(boolean restored) { this.restoredState = restored; }
    public void setLayerY(int y) { this.layerY = y; }
    public void setClosest90Deg(Optional<Float> deg) { this.closest90Deg = deg; }
    public void setRotated(boolean rotated) { this.rotated = rotated; }
    public void setRewarpState(RewarpState state) { this.rewarpState = state; }
    public void setWalkingDirection(WalkingDirection dir) { this.walkingDirection = dir; }
    public void setPreviousWalkingCoord(int coord) { this.previousWalkingCoord = coord; }

    public AbstractMacro() {
        this.currentState = State.NONE;
    }

    /**
     * Check if macro is enabled and no feature is pausing it
     */
    public boolean isEnabledAndNoFeature() {
        return enabled;
    }

    /**
     * Check if macro is paused
     */
    public boolean isPaused() {
        return !enabled;
    }

    /**
     * Rotation getter/setter methods
     */
    public boolean isYawSet() {
        return yaw.isPresent();
    }

    public boolean isPitchSet() {
        return pitch.isPresent();
    }

    public float getYaw() {
        return yaw.orElse(0f);
    }

    public float getPitch() {
        return pitch.orElse(0f);
    }

    public void setYaw(float yaw) {
        this.yaw = Optional.of(yaw);
    }

    public void setPitch(float pitch) {
        this.pitch = Optional.of(pitch);
    }

    /**
     * Main tick method - called every game tick
     * Override for custom tick behavior
     */
    public void onTick() {
        if (!enabled) return;

        if (handleRewarpFlow()) {
            return;
        }

        // Handle player flying
        if (MC.player != null && MC.player.getAbilities().flying) {
            LogUtils.sendDebug("Player is flying");
            return;
        }

        if (!delayBeforeBreakTime.passed()) {
            LogUtils.sendDebug("Delay before break time: " + delayBeforeBreakTime.getRemainingTime());
            return;
        }

        if (!breakTime.passed()) {
            LogUtils.sendDebug("Break time remaining: " + breakTime.getRemainingTime());
            KeyBindUtils.stopMovement();
            GameStateHandler.getInstance().scheduleNotMoving();
            return;
        }

        LogUtils.sendDebug("Macro tick - State: " + currentState);

        if (!PlayerUtils.isRewarpLocationSet()) {
            LogUtils.sendError("Rewarp position is not set!");
            MacroHandler.getInstance().disableMacro();
            return;
        }

        if (rotation.isRotating()) {
            if (MC.player != null && !MC.player.isShiftKeyDown()) {
                KeyBindUtils.stopMovement();
            }
            GameStateHandler.getInstance().scheduleNotMoving();
            return;
        }

        if (rewarpState == RewarpState.TELEPORTED) {
            // Handle post-rewarp rotation
            if (rotated && MC.player != null) {
                LogUtils.sendDebug("Rotated after teleport");
                rewarpState = RewarpState.POST_REWARP;
                rewarpDelay.schedule(500);
                rotated = true;
                return;
            }

            if (shouldRotateAfterWarp()) {
                Rotation newRotation = new Rotation(getYaw(), getPitch());
                if (MC.player != null) {
                    Rotation currentRotation = new Rotation(MC.player.getYRot(), MC.player.getXRot());
                    Rotation neededChange = rotation.getNeededChange(newRotation, currentRotation);

                    if (Math.abs(neededChange.getYaw()) > 0.5f || Math.abs(neededChange.getPitch()) > 0.5f) {
                        rotation.easeTo(new RotationConfiguration(
                                newRotation,
                                500,
                                null
                        ).easeOutBack(true));
                    }
                }
            }
            rotated = true;
            setLayerY(MC.player != null ? MC.player.getBlockY() : 0);
            return;
        }

        updateState();
        if (getCurrentState() == State.NONE) {
            return;
        }
        invokeState();
    }

    private boolean handleRewarpFlow() {
        if (MC.player == null) {
            return false;
        }

        if (!rewarpPending && PlayerUtils.isInRewarpZone(1.35, 2) && rewarpCommandCooldown.passed()) {
            rewarpPending = true;
            rewarpSent = false;
            rewarpStopDelay.reset();
            rewarpResumeDelay.reset();
            LogUtils.sendInfo("In rewarp zone, stopping to prepare warp");
        }

        if (!rewarpPending) {
            return false;
        }

        KeyBindUtils.stopMovement(false);
        MC.options.keyAttack.setDown(false);

        if (!rewarpSent) {
            if (!rewarpStopDelay.isScheduled()) {
                rewarpStopDelay.schedule(2000);
            }

            if (!rewarpStopDelay.passed()) {
                return true;
            }

            ReimoHelperConfig config = ReimoHelperConfig.getInstance();
            LogUtils.sendInfo("Executing rewarp command: " + config.rewarpCommand);
            RewarpManager.getInstance().executeReward();
            rewarpSent = true;
            rewarpResumeDelay.schedule(3000);
            rewarpCommandCooldown.schedule(10000);
            return true;
        }

        if (!rewarpResumeDelay.passed()) {
            return true;
        }

        LogUtils.sendInfo("Rewarp wait complete, resuming macro");
        rewarpPending = false;
        rewarpSent = false;
        rewarpStopDelay.reset();
        rewarpResumeDelay.reset();
        return false;
    }

    /**
     * Called when macro is enabled
     */
    public void onEnable() {
        if (MC.player == null) return;

        if (savedState.isPresent()) {
            LogUtils.sendInfo("Restoring saved state");
            SavedState state = savedState.get();
            changeState(state.getState());
            setYaw(state.getYaw());
            setPitch(state.getPitch());
            setClosest90Deg(state.getClosest90Deg());
            restoredState = true;
            savedState = Optional.empty();
        }

        setEnabled(true);
        rewarpCommandCooldown.reset();
        rewarpStopDelay.reset();
        rewarpResumeDelay.reset();
        rewarpPending = false;
        rewarpSent = false;
        setLayerY(MC.player.getBlockY());
        if (currentState == null) {
            changeState(State.NONE);
        }

        LogUtils.sendInfo("Macro enabled");
    }

    /**
     * Called when macro is disabled
     */
    public void onDisable() {
        changeState(State.NONE);
        setRewarpState(RewarpState.NONE);
        setClosest90Deg(Optional.empty());
        setEnabled(false);
        rewarpCommandCooldown.reset();
        rewarpStopDelay.reset();
        rewarpResumeDelay.reset();
        rewarpPending = false;
        rewarpSent = false;
        KeyBindUtils.stopMovement(false);
        if (MC.player != null) {
            MC.options.keyAttack.setDown(false);
        }
        yaw = Optional.empty();
        pitch = Optional.empty();

        LogUtils.sendInfo("Macro disabled");
    }

    /**
     * Save macro state
     */
    public void saveState() {
        if (savedState.isEmpty()) {
            LogUtils.sendInfo("Saving macro state: " + currentState);
            savedState = Optional.of(new SavedState(
                    currentState,
                    getYaw(),
                    getPitch(),
                    closest90Deg.orElse(0f)
            ));
        }
    }

    /**
     * Change macro state
     */
    public void changeState(State state) {
        if (currentState != state) {
            LogUtils.sendDebug("Changing state from " + currentState + " to " + state);
            setPreviousState(currentState);
            setCurrentState(state);
        }
    }

    /**
     * Abstract methods to be implemented by subclasses
     */
    public abstract void updateState();

    public abstract void invokeState();

    public abstract void actionAfterTeleport();

    /**
     * Check movement direction based on surrounding blocks
     */
    public State calculateDirection() {
        if (MC.level == null || MC.player == null) {
            return State.NONE;
        }

        // Check left and back-left for air blocks
        if (isBlockAir(-1, 0, 0) && isBlockAir(-1, -1, 0)) {
            return State.RIGHT;
        }

        // Check right and back-right for air blocks
        if (isBlockAir(1, 0, 0) && isBlockAir(1, -1, 0)) {
            return State.LEFT;
        }

        return State.FORWARD;
    }

    /**
     * Helper method to check if block is air
     */
    private boolean isBlockAir(int dx, int dy, int dz) {
        if (MC.player == null || MC.level == null) return false;
        int x = (int) MC.player.getX() + dx;
        int y = (int) MC.player.getY() + dy;
        int z = (int) MC.player.getZ() + dz;
        return MC.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).isAir();
    }

    /**
     * Rotate player yaw after rewarp
     */
    public void doAfterRewarpRotation() {
        float newYaw = getYaw() + 180f;
        if (newYaw > 360) newYaw -= 360;
        setYaw(newYaw);
        setClosest90Deg(Optional.of(getClosest90Rotation(newYaw)));
    }

    /**
     * Get closest 90-degree rotation
     */
    private float getClosest90Rotation(float yaw) {
        float normalized = yaw % 360;
        if (normalized < 0) normalized += 360;

        // Find closest 90-degree angle
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
     * Set walking direction based on block positions
     */
    protected void setWalkingDirection() {
        if (MC.player == null || MC.level == null) return;

        int playerX = MC.player.getBlockX();
        int playerZ = MC.player.getBlockZ();

        // Determine if walking in X or Z direction
        setWalkingDirection(WalkingDirection.X);
        setPreviousWalkingCoord(playerZ);

        LOGGER.debug("Walking direction: {}", getWalkingDirection());
    }

    /**
     * Check if rotation after warp is needed
     */
    public boolean shouldRotateAfterWarp() {
        return true;
    }

    /**
     * Set break time for macro
     */
    public void setBreakTime(double time, double timeBefore) {
        // Timer logic to be implemented with scheduler
    }

    /**
     * Macro state enum
     */
    public enum State {
        NONE,
        DROPPING,
        SWITCHING_SIDE,
        SWITCHING_LANE,
        LEFT,
        RIGHT,
        BACKWARD,
        FORWARD,
        A,
        D,
        S,
        W
    }

    /**
     * Rewarp state enum
     */
    public enum RewarpState {
        NONE,
        TELEPORTING,
        TELEPORTED,
        POST_REWARP
    }

    /**
     * Walking direction enum
     */
    public enum WalkingDirection {
        X,
        Z
    }

    /**
     * Saved state for macro persistence
     */
    public static class SavedState {
        private State state;
        private float yaw;
        private float pitch;
        private Optional<Float> closest90Deg;

        public State getState() { return state; }
        public void setState(State state) { this.state = state; }
        public float getYaw() { return yaw; }
        public void setYaw(float yaw) { this.yaw = yaw; }
        public float getPitch() { return pitch; }
        public void setPitch(float pitch) { this.pitch = pitch; }
        public Optional<Float> getClosest90Deg() { return closest90Deg; }
        public void setClosest90Deg(Optional<Float> deg) { this.closest90Deg = deg; }

        public SavedState(State state, float yaw, float pitch, float closest90Deg) {
            this.state = state;
            this.yaw = yaw;
            this.pitch = pitch;
            this.closest90Deg = Optional.of(closest90Deg);
        }

        @Override
        public String toString() {
            return "SavedState{" +
                    "state=" + state +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    ", closest90Deg=" + closest90Deg +
                    '}';
        }
    }
}
