package com.reimo.reimohelper.handler;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.feature.DiscordWebhookService;
import com.reimo.reimohelper.feature.MacroStatsStore;
import com.reimo.reimohelper.feature.RewarpManager;
import com.reimo.reimohelper.feature.UngrabMouse;
import com.reimo.reimohelper.macro.AbstractMacro;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MacroHandler {
    private static MacroHandler instance;
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Minecraft MC = Minecraft.getInstance();

    private Optional<AbstractMacro> currentMacro = Optional.empty();
    private boolean isMacroRunning = false;
    private boolean isTeleporting = false;
    private long teleportExpectedUntilMs = 0L;
    private long macroStartedAtMs = 0L;
    private String lastStartError = "";
    private long flightTapStartedAtMs = 0L;
    private long flightStabilizeUntilMs = 0L;

    public Optional<AbstractMacro> getCurrentMacro() { return currentMacro; }
    public void setCurrentMacro(Optional<AbstractMacro> macro) { this.currentMacro = macro; }
    public boolean isMacroRunning() { return isMacroRunning; }
    public void setMacroRunning(boolean running) { this.isMacroRunning = running; }
    public boolean isTeleporting() { return isTeleporting || System.currentTimeMillis() < teleportExpectedUntilMs; }
    public void setTeleporting(boolean teleporting) { this.isTeleporting = teleporting; }
    public long getMacroStartedAtMs() { return macroStartedAtMs; }
    public long getMacroRuntimeSeconds() {
        if (!isMacroRunning || macroStartedAtMs <= 0L) return 0L;
        return Math.max(0L, (System.currentTimeMillis() - macroStartedAtMs) / 1000L);
    }

    private MacroHandler() {
    }

    public static MacroHandler getInstance() {
        if (instance == null) {
            instance = new MacroHandler();
        }
        return instance;
    }

    public String getLastStartError() { return lastStartError; }

    public boolean enableMacro() {
        ReimoHelperConfig config = ReimoHelperConfig.getInstance();
        lastStartError = "";

        if (!RewarpManager.getInstance().isRewarpSet()) {
            lastStartError = "Set rewarp position first";
            LOGGER.warn("Macro start blocked: {}", lastStartError);
            if (MC.player != null) {
                MC.player.displayClientMessage(
                        Component.literal("[ReimoHelper] " + lastStartError).withStyle(ChatFormatting.RED),
                        false
                );
            }
            return false;
        }

        if (!currentMacro.isPresent() || !isCurrentMacroMatchingConfig(config)) {
            currentMacro = Optional.empty();
            createMacroFromConfig();
        }

        if (!preparePlayerForFarmingStart()) {
            return false;
        }
        
        if (currentMacro.isPresent()) {
            LOGGER.info("Enabling macro: {}", currentMacro.get().getClass().getSimpleName());
            currentMacro.get().onEnable();
            UngrabMouse.getInstance().start();
            isMacroRunning = true;
            macroStartedAtMs = System.currentTimeMillis();
            String playerName = MC.player != null ? MC.player.getName().getString() : MC.getUser().getName();
            MacroStatsStore.getInstance().onMacroStart(playerName);
            config.macroEnabled = true;
            config.save();
            DiscordWebhookService.sendMacroStarted();
            return true;
        }
        lastStartError = "Could not create macro instance";
        return false;
    }

    private boolean isCurrentMacroMatchingConfig(com.reimo.reimohelper.config.ReimoHelperConfig config) {
        if (currentMacro.isEmpty()) {
            return false;
        }

        AbstractMacro macro = currentMacro.get();
        return macro instanceof com.reimo.reimohelper.macro.impl.LayerDropFarmMacro;
    }

    private void createMacroFromConfig() {
        ReimoHelperConfig config = ReimoHelperConfig.getInstance();
        // adjust to new enum type; default to LAYERDROP when creating
        config.macroType = ReimoHelperConfig.MacroType.LAYERDROP;
        com.reimo.reimohelper.macro.AbstractMacro macro = new com.reimo.reimohelper.macro.impl.LayerDropFarmMacro();
        
        if (macro != null) {
            currentMacro = Optional.of(macro);
            LOGGER.info("Macro created: {}", config.macroType);
        }
    }

    public void disableMacro() {
        if (currentMacro.isPresent()) {
            LOGGER.info("Disabling macro: {}", currentMacro.get().getClass().getSimpleName());
            currentMacro.get().onDisable();
            UngrabMouse.getInstance().stop();
            long runtimeSeconds = getMacroRuntimeSeconds();
            isMacroRunning = false;
            macroStartedAtMs = 0L;
            flightTapStartedAtMs = 0L;
            flightStabilizeUntilMs = 0L;
            MacroStatsStore.getInstance().onMacroStop(runtimeSeconds);
            ReimoHelperConfig config = ReimoHelperConfig.getInstance();
            config.macroEnabled = false;
            config.save();
            DiscordWebhookService.sendMacroStopped(runtimeSeconds);
        }
    }

    public boolean toggleMacro() {
        if (isMacroRunning) {
            disableMacro();
            return false;
        } else {
            return enableMacro();
        }
    }

    public void setMacro(AbstractMacro macro) {
        if (currentMacro.isPresent()) {
            disableMacro();
        }
        currentMacro = Optional.of(macro);
        LOGGER.info("Macro set to: {}", macro.getClass().getSimpleName());
    }

    public void onTick() {
        handlePendingFlightTap();
        if (isMacroRunning && System.currentTimeMillis() < flightStabilizeUntilMs) {
            return;
        }
        if (isMacroRunning && currentMacro.isPresent()) {
            currentMacro.get().onTick();
        }
    }

    public boolean isMacroActive() {
        return isMacroRunning && currentMacro.isPresent() && currentMacro.get().isEnabled();
    }

    public void saveMacroState() {
        if (currentMacro.isPresent()) {
            currentMacro.get().saveState();
        }
    }

    public void triggerWarpGarden(boolean tpToRewarp, boolean tpToSpawn) {
        if (currentMacro.isPresent()) {
            LOGGER.info("Warping garden - toRewarp: {}, toSpawn: {}", tpToRewarp, tpToSpawn);
            isTeleporting = true;
            markTeleportExpected(8000);
            com.reimo.reimohelper.feature.RewarpManager.getInstance().executeReward();
            isTeleporting = false;
            currentMacro.get().actionAfterTeleport();
        }
    }

    public void markTeleportExpected(long durationMs) {
        teleportExpectedUntilMs = Math.max(teleportExpectedUntilMs, System.currentTimeMillis() + Math.max(0, durationMs));
    }

    public boolean canTriggerFeatureAfterWarp(boolean afterWarp) {
        return !isTeleporting;
    }

    private boolean preparePlayerForFarmingStart() {
        if (MC.player == null) {
            lastStartError = "Player not ready";
            return false;
        }

        if (MC.player.getAbilities().flying) {
            scheduleFlightDisableByDoubleSpace();
            LOGGER.info("Detected flying; scheduling double-space to disable flight");
        }

        int hoeSlot = findHotbarHoeSlot();
        if (hoeSlot == -1) {
            lastStartError = "You need a hoe in your hotbar to farm";
            LOGGER.warn("Macro start blocked: {}", lastStartError);
            MC.player.displayClientMessage(
                    Component.literal("[ReimoHelper] " + lastStartError).withStyle(ChatFormatting.RED),
                    false
            );
            return false;
        }

        MC.player.getInventory().setSelectedSlot(hoeSlot);
        LOGGER.info("Switched to hoe slot {}", hoeSlot + 1);
        return true;
    }

    private int findHotbarHoeSlot() {
        if (MC.player == null) {
            return -1;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof HoeItem) {
                return i;
            }
        }
        return -1;
    }

    private void scheduleFlightDisableByDoubleSpace() {
        long now = System.currentTimeMillis();
        flightTapStartedAtMs = now;
        // two taps take ~180ms, then wait 1s for stable fall before macro actions.
        flightStabilizeUntilMs = now + 1180L;
    }

    private void handlePendingFlightTap() {
        if (flightTapStartedAtMs == 0L || MC.player == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - flightTapStartedAtMs;
        // Simulate quick double-press of space:
        // 0-60ms press, 60-120ms release, 120-180ms press, 180ms+ release.
        if (elapsed < 60L) {
            MC.options.keyJump.setDown(true);
        } else if (elapsed < 120L) {
            MC.options.keyJump.setDown(false);
        } else if (elapsed < 180L) {
            MC.options.keyJump.setDown(true);
        } else {
            MC.options.keyJump.setDown(false);
            flightTapStartedAtMs = 0L;
        }
    }
}
