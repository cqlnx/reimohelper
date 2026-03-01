package com.reimo.reimohelper.failsafe.impl;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.handler.MacroHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class EvacuateFailsafe {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Minecraft MC = Minecraft.getInstance();
    private static EvacuateFailsafe instance;

    private boolean alertActive = false;
    private String activeReason = "";
    private long lastSoundMs = 0L;
    private static final long ALARM_INTERVAL_MS = 900L;

    private EvacuateState state = EvacuateState.NONE;
    private long nextActionAtMs = 0L;
    private boolean resumeMacroAfter = false;

    public static EvacuateFailsafe getInstance() {
        if (instance == null) {
            instance = new EvacuateFailsafe();
        }
        return instance;
    }

    public boolean isAlertActive() {
        return alertActive;
    }

    public String getActiveReason() {
        return activeReason;
    }

    public void onChatDetection(String rawMessage) {
        if (rawMessage == null) return;
        String msg = rawMessage.toLowerCase(Locale.ROOT);
        if (msg.contains("server is about to") || msg.contains("server reboot") || msg.contains("can't use this when the server is about to")) {
            if (MacroHandler.getInstance().isMacroActive() && ReimoHelperConfig.getInstance().autoEvacuateOnServerReboot) {
                startEvacuation("EVACUATE: server reboot warning");
            }
        }
    }

    public void alertOnly(String reason) {
        if (MC.player == null) return;
        alertActive = true;
        activeReason = reason;
        MC.player.displayClientMessage(
                Component.literal("[ReimoHelper] Failsafe: " + reason + " | Press K to stop alarm")
                        .withStyle(ChatFormatting.RED),
                false
        );
        playAlarmNow();
        LOGGER.warn("Failsafe alert active: {}", reason);
    }

    public void onClientTick() {
        if (alertActive && MC.player != null && System.currentTimeMillis() - lastSoundMs >= ALARM_INTERVAL_MS) {
            playAlarmNow();
        }
        runEvacuationState();
    }

    public void acknowledge() {
        if (!alertActive) return;
        alertActive = false;
        if (MC.player != null) {
            MC.player.displayClientMessage(
                    Component.literal("[ReimoHelper] Failsafe alarm acknowledged").withStyle(ChatFormatting.YELLOW),
                    false
            );
        }
        LOGGER.info("Failsafe alarm acknowledged");
    }

    private void startEvacuation(String reason) {
        if (state != EvacuateState.NONE) return;
        alertOnly(reason);
        resumeMacroAfter = MacroHandler.getInstance().isMacroActive();
        MacroHandler.getInstance().disableMacro();
        state = EvacuateState.EVACUATE_FROM_ISLAND;
        nextActionAtMs = System.currentTimeMillis() + 700L;
        LOGGER.warn("Evacuation flow started");
    }

    private void runEvacuationState() {
        if (state == EvacuateState.NONE || MC.player == null || MC.player.connection == null) return;
        if (System.currentTimeMillis() < nextActionAtMs) return;

        switch (state) {
            case EVACUATE_FROM_ISLAND:
                sendCommand("/evacuate");
                state = EvacuateState.TP_BACK_TO_ISLAND;
                nextActionAtMs = System.currentTimeMillis() + 5500L;
                break;
            case TP_BACK_TO_ISLAND:
                sendCommand("/warp garden");
                state = EvacuateState.END;
                nextActionAtMs = System.currentTimeMillis() + 6500L;
                break;
            case END:
                if (MC.player != null) {
                    MC.player.displayClientMessage(
                            Component.literal("[ReimoHelper] Evacuation flow complete").withStyle(ChatFormatting.GREEN),
                            false
                    );
                }
                if (resumeMacroAfter) {
                    MacroHandler.getInstance().enableMacro();
                }
                resumeMacroAfter = false;
                state = EvacuateState.NONE;
                break;
            default:
                state = EvacuateState.NONE;
                break;
        }
    }

    private void sendCommand(String command) {
        if (MC.player == null || MC.player.connection == null) return;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        MC.player.connection.sendCommand(cmd);
        LOGGER.info("Evacuation command sent: /{}", cmd);
    }

    private void playAlarmNow() {
        if (MC.player == null) return;
        float master = (float) MC.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER);
        float scaled = Math.max(1.0F, 1.0F / Math.max(0.1F, master));
        MC.player.playSound(SoundEvents.ANVIL_LAND, scaled, 0.9F);
        lastSoundMs = System.currentTimeMillis();
    }

    enum EvacuateState {
        NONE,
        EVACUATE_FROM_ISLAND,
        TP_BACK_TO_ISLAND,
        END
    }
}
