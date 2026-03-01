package com.reimo.reimohelper.screen;

import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.feature.RewarpManager;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.hud.InventoryPreviewHud;
import com.reimo.reimohelper.hud.StatusHud;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ReimoHelperScreen extends Screen {
    private enum MenuTab {
        GENERAL,
        HUD,
        FAILSAFE
    }

    private final ReimoHelperConfig config = ReimoHelperConfig.getInstance();
    private MenuTab activeTab = MenuTab.GENERAL;

    private static final int PANEL_W = 302;
    private static final int PANEL_H = 360;
    private static final int BUTTON_W = 218;
    private static final int BUTTON_H = 20;
    private static final int ROW_GAP = 7;

    private final List<AbstractWidget> generalButtons = new ArrayList<>();
    private final List<AbstractWidget> hudButtons = new ArrayList<>();
    private final List<AbstractWidget> failsafeButtons = new ArrayList<>();

    private Button macroButton;
    private Button ungrabButton;
    private Button previewButton;
    private Button evacuateRebootButton;
    private Button pestHighlightButton;
    private Button statusHudButton;
    private Button debugHudButton;
    private Button hudEditButton;
    private Button webhookButton;
    private AbstractSliderButton soundSlider;
    private AbstractSliderButton webhookIntervalSlider;
    private Button webhookUrlButton;
    private String webhookUrlInput = "";
    private boolean editingWebhookUrl = false;
    private Button tabGeneralButton;
    private Button tabHudButton;
    private Button tabFailsafeButton;
    private Button closeButton;

    private String status = "Ready";
    private int statusColor = 0xFF9FE19F;

    private boolean hudEditMode = false;
    private enum DragTarget { NONE, INVENTORY, STATUS }
    private DragTarget dragTarget = DragTarget.NONE;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public ReimoHelperScreen() {
        super(Component.literal("ReimoHelper"));
    }

    @Override
    protected void init() {
        clearWidgets();
        generalButtons.clear();
        hudButtons.clear();
        failsafeButtons.clear();

        int panelW = Math.min(PANEL_W, Math.max(220, width - 16));
        int panelH = Math.min(PANEL_H, Math.max(240, height - 16));
        int bw = Math.min(BUTTON_W, panelW - 28);
        int cx = width / 2;
        int panelTop = (height - panelH) / 2;
        int panelBottom = panelTop + panelH;
        int step = BUTTON_H + ROW_GAP;

        int tabY = panelTop + 32;
        int tabW = 84;
        tabGeneralButton = addRenderableWidget(Button.builder(Component.literal("General"), b -> setTab(MenuTab.GENERAL))
                .pos(cx - tabW - 44, tabY).size(tabW, 18).build());
        tabHudButton = addRenderableWidget(Button.builder(Component.literal("HUD"), b -> setTab(MenuTab.HUD))
                .pos(cx - tabW / 2, tabY).size(tabW, 18).build());
        tabFailsafeButton = addRenderableWidget(Button.builder(Component.literal("Failsafe"), b -> setTab(MenuTab.FAILSAFE))
                .pos(cx + 44, tabY).size(tabW, 18).build());

        int contentY = panelTop + 60;

        macroButton = addGeneralButton(Button.builder(
                Component.literal(config.macroEnabled ? "Stop Macro (~)" : "Start Macro (~)"),
                b -> onToggleMacro()
        ).pos(cx - bw / 2, contentY).size(bw, BUTTON_H).build());

        addGeneralButton(Button.builder(
                Component.literal("Set Rewarp Position"),
                b -> onSetRewarp()
        ).pos(cx - bw / 2, contentY + step).size(bw, BUTTON_H).build());

        ungrabButton = addGeneralButton(Button.builder(
                Component.literal("Auto Ungrab: " + onOff(config.autoUngrabMouse)),
                b -> onToggleUngrab()
        ).pos(cx - bw / 2, contentY + step * 2).size(bw, BUTTON_H).build());

        soundSlider = addGeneralButton(new AbstractSliderButton(
                cx - bw / 2,
                contentY + step * 3 + 2,
                bw,
                BUTTON_H,
                Component.literal("Game Sound: " + config.gameVolumePercent + "%"),
                Math.max(0.0, Math.min(1.0, config.gameVolumePercent / 100.0))
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal("Game Sound: " + (int) Math.round(this.value * 100.0) + "%"));
            }

            @Override
            protected void applyValue() {
                config.gameVolumePercent = (int) Math.round(this.value * 100.0);
                status = "Game sound set to " + config.gameVolumePercent + "%";
                statusColor = 0xFF9FE19F;
            }
        });

        previewButton = addHudButton(Button.builder(
                Component.literal("Inventory Preview: " + onOff(config.inventoryPreviewEnabled)),
                b -> onTogglePreview()
        ).pos(cx - bw / 2, contentY).size(bw, BUTTON_H).build());

        pestHighlightButton = addHudButton(Button.builder(
                Component.literal("Highlight Pests: " + onOff(config.highlightPests)),
                b -> onTogglePestHighlight()
        ).pos(cx - bw / 2, contentY + step).size(bw, BUTTON_H).build());

        statusHudButton = addHudButton(Button.builder(
                Component.literal("Status HUD: " + onOff(config.showStatusHud)),
                b -> onToggleStatusHud()
        ).pos(cx - bw / 2, contentY + step * 2).size(bw, BUTTON_H).build());

        debugHudButton = addHudButton(Button.builder(
                Component.literal("Debug HUD: " + onOff(config.showDebugHud)),
                b -> onToggleDebugHud()
        ).pos(cx - bw / 2, contentY + step * 3).size(bw, BUTTON_H).build());

        hudEditButton = addHudButton(Button.builder(
                Component.literal("Edit HUD Layout"),
                b -> toggleHudEditMode()
        ).pos(cx - bw / 2, contentY + step * 4 + 2).size(bw, BUTTON_H).build());

        evacuateRebootButton = addFailsafeButton(Button.builder(
                Component.literal("Evacuate Reboot: " + onOff(config.autoEvacuateOnServerReboot)),
                b -> onToggleEvacuateReboot()
        ).pos(cx - bw / 2, contentY).size(bw, BUTTON_H).build());

        webhookButton = addFailsafeButton(Button.builder(
                Component.literal("Discord Webhook: " + onOff(config.discordWebhookEnabled)),
                b -> onToggleWebhook()
        ).pos(cx - bw / 2, contentY + step).size(bw, BUTTON_H).build());

        webhookIntervalSlider = addFailsafeButton(new AbstractSliderButton(
                cx - bw / 2,
                contentY + step * 2 + 2,
                bw,
                BUTTON_H,
                Component.literal("Webhook Interval: " + config.discordWebhookIntervalSeconds + "s"),
                Math.max(0.0, Math.min(1.0, (config.discordWebhookIntervalSeconds - 10) / 590.0))
        ) {
            @Override
            protected void updateMessage() {
                int interval = 10 + (int) Math.round(this.value * 590.0);
                setMessage(Component.literal("Webhook Interval: " + interval + "s"));
            }

            @Override
            protected void applyValue() {
                config.discordWebhookIntervalSeconds = 10 + (int) Math.round(this.value * 590.0);
                status = "Webhook interval set to " + config.discordWebhookIntervalSeconds + "s";
                statusColor = 0xFF9FE19F;
                config.save();
            }
        });

        webhookUrlButton = addFailsafeButton(Button.builder(
                Component.literal("Webhook URL: " + (config.discordWebhookUrl.isEmpty() ? "(empty)" : "***")),
                b -> onEditWebhookUrl()
        ).pos(cx - bw / 2, contentY + step * 3 + 2).size(bw, BUTTON_H).build());

        closeButton = addRenderableWidget(Button.builder(
                Component.literal("Close"),
                b -> onClose()
        ).pos(cx - bw / 2, panelBottom - 28).size(bw, BUTTON_H).build());

        applyTabVisibility();
        updateTabLabels();
    }

    private <T extends AbstractWidget> T addGeneralButton(T button) {
        generalButtons.add(addRenderableWidget(button));
        return button;
    }

    private <T extends AbstractWidget> T addHudButton(T button) {
        hudButtons.add(addRenderableWidget(button));
        return button;
    }

    private <T extends AbstractWidget> T addFailsafeButton(T button) {
        failsafeButtons.add(addRenderableWidget(button));
        return button;
    }

    private void setTab(MenuTab tab) {
        activeTab = tab;
        hudEditMode = false;
        dragTarget = DragTarget.NONE;
        if (hudEditButton != null) hudEditButton.setMessage(Component.literal("Edit HUD Layout"));
        applyTabVisibility();
        updateTabLabels();
    }

    private void applyTabVisibility() {
        boolean general = activeTab == MenuTab.GENERAL && !hudEditMode;
        boolean hud = activeTab == MenuTab.HUD && !hudEditMode;
        boolean failsafe = activeTab == MenuTab.FAILSAFE && !hudEditMode;

        setButtonsVisible(generalButtons, general);
        setButtonsVisible(hudButtons, hud);
        setButtonsVisible(failsafeButtons, failsafe);

        if (activeTab == MenuTab.HUD) {
            hudEditButton.visible = true;
            hudEditButton.active = true;
        }
        if (soundSlider != null) {
            soundSlider.visible = general;
            soundSlider.active = general;
        }
    }

    private void setButtonsVisible(List<AbstractWidget> buttons, boolean visible) {
        for (AbstractWidget button : buttons) {
            button.visible = visible;
            button.active = visible;
        }
    }

    private void updateTabLabels() {
        tabGeneralButton.setMessage(Component.literal((activeTab == MenuTab.GENERAL ? "> " : "") + "General"));
        tabHudButton.setMessage(Component.literal((activeTab == MenuTab.HUD ? "> " : "") + "HUD"));
        tabFailsafeButton.setMessage(Component.literal((activeTab == MenuTab.FAILSAFE ? "> " : "") + "Failsafe"));
    }

    private void toggleHudEditMode() {
        if (activeTab != MenuTab.HUD) return;
        hudEditMode = !hudEditMode;
        dragTarget = DragTarget.NONE;
        hudEditButton.setMessage(Component.literal(hudEditMode ? "Finish HUD Edit" : "Edit HUD Layout"));
        if (hudEditMode) {
            status = "Drag HUD boxes. Scroll over box to resize.";
            statusColor = 0xFFB7C6D8;
        } else {
            status = "HUD layout saved";
            statusColor = 0xFF9FE19F;
            config.save();
        }
        applyTabVisibility();
    }

    private void onToggleMacro() {
        MacroHandler handler = MacroHandler.getInstance();
        if (config.macroEnabled) {
            handler.disableMacro();
            config.macroEnabled = false;
            status = "Macro stopped";
            statusColor = 0xFFFFAA66;
        } else {
            boolean started = handler.enableMacro();
            config.macroEnabled = started;
            status = started ? "Macro running" : handler.getLastStartError();
            statusColor = started ? 0xFF9FE19F : 0xFFFF7777;
        }
        macroButton.setMessage(Component.literal(config.macroEnabled ? "Stop Macro (~)" : "Start Macro (~)"));
        config.save();
    }

    private void onSetRewarp() {
        if (minecraft == null || minecraft.player == null) return;
        RewarpManager.getInstance().setRewarpLocation(minecraft.player.getOnPos());
        status = "Rewarp saved";
        statusColor = 0xFF9FE19F;
    }

    private void onToggleUngrab() {
        config.autoUngrabMouse = !config.autoUngrabMouse;
        ungrabButton.setMessage(Component.literal("Auto Ungrab: " + onOff(config.autoUngrabMouse)));
        status = "Auto ungrab " + (config.autoUngrabMouse ? "enabled" : "disabled");
        statusColor = 0xFF9FE19F;
        config.save();
    }

    private void onTogglePreview() {
        config.inventoryPreviewEnabled = !config.inventoryPreviewEnabled;
        previewButton.setMessage(Component.literal("Inventory Preview: " + onOff(config.inventoryPreviewEnabled)));
        status = "Inventory preview " + (config.inventoryPreviewEnabled ? "enabled" : "disabled");
        statusColor = 0xFF9FE19F;
        config.save();
    }

    private void onTogglePestHighlight() {
        config.highlightPests = !config.highlightPests;
        pestHighlightButton.setMessage(Component.literal("Highlight Pests: " + onOff(config.highlightPests)));
        status = "Pest highlight " + (config.highlightPests ? "enabled" : "disabled");
        statusColor = 0xFF9FE19F;
        config.save();
    }

    private void onToggleStatusHud() {
        config.showStatusHud = !config.showStatusHud;
        statusHudButton.setMessage(Component.literal("Status HUD: " + onOff(config.showStatusHud)));
        status = "Status HUD " + (config.showStatusHud ? "enabled" : "disabled");
        statusColor = 0xFF9FE19F;
        config.save();
    }

    private void onToggleDebugHud() {
        config.showDebugHud = !config.showDebugHud;
        debugHudButton.setMessage(Component.literal("Debug HUD: " + onOff(config.showDebugHud)));
        status = "Debug HUD " + (config.showDebugHud ? "enabled" : "disabled");
        statusColor = 0xFF9FE19F;
        config.save();
    }

    private void onToggleEvacuateReboot() {
        config.autoEvacuateOnServerReboot = !config.autoEvacuateOnServerReboot;
        evacuateRebootButton.setMessage(Component.literal("Evacuate Reboot: " + onOff(config.autoEvacuateOnServerReboot)));
        status = "Evacuate reboot " + (config.autoEvacuateOnServerReboot ? "enabled" : "disabled");
        statusColor = 0xFF9FE19F;
        config.save();
    }

    private void onToggleWebhook() {
        config.discordWebhookEnabled = !config.discordWebhookEnabled;
        webhookButton.setMessage(Component.literal("Discord Webhook: " + onOff(config.discordWebhookEnabled)));
        status = "Discord webhook " + (config.discordWebhookEnabled ? "enabled" : "disabled");
        statusColor = 0xFF9FE19F;
        config.save();
    }

    private void onSetWebhookUrl() {
        editingWebhookUrl = !editingWebhookUrl;
        webhookUrlInput = config.discordWebhookUrl == null ? "" : config.discordWebhookUrl;
        if (editingWebhookUrl) {
            status = "Editing webhook URL... (Paste with Ctrl+V, Enter to confirm)";
            statusColor = 0xFFB7C6D8;
        } else {
            config.discordWebhookUrl = webhookUrlInput;
            status = "Webhook URL saved";
            statusColor = 0xFF9FE19F;
            config.save();
        }
        webhookUrlButton.setMessage(Component.literal("Webhook URL: " + (webhookUrlInput.isEmpty() ? "(empty)" : "***")));
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private void onEditWebhookUrl() {
        editingWebhookUrl = !editingWebhookUrl;
        if (editingWebhookUrl) {
            status = "Webhook URL input mode - Press Ctrl+C then paste in config file, or edit config/reimohelper/config.json";
            statusColor = 0xFFB7C6D8;
        } else {
            status = "Webhook URL mode closed";
            statusColor = 0xFF9FE19F;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (hudEditMode && event.button() == 0) {
            if (insideInventory(mouseX, mouseY)) {
                dragTarget = DragTarget.INVENTORY;
                dragOffsetX = (int) mouseX - config.inventoryPreviewX;
                dragOffsetY = (int) mouseY - config.inventoryPreviewY;
                return true;
            }
            if (insideStatus(mouseX, mouseY)) {
                dragTarget = DragTarget.STATUS;
                dragOffsetX = (int) mouseX - config.statusHudX;
                dragOffsetY = (int) mouseY - config.statusHudY;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (hudEditMode && event.button() == 0 && dragTarget != DragTarget.NONE) {
            if (dragTarget == DragTarget.INVENTORY) {
                int w = InventoryPreviewHud.getPreviewWidth(config) + 6;
                int h = InventoryPreviewHud.getPreviewHeight(config) + 6;
                config.inventoryPreviewX = clamp((int) mouseX - dragOffsetX, 4, width - w - 4);
                config.inventoryPreviewY = clamp((int) mouseY - dragOffsetY, 4, height - h - 4);
                return true;
            } else if (dragTarget == DragTarget.STATUS) {
                int w = StatusHud.getHudWidth(config);
                int h = StatusHud.getHudHeight(config, debugLineCount()) + 2;
                config.statusHudX = clamp((int) mouseX - dragOffsetX, 4, width - w - 4);
                config.statusHudY = clamp((int) mouseY - dragOffsetY, 4, height - h - 4);
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (hudEditMode && dragTarget != DragTarget.NONE) {
            dragTarget = DragTarget.NONE;
            config.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hudEditMode) {
            int delta = scrollY > 0 ? 1 : -1;
            if (insideInventory(mouseX, mouseY)) {
                config.inventoryPreviewScale = clamp(config.inventoryPreviewScale + delta, 1, 4);
                config.save();
                return true;
            }
            if (insideStatus(mouseX, mouseY)) {
                config.statusHudScale = clamp(config.statusHudScale + delta, 1, 4);
                config.save();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean insideInventory(double mx, double my) {
        int x = config.inventoryPreviewX - 3;
        int y = config.inventoryPreviewY - 3;
        int w = InventoryPreviewHud.getPreviewWidth(config) + 6;
        int h = InventoryPreviewHud.getPreviewHeight(config) + 6;
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }

    private boolean insideStatus(double mx, double my) {
        int x = config.statusHudX;
        int y = config.statusHudY;
        int w = StatusHud.getHudWidth(config);
        int h = StatusHud.getHudHeight(config, debugLineCount());
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }

    private int debugLineCount() {
        int base = 7;
        if (config.showDebugHud) base += 3;
        return base;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() {
        config.save();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float pt) {
        renderTransparentBackground(gg);
        int panelW = Math.min(PANEL_W, Math.max(220, width - 16));
        int panelH = Math.min(PANEL_H, Math.max(240, height - 16));
        int cx = width / 2;
        int l = cx - panelW / 2;
        int r = cx + panelW / 2;
        int t = (height - panelH) / 2;
        int b = t + panelH;

        gg.fill(l + 2, t + 2, r + 2, b + 2, 0x66000000);
        gg.fill(l, t, r, b, 0xD8121821);
        gg.fill(l + 1, t + 1, r - 1, t + 28, 0xA01E2A38);
        gg.fill(l, t, r, t + 2, 0xFF2EC4B6);
        gg.fill(l, b - 2, r, b, 0xFF2EC4B6);

        gg.fill(l + 16, t + 53, r - 16, t + 54, 0x553F5368);
        gg.fill(l + 16, b - 78, r - 16, b - 77, 0x553F5368);

        gg.drawCenteredString(font, "ReimoHelper", cx, t + 10, 0xFFFFFFFF);
        gg.drawCenteredString(font, "Menu: Right Shift | Toggle: ~", cx, t + 20, 0xFFB7C6D8);

        if (activeTab == MenuTab.FAILSAFE && !hudEditMode) {
            gg.drawCenteredString(font, "Failsafe options", cx, t + 95, 0xFFB7C6D8);
        }

        if (hudEditMode) {
            drawHudEditor(gg);
        }

        super.render(gg, mx, my, pt);

        String rewarp = RewarpManager.getInstance().isRewarpSet() ? "Rewarp: Set" : "Rewarp: Not Set";
        int rewarpColor = RewarpManager.getInstance().isRewarpSet() ? 0xFF84F78A : 0xFFFF7A7A;

        gg.drawCenteredString(font, rewarp, cx, b - 72, rewarpColor);
        gg.drawCenteredString(font, hudEditMode ? "Drag boxes to move | Scroll on box to resize" : "Open HUD tab for layout editor", cx, b - 61, 0xFFB7C6D8);
        gg.drawCenteredString(font, status, cx, b - 50, statusColor);
    }

    private void drawHudEditor(GuiGraphics gg) {
        int invX = config.inventoryPreviewX - 3;
        int invY = config.inventoryPreviewY - 3;
        int invW = InventoryPreviewHud.getPreviewWidth(config) + 6;
        int invH = InventoryPreviewHud.getPreviewHeight(config) + 6;

        int stX = config.statusHudX;
        int stY = config.statusHudY;
        int stW = StatusHud.getHudWidth(config);
        int stH = StatusHud.getHudHeight(config, debugLineCount());

        gg.fill(invX, invY, invX + invW, invY + invH, 0x3312B6A4);
        gg.fill(invX, invY, invX + invW, invY + 2, 0xFF2EC4B6);
        gg.fill(invX, invY + invH - 2, invX + invW, invY + invH, 0xFF2EC4B6);
        gg.fill(invX + invW - 6, invY + invH - 6, invX + invW - 2, invY + invH - 2, 0xFFFFFFFF);
        gg.drawString(font, "Inventory HUD", invX + 4, invY - 10, 0xFFD1F5F0, false);

        gg.fill(stX, stY, stX + stW, stY + stH, 0x332E66C7);
        gg.fill(stX, stY, stX + stW, stY + 2, 0xFF5CA8FF);
        gg.fill(stX, stY + stH - 2, stX + stW, stY + stH, 0xFF5CA8FF);
        gg.fill(stX + stW - 6, stY + stH - 6, stX + stW - 2, stY + stH - 2, 0xFFFFFFFF);
        gg.drawString(font, "Status HUD", stX + 4, stY - 10, 0xFFD6E8FF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
