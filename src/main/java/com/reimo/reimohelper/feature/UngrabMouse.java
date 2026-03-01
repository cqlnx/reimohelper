package com.reimo.reimohelper.feature;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.macro.AbstractMacro;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.network.chat.Component;

/**
 * This will need alot of updates it straight dog shit and doesnt do what it should.
 */
public final class UngrabMouse {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static UngrabMouse instance;
    private static final long NON_ATTACK_UNGRAB_DELAY_MS = 450L;

    private boolean mouseUngrabbed = false;
    private boolean running = false;
    private long ungrabAfterMs = 0L;
    private boolean oldPauseOnLostFocus = true;
    private boolean announcedAltTabSafe = false;
    private boolean lookLocked = false;
    private float lockedYaw = 0f;
    private float lockedPitch = 0f;
    private long nonAttackSinceMs = 0L;

    private UngrabMouse() {
    }

    public static UngrabMouse getInstance() {
        if (instance == null) {
            instance = new UngrabMouse();
        }
        return instance;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        running = true;
        ungrabAfterMs = System.currentTimeMillis() + 1400L;
        mouseUngrabbed = !MC.mouseHandler.isMouseGrabbed();
        nonAttackSinceMs = 0L;
        oldPauseOnLostFocus = MC.options.pauseOnLostFocus;
        MC.options.pauseOnLostFocus = false;
        if (ReimoHelperConfig.getInstance().autoUngrabMouse) {
            announceAltTabSafeOnce();
        }
    }

    public void stop() {
        running = false;
        MC.options.pauseOnLostFocus = oldPauseOnLostFocus;
        releaseAttackHold();
        clearLookLock();
        nonAttackSinceMs = 0L;
        announcedAltTabSafe = false;
        regrab();
    }

    /**
     * Set the look lock to the player's current yaw/pitch so the macro will keep
     * the camera at the position it was at when this method was called.
     */
    public void lockToCurrentRotation() {
        if (MC.player == null) return;
        lockedYaw = MC.player.getYRot();
        lockedPitch = MC.player.getXRot();
        lookLocked = true;
        LOGGER.info("Locked look to current rotation: yaw={}, pitch={}", lockedYaw, lockedPitch);
    }

    public void onClientTick() {
        if (!running) {
            clearLookLock();
            return;
        }
        if (!ReimoHelperConfig.getInstance().autoUngrabMouse) {
            clearLookLock();
            return;
        }

        if (MC.screen != null) {
            releaseAttackHold();
            clearLookLock();
            return;
        }

        if (System.currentTimeMillis() < ungrabAfterMs) {
            holdAttackIfNeeded();
            return;
        }

        // Attack-priority fallback:
        // When macro is actively farming, keep mouse grabbed for reliable held attack.
        // We still ignore mouse movement by locking yaw/pitch each tick.
        if (shouldHoldAttack()) {
            nonAttackSinceMs = 0L;
            if (!MC.mouseHandler.isMouseGrabbed()) {
                MC.mouseHandler.grabMouse();
                mouseUngrabbed = false;
            }
            applyLookLock();
            holdAttackIfNeeded();
            return;
        }

        if (!isNonAttackStable()) {
            holdAttackIfNeeded();
            return;
        }

        // Keep game logic active when cursor is released from window center.
        if (!MC.isWindowActive()) {
            MC.setWindowActive(true);
        }

        // Keep mouse ungrabbed while macro is running.
        if (MC.mouseHandler.isMouseGrabbed()) {
            MC.mouseHandler.releaseMouse();
            mouseUngrabbed = true;
        }
        applyLookLock();
        holdAttackIfNeeded();
    }

    public void onClientTickPost() {
        if (!running) {
            clearLookLock();
            return;
        }
        if (!ReimoHelperConfig.getInstance().autoUngrabMouse) {
            clearLookLock();
            return;
        }
        if (MC.screen != null) {
            releaseAttackHold();
            clearLookLock();
            return;
        }
        // Re-assert held attack after game input updates in this tick.
        applyLookLock();
        holdAttackIfNeeded();
        if (!shouldHoldAttack() && isNonAttackStable() && MC.mouseHandler.isMouseGrabbed()) {
            MC.mouseHandler.releaseMouse();
            mouseUngrabbed = true;
        }
    }

    public void ungrab() {
        if (MC.mouseHandler.isMouseGrabbed()) {
            MC.mouseHandler.releaseMouse();
            mouseUngrabbed = true;
        }
    }

    public void onEnabledWhileRunning() {
        if (!running) {
            return;
        }
        // Small delay avoids losing held-attack state during focus transition.
        ungrabAfterMs = System.currentTimeMillis() + 120L;
        mouseUngrabbed = false;
        announceAltTabSafeOnce();
        ungrab();
        holdAttackIfNeeded();
    }

    public void onDisabledWhileRunning() {
        releaseAttackHold();
        clearLookLock();
        regrab();
    }

    public void regrab() {
        if (mouseUngrabbed && MC.screen == null) {
            MC.mouseHandler.grabMouse();
        }
        mouseUngrabbed = false;
    }

    private void holdAttackIfNeeded() {
        if (!shouldHoldAttack()) {
            releaseAttackHold();
            return;
        }
        setAttackHeld(true);
    }

    private boolean shouldHoldAttack() {
        MacroHandler macroHandler = MacroHandler.getInstance();
        if (!macroHandler.isMacroActive()) {
            return false;
        }
        if (macroHandler.getCurrentMacro().isPresent()) {
            AbstractMacro.State state = macroHandler.getCurrentMacro().get().getCurrentState();
            if (state == AbstractMacro.State.DROPPING || state == AbstractMacro.State.NONE) {
                return false;
            }
        }
        return true;
    }

    private void setAttackHeld(boolean held) {
        MC.options.keyAttack.setDown(held);
        KeyMapping.set(MC.options.keyAttack.getKey(), held);
    }

    private void releaseAttackHold() {
        setAttackHeld(false);
    }

    private void announceAltTabSafeOnce() {
        if (announcedAltTabSafe || MC.player == null) {
            return;
        }
        MC.player.displayClientMessage(
                Component.literal("[ReimoHelper] Auto ungrab enabled. You can safely alt-tab."),
                false
        );
        announcedAltTabSafe = true;
    }

    private void applyLookLock() {
        if (MC.player == null) {
            return;
        }
        if (!lookLocked) {
            lockedYaw = MC.player.getYRot();
            lockedPitch = MC.player.getXRot();
            lookLocked = true;
            return;
        }
        MC.player.setYRot(lockedYaw);
        MC.player.setXRot(lockedPitch);
    }

    private void clearLookLock() {
        lookLocked = false;
    }

    private boolean isNonAttackStable() {
        long now = System.currentTimeMillis();
        if (nonAttackSinceMs == 0L) {
            nonAttackSinceMs = now;
            return false;
        }
        return now - nonAttackSinceMs >= NON_ATTACK_UNGRAB_DELAY_MS;
    }
}
