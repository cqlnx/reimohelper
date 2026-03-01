package com.reimo.reimohelper.failsafe;

import com.reimo.reimohelper.failsafe.impl.BedrockCageFailsafe;
import com.reimo.reimohelper.failsafe.impl.DirtCheckFailsafe;
import com.reimo.reimohelper.failsafe.impl.EvacuateFailsafe;
import com.reimo.reimohelper.failsafe.impl.FullInventoryFailsafe;
import com.reimo.reimohelper.failsafe.impl.RotationFailsafe;
import com.reimo.reimohelper.failsafe.impl.TeleportFailsafe;
import com.reimo.reimohelper.feature.DiscordWebhookService;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.util.KeyBindUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailsafeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Minecraft MC = Minecraft.getInstance();
    private static FailsafeManager instance;

    private final DirtCheckFailsafe dirtCheckFailsafe = new DirtCheckFailsafe();
    private final BedrockCageFailsafe bedrockCageFailsafe = new BedrockCageFailsafe();
    private final RotationFailsafe rotationFailsafe = new RotationFailsafe();
    private final TeleportFailsafe teleportFailsafe = new TeleportFailsafe();
    private final FullInventoryFailsafe fullInventoryFailsafe = new FullInventoryFailsafe();
    private final EvacuateFailsafe evacuateFailsafe = EvacuateFailsafe.getInstance();

    private long lastTriggerMs = 0L;
    private static final long TRIGGER_COOLDOWN_MS = 5000L;

    public static FailsafeManager getInstance() {
        if (instance == null) {
            instance = new FailsafeManager();
        }
        return instance;
    }

    public void onTick() {
        if (MC.player == null || MC.level == null) {
            return;
        }

        evacuateFailsafe.onClientTick();

        if (!MacroHandler.getInstance().isMacroActive()) {
            resetState();
            return;
        }

        String trigger = findTrigger();
        if (trigger == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTriggerMs < TRIGGER_COOLDOWN_MS) {
            return;
        }

        lastTriggerMs = now;
        LOGGER.warn("Failsafe triggered: {}", trigger);
        KeyBindUtils.stopMovement(false);
        if (MC.options != null) {
            MC.options.keyAttack.setDown(false);
            MC.options.keyUse.setDown(false);
        }
        MacroHandler.getInstance().disableMacro();
        evacuateFailsafe.alertOnly(trigger);
        DiscordWebhookService.sendFailsafeTriggered(trigger);
    }

    public void onSystemMessage(String message) {
        evacuateFailsafe.onChatDetection(message);
    }

    private String findTrigger() {
        String reason = dirtCheckFailsafe.check();
        if (reason != null) return reason;

        reason = bedrockCageFailsafe.check();
        if (reason != null) return reason;

        reason = rotationFailsafe.check();
        if (reason != null) return reason;

        reason = teleportFailsafe.check(MacroHandler.getInstance().isTeleporting());
        if (reason != null) return reason;

        return fullInventoryFailsafe.check();
    }

    private void resetState() {
        dirtCheckFailsafe.reset();
        rotationFailsafe.reset();
        teleportFailsafe.reset();
    }
}
