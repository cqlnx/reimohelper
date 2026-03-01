package com.reimo.reimohelper.feature;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.failsafe.impl.EvacuateFailsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

public class BlockSoundController {
    private static final Minecraft MC = Minecraft.getInstance();

    public static void onClientTick() {
        if (MC.options == null) return;
        ReimoHelperConfig config = ReimoHelperConfig.getInstance();
        double volume = Math.max(0.0, Math.min(1.0, config.gameVolumePercent / 100.0));
        if (EvacuateFailsafe.getInstance().isAlertActive()) {
            volume = Math.max(volume, 0.35);
        }
        try {
            MC.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(volume);
        } catch (Throwable ignored) {
        }
    }
}
