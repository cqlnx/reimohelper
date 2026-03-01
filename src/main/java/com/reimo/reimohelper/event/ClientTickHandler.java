package com.reimo.reimohelper.event;

import com.reimo.reimohelper.failsafe.FailsafeManager;
import com.reimo.reimohelper.failsafe.impl.EvacuateFailsafe;
import com.reimo.reimohelper.feature.BlockSoundController;
import com.reimo.reimohelper.feature.DiscordWebhookService;
import com.reimo.reimohelper.feature.RewarpHighlightService;
import com.reimo.reimohelper.feature.UngrabMouse;
import com.reimo.reimohelper.handler.MacroHandler;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");

    public static void onTick(TickEvent.ClientTickEvent.Pre event) {
        try {
            MacroHandler.getInstance().onTick();
            FailsafeManager.getInstance().onTick();
            EvacuateFailsafe.getInstance().onClientTick();
            KeybindHandler.onClientTick();
            RewarpHighlightService.onClientTick();
            UngrabMouse.getInstance().onClientTick();
            BlockSoundController.onClientTick();
            DiscordWebhookService.onClientTick();
        } catch (Exception e) {
            LOGGER.error("Error in client tick", e);
        }
    }

    public static void onTickPost(TickEvent.ClientTickEvent.Post event) {
        try {
            UngrabMouse.getInstance().onClientTickPost();
        } catch (Exception e) {
            LOGGER.error("Error in client tick post", e);
        }
    }
}
