package com.reimo.reimohelper;

import com.mojang.logging.LogUtils;
import com.reimo.reimohelper.event.ChatEventHandler;
import net.minecraft.client.Minecraft;
import com.reimo.reimohelper.event.ClientTickHandler;
import com.reimo.reimohelper.hud.FailsafeAlertHud;
import com.reimo.reimohelper.hud.InventoryPreviewHud;
import com.reimo.reimohelper.hud.PestHighlightHud;
import com.reimo.reimohelper.hud.RewarpHighlightHud;
import com.reimo.reimohelper.hud.StatusHud;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ReimoHelperMod.MODID)
public final class ReimoHelperMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "reimohelper";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ReimoHelperMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register the commonSetup method for modloading
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        FMLClientSetupEvent.getBus(modBusGroup).addListener(ClientModEvents::onClientSetup);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // Manually register client-side listeners instead of using @EventBusSubscriber
    public static class ClientModEvents {
        public static void onClientSetup(FMLClientSetupEvent event) {
            TickEvent.ClientTickEvent.Pre.BUS.addListener(ClientTickHandler::onTick);
            TickEvent.ClientTickEvent.Post.BUS.addListener(ClientTickHandler::onTickPost);
            CustomizeGuiOverlayEvent.Chat.BUS.addListener(InventoryPreviewHud::onRender);
            CustomizeGuiOverlayEvent.Chat.BUS.addListener(FailsafeAlertHud::onRender);
            CustomizeGuiOverlayEvent.Chat.BUS.addListener(PestHighlightHud::onRender);
            CustomizeGuiOverlayEvent.Chat.BUS.addListener(StatusHud::onRender);
            CustomizeGuiOverlayEvent.Chat.BUS.addListener(RewarpHighlightHud::onRender);
            SystemMessageReceivedEvent.BUS.addListener(ChatEventHandler::onSystemMessage);
            // also register for vanilla chat events so command-block and generic server messages are caught
            net.minecraftforge.client.event.ClientChatReceivedEvent.BUS.addListener(ChatEventHandler::onChat);

            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
