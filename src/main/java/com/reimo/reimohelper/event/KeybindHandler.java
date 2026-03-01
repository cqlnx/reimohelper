package com.reimo.reimohelper.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.failsafe.impl.EvacuateFailsafe;
import com.reimo.reimohelper.feature.UngrabMouse;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.screen.ReimoHelperScreen;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Minecraft MC = Minecraft.getInstance();

    private static boolean toggleKeyPressed = false;
    private static boolean rightShiftPressed = false;
    private static boolean f8Pressed = false;
    private static boolean acknowledgePressed = false;

    public static void onClientTick() {
        if (MC.getWindow() == null) return;

        boolean isAcknowledgeDown = InputConstants.isKeyDown(MC.getWindow(), InputConstants.KEY_K);
        if (isAcknowledgeDown && !acknowledgePressed) {
            EvacuateFailsafe.getInstance().acknowledge();
        }
        acknowledgePressed = isAcknowledgeDown;

        if (MC.screen == null) {
            ReimoHelperConfig config = ReimoHelperConfig.getInstance();
            boolean isToggleDown = InputConstants.isKeyDown(MC.getWindow(), config.macroToggleKey);
            if (isToggleDown && !toggleKeyPressed) {
                MacroHandler.getInstance().toggleMacro();
            }
            toggleKeyPressed = isToggleDown;

            boolean isRightShiftDown = InputConstants.isKeyDown(MC.getWindow(), InputConstants.KEY_RSHIFT);
            if (isRightShiftDown && !rightShiftPressed) {
                MC.setScreen(new ReimoHelperScreen());
                LOGGER.info("Opened ReimoHelper menu");
            }
            rightShiftPressed = isRightShiftDown;
        } else {
            toggleKeyPressed = false;
            rightShiftPressed = false;
        }

        boolean isF8Down = InputConstants.isKeyDown(MC.getWindow(), InputConstants.KEY_F8);
        if (isF8Down && !f8Pressed) {
            ReimoHelperConfig config = ReimoHelperConfig.getInstance();
            config.autoUngrabMouse = !config.autoUngrabMouse;
            config.save();
            LOGGER.info("Auto ungrab mouse: {}", config.autoUngrabMouse ? "enabled" : "disabled");
            if (!config.autoUngrabMouse) {
                UngrabMouse.getInstance().onDisabledWhileRunning();
            } else if (UngrabMouse.getInstance().isRunning()) {
                UngrabMouse.getInstance().onEnabledWhileRunning();
            }
        }
        f8Pressed = isF8Down;
    }
}
