package com.reimo.reimohelper.hud;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

public class InventoryPreviewHud {
    private static final Minecraft MC = Minecraft.getInstance();

    public static int getPreviewWidth(ReimoHelperConfig config) {
        int scale = Math.max(1, Math.min(config.inventoryPreviewScale, 4));
        int slot = 16;
        int pad = 2;
        int step = slot + pad + (scale - 1) * 2;
        return 9 * step - pad;
    }

    public static int getPreviewHeight(ReimoHelperConfig config) {
        int scale = Math.max(1, Math.min(config.inventoryPreviewScale, 4));
        int slot = 16;
        int pad = 2;
        int step = slot + pad + (scale - 1) * 2;
        return 4 * step - pad;
    }

    public static void onRender(CustomizeGuiOverlayEvent.Chat event) {
        if (MC.player == null) return;

        ReimoHelperConfig config = ReimoHelperConfig.getInstance();
        if (!config.inventoryPreviewEnabled) return;
        HudLayoutScaler.syncWithWindow(config, MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight());

        GuiGraphics gg = event.getGuiGraphics();
        int scale = Math.max(1, Math.min(config.inventoryPreviewScale, 4));
        int slot = 16;
        int pad = 2;
        int step = slot + pad + (scale - 1) * 2;
        int baseW = getPreviewWidth(config);
        int baseH = getPreviewHeight(config);

        int x = config.inventoryPreviewX;
        int y = config.inventoryPreviewY;
        int width = baseW;
        int height = baseH;

        gg.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0x66000000);
        gg.fill(x - 3, y - 3, x + width + 3, y - 1, 0xFF69D2E7);
        gg.fill(x - 3, y + height + 1, x + width + 3, y + height + 3, 0xFF69D2E7);

        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            int col = i % 9;
            int row = i / 9;
            int drawX = x + col * step;
            int drawY = y + row * step;

            gg.fill(drawX - 1, drawY - 1, drawX + slot + 1, drawY + slot + 1, 0x22000000);
            if (!stack.isEmpty()) {
                gg.renderItem(stack, drawX, drawY);
                gg.renderItemDecorations(MC.font, stack, drawX, drawY);
            }
        }
    }
}
