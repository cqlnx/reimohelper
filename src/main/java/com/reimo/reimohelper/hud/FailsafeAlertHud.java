package com.reimo.reimohelper.hud;

import com.reimo.reimohelper.failsafe.impl.EvacuateFailsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

public class FailsafeAlertHud {
    private static final Minecraft MC = Minecraft.getInstance();

    public static void onRender(CustomizeGuiOverlayEvent.Chat event) {
        if (MC.player == null) return;
        EvacuateFailsafe fs = EvacuateFailsafe.getInstance();
        if (!fs.isAlertActive()) return;
        if (fs.isEvacuateAlert()) return;  // Don't show HUD for evacuate alerts

        GuiGraphics gg = event.getGuiGraphics();
        int cx = MC.getWindow().getGuiScaledWidth() / 2;
        int y = 26;
        gg.fill(cx - 168, y - 8, cx + 168, y + 24, 0xCC3A0B0B);
        gg.fill(cx - 168, y - 8, cx + 168, y - 6, 0xFFFF5C5C);
        gg.drawCenteredString(MC.font, "FAILSAFE TRIGGERED", cx, y, 0xFFFFCCCC);
        gg.drawCenteredString(MC.font, "Press K to stop alarm | " + fs.getActiveReason(), cx, y + 12, 0xFFFFFFFF);
    }
}
