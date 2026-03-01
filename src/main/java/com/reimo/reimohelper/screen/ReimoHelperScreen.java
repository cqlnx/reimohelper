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
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReimoHelperScreen extends Screen {
    private enum MenuTab {
        GENERAL,
        HUD,
        FAILSAFE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
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

    // simple custom button implementation to give us flat backgrounds and hover
    // highlights without fighting Minecraft's built-in texture.  We deliberately
    // subclass Button so that we can still call setMessage() and use the
    // existing listeners, but override the rendering logic in renderContents.
    private static class StyledButton extends Button {
        public StyledButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
            // the third parameter in this constructor is a narration supplier; just
            // provide a no-op so we can keep the other signature simple.
            super(x, y, w, h, msg, onPress, (button) -> Component.empty());
        }

        @Override
        protected void renderContents(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
            // draw flat background
            int col = this.isMouseOver(mouseX, mouseY) ? 0xFF3A3C40 : 0xFF2E3036;
            gg.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), col);
            // thin accent lines top/bottom
            gg.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, 0xFF2EC4B6);
            gg.fill(this.getX(), this.getY() + this.getHeight() - 1,
                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF2EC4B6);
            // draw label centred vertically
            gg.drawCenteredString(Minecraft.getInstance().font, this.getMessage().getString(),
                    this.getX() + this.getWidth() / 2,
                    this.getY() + (this.getHeight() - 8) / 2,
                    0xFFFFFFFF);
        }
    }

    private StyledButton createButton(int x, int y, int w, int h, Component text, Button.OnPress onPress) {
        return new StyledButton(x, y, w, h, text, onPress);
    }


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
    private Button pasteBtn;
    private String lastClipboardBackend = "";
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
        tabGeneralButton = addRenderableWidget(createButton(cx - tabW - 44, tabY, tabW, 18,
                Component.literal("General"), b -> setTab(MenuTab.GENERAL)));
        tabHudButton = addRenderableWidget(createButton(cx - tabW / 2, tabY, tabW, 18,
                Component.literal("HUD"), b -> setTab(MenuTab.HUD)));
        tabFailsafeButton = addRenderableWidget(createButton(cx + 44, tabY, tabW, 18,
                Component.literal("Failsafe"), b -> setTab(MenuTab.FAILSAFE)));

        int contentY = panelTop + 60;

        macroButton = addGeneralButton(createButton(
                cx - bw / 2, contentY, bw, BUTTON_H,
                Component.literal(config.macroEnabled ? "Stop Macro (~)" : "Start Macro (~)"),
                b -> onToggleMacro()));

        addGeneralButton(createButton(
                cx - bw / 2, contentY + step, bw, BUTTON_H,
                Component.literal("Set Rewarp Position"),
                b -> onSetRewarp()));

        ungrabButton = addGeneralButton(createButton(
                cx - bw / 2, contentY + step * 2, bw, BUTTON_H,
                Component.literal("Auto Ungrab: " + onOff(config.autoUngrabMouse) + " [DEV]"),
                b -> onToggleUngrab()));

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

        previewButton = addHudButton(createButton(
                cx - bw / 2, contentY, bw, BUTTON_H,
                Component.literal("Inventory Preview: " + onOff(config.inventoryPreviewEnabled)),
                b -> onTogglePreview()));

        pestHighlightButton = addHudButton(createButton(
                cx - bw / 2, contentY + step, bw, BUTTON_H,
                Component.literal("Highlight Pests: " + onOff(config.highlightPests)),
                b -> onTogglePestHighlight()));

        statusHudButton = addHudButton(createButton(
                cx - bw / 2, contentY + step * 2, bw, BUTTON_H,
                Component.literal("Status HUD: " + onOff(config.showStatusHud)),
                b -> onToggleStatusHud()));

        debugHudButton = addHudButton(createButton(
                cx - bw / 2, contentY + step * 3, bw, BUTTON_H,
                Component.literal("Debug HUD: " + onOff(config.showDebugHud)),
                b -> onToggleDebugHud()));

        hudEditButton = addHudButton(createButton(
                cx - bw / 2, contentY + step * 4 + 2, bw, BUTTON_H,
                Component.literal("Edit HUD Layout"),
                b -> toggleHudEditMode()));

        evacuateRebootButton = addFailsafeButton(createButton(
                cx - bw / 2, contentY, bw, BUTTON_H,
                Component.literal("Evacuate Reboot: " + onOff(config.autoEvacuateOnServerReboot)),
                b -> onToggleEvacuateReboot()));

        webhookButton = addFailsafeButton(createButton(
                cx - bw / 2, contentY + step, bw, BUTTON_H,
                Component.literal("Discord Webhook: " + onOff(config.discordWebhookEnabled)),
                b -> onToggleWebhook()));

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

        webhookUrlButton = addFailsafeButton(createButton(
                cx - bw / 2, contentY + step * 3 + 2, bw, BUTTON_H,
                Component.literal("Webhook URL: " + (config.discordWebhookUrl.isEmpty() ? "(empty)" : "***")),
                b -> onEditWebhookUrl()));

        // create paste button (hidden by default) so it looks exactly like other buttons
        int pasteW = 75;
        int pasteH = 20;
        int pasteX = width - pasteW - 20; // aligns with drawWebhookUrlInput
        int pasteY = 12 + 32; // y2 + 32
        pasteBtn = addRenderableWidget(createButton(
                pasteX, pasteY, pasteW, pasteH,
                Component.literal("Paste"),
                b -> {
                    String clipboard = getClipboardAsString();
                    LOGGER.info("Paste button action clipboard={}", clipboard);
                    if (clipboard != null && !clipboard.isEmpty()) {
                        webhookUrlInput = clipboard.trim();
                        status = "Pasted webhook URL (" + (lastClipboardBackend == null || lastClipboardBackend.isEmpty() ? "unknown" : lastClipboardBackend) + ")";
                        statusColor = 0xFF9FE19F;
                        webhookUrlButton.setMessage(Component.literal("Webhook URL: " + (webhookUrlInput.isEmpty() ? "(empty)" : "***")));
                    } else {
                        status = "Clipboard empty";
                        statusColor = 0xFFFFAA66;
                    }
                }
        ));
        pasteBtn.visible = false;
        pasteBtn.active = false;

        closeButton = addRenderableWidget(createButton(
                cx - bw / 2, panelBottom - 28, bw, BUTTON_H,
                Component.literal("Close"),
                b -> onClose()));

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


    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private void onEditWebhookUrl() {
        editingWebhookUrl = !editingWebhookUrl;
        if (editingWebhookUrl) {
            webhookUrlInput = config.discordWebhookUrl == null ? "" : config.discordWebhookUrl;
            status = "Webhook URL: Paste with Ctrl+V, Enter to save, Esc to cancel";
            statusColor = 0xFFB7C6D8;
            if (pasteBtn != null) {
                pasteBtn.visible = true;
                pasteBtn.active = true;
            }
        } else {
            status = "Webhook URL closed without saving";
            statusColor = 0xFFFFAA66;
            webhookUrlButton.setMessage(Component.literal("Webhook URL: " + (config.discordWebhookUrl.isEmpty() ? "(empty)" : "***")));
            if (pasteBtn != null) {
                pasteBtn.visible = false;
                pasteBtn.active = false;
            }
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (editingWebhookUrl) {
            LOGGER.info("WebhookScreen keyPressed event={}", event);
            int key = event.key();
            int scancode = event.scancode();
            int mods = event.modifiers();
            if (key == 256) { // ESC key
                editingWebhookUrl = false;
                status = "Webhook URL closed without saving";
                statusColor = 0xFFFFAA66;
                return true;
            }
            if (key == 257 || key == 335) { // ENTER or KP_ENTER
                config.discordWebhookUrl = webhookUrlInput;
                editingWebhookUrl = false;
                status = "Webhook URL saved!";
                statusColor = 0xFF9FE19F;
                config.save();
                webhookUrlButton.setMessage(Component.literal("Webhook URL: " + (webhookUrlInput.isEmpty() ? "(empty)" : "***")));
                return true;
            }
            if (key == 259) { // BACKSPACE
                if (!webhookUrlInput.isEmpty()) {
                    webhookUrlInput = webhookUrlInput.substring(0, webhookUrlInput.length() - 1);
                }
                return true;
            }
            if (key == 86 && (mods & 2) != 0) { // Ctrl+V - paste
                String clipboard = getClipboardAsString();
                if (clipboard != null && !clipboard.isEmpty()) {
                    webhookUrlInput = clipboard.trim();
                    status = "Pasted webhook URL (" + (lastClipboardBackend == null || lastClipboardBackend.isEmpty() ? "unknown" : lastClipboardBackend) + ")";
                    statusColor = 0xFF9FE19F;
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (editingWebhookUrl) {
            LOGGER.info("WebhookScreen charTyped event={}", event);
            int codePoint = event.codepoint();
            if (codePoint >= 32 && codePoint <= 126 && webhookUrlInput.length() < 200) {
                webhookUrlInput += (char) codePoint;
                return true;
            }
            // consume any other character so it doesn't leak
            return true;
        }
        return super.charTyped(event);
    }

    private String getClipboardAsString() {
        // Try AWT clipboard first
        try {
            java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
            java.awt.datatransfer.Clipboard clipboard = toolkit.getSystemClipboard();
            java.awt.datatransfer.DataFlavor flavor = java.awt.datatransfer.DataFlavor.stringFlavor;
            if (clipboard.isDataFlavorAvailable(flavor)) {
                Object data = clipboard.getData(flavor);
                if (data instanceof String) {
                    LOGGER.info("Clipboard read via AWT");
                    lastClipboardBackend = "awt";
                    return (String) data;
                }
            }
        } catch (Exception ignored) {
            LOGGER.info("AWT clipboard read failed: {}", ignored.toString());
        }

        // On Linux, try wl-paste, xclip, xsel as fallbacks (Wayland/X11)
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                String session = System.getenv("XDG_SESSION_TYPE");
                String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
                // If we're on Wayland and have a WAYLAND_DISPLAY, prefer wl-paste
                if (session != null && session.equalsIgnoreCase("wayland") && waylandDisplay != null && !waylandDisplay.isEmpty()) {
                    String out = tryCommand(new String[]{"wl-paste", "-n"});
                    if (out != null && !out.isEmpty()) {
                        lastClipboardBackend = "wl-paste";
                        return out;
                    }
                }

                // Otherwise prefer X11 helpers (xclip, xsel) on X11 or when Wayland isn't available
                String out = tryCommand(new String[]{"xclip", "-o", "-selection", "clipboard"});
                if (out != null && !out.isEmpty()) { lastClipboardBackend = "xclip"; return out; }
                out = tryCommand(new String[]{"xclip", "-o", "-selection", "primary"});
                if (out != null && !out.isEmpty()) { lastClipboardBackend = "xclip-primary"; return out; }

                out = tryCommand(new String[]{"xsel", "-b", "-o"});
                if (out != null && !out.isEmpty()) { lastClipboardBackend = "xsel"; return out; }
                out = tryCommand(new String[]{"xsel", "-p", "-o"});
                if (out != null && !out.isEmpty()) { lastClipboardBackend = "xsel-primary"; return out; }
            }
        } catch (Exception ignored) {
            LOGGER.info("Clipboard fallback check failed: {}", ignored.toString());
        }

        return null;
    }

    private String tryCommand(String[] cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.InputStream is = p.getInputStream(); java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (sb.length() > 8192) break;
                }
                String out = sb.toString().trim();
                p.destroy();
                if (out != null && !out.isEmpty()) {
                    LOGGER.info("Clipboard read via fallback: {}", cmd[0]);
                    return out;
                }
                return null;
            }
        } catch (Throwable t) {
            LOGGER.info("Clipboard command {} failed: {}", Arrays.toString(cmd), t.toString());
            return null;
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        // Let button widgets handle clicks (paste button is an actual Button)
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

    // helper used during early iterations; no longer needed (hover handled in StyledButton)

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

        // base panel and shadow
        gg.fill(l + 2, t + 2, r + 2, b + 2, 0x55000000);
        gg.fill(l, t, r, b, 0xFF20232A);  // dark grey background
        // header bar with accent color
        gg.fill(l, t, r, t + 28, 0xFF2EC4B6);
        // top/bottom border lines
        gg.fill(l, t, r, t + 2, 0xFF1B252D);
        gg.fill(l, b - 2, r, b, 0xFF1B252D);

        // small horizontal separators
        gg.fill(l + 16, t + 53, r - 16, t + 54, 0x553F5368);
        gg.fill(l + 16, b - 78, r - 16, b - 77, 0x553F5368);

        gg.drawCenteredString(font, "ReimoHelper", cx, t + 10, 0xFF20232A);
        gg.drawCenteredString(font, "Menu: Right Shift | Toggle: ~", cx, t + 20, 0xFFB7C6D8);

        if (activeTab == MenuTab.FAILSAFE && !hudEditMode) {
            gg.drawCenteredString(font, "Failsafe options", cx, t + 95, 0xFFB7C6D8);
        }

        if (hudEditMode) {
            drawHudEditor(gg);
        }

        if (editingWebhookUrl) {
            drawWebhookUrlInput(gg, cx, 0);
        }

        // note: we render the active-tab border *after* drawing widgets below so
        // it remains visible on top of our custom button backgrounds.
        super.render(gg, mx, my, pt);

        // highlight active tab with a thin accent outline (no fill) so the
        // button label is never obscured.
        Button activeBtn = switch(activeTab) {
            case GENERAL -> tabGeneralButton;
            case HUD -> tabHudButton;
            case FAILSAFE -> tabFailsafeButton;
        };
        if (activeBtn != null) {
            int ax = activeBtn.getX();
            int ay = activeBtn.getY();
            int aw = activeBtn.getWidth();
            int ah = activeBtn.getHeight();
            int col = 0xFF2EC4B6;
            // top edge
            gg.fill(ax - 2, ay - 2, ax + aw + 2, ay - 1, col);
            // bottom edge
            gg.fill(ax - 2, ay + ah + 1, ax + aw + 2, ay + ah + 2, col);
            // left edge
            gg.fill(ax - 2, ay - 1, ax - 1, ay + ah + 1, col);
            // right edge
            gg.fill(ax + aw + 1, ay - 1, ax + aw + 2, ay + ah + 1, col);
        }


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

    private double pasteX, pasteY, pasteW, pasteH;
    private int lastPasteRenderX, lastPasteRenderY, lastPasteRenderW, lastPasteRenderH;

    private void drawWebhookUrlInput(GuiGraphics gg, int cx, int y) {
        int w = 300;
        int h = 100;
        String displayUrl = webhookUrlInput.isEmpty() ? "paste your webhook URL here" : webhookUrlInput;
        if (editingWebhookUrl) {
            // make the whole dialog wider to accommodate the URL text, up to screen size
            int needed = font.width(displayUrl) + 120; // include padding and space for paste button
            w = Math.min(width - 20, Math.max(w, needed));
        }
        int x = width - w - 12; // Top-right corner
        int y2 = 12; // Position near top

        // Dark background overlay
        gg.fill(x - 3, y2 - 3, x + w + 3, y2 + h + 3, 0xDD000000);
        gg.fill(x, y2, x + w, y2 + h, 0xD8121821);
        gg.fill(x, y2, x + w, y2 + 2, 0xFF2EC4B6);
        gg.fill(x, y2 + h - 2, x + w, y2 + h, 0xFF2EC4B6);

        gg.drawString(font, "Discord Webhook URL", x + 10, y2 + 10, 0xFFFFFFFF, false);
        
        // Draw text input box
        int boxY = y2 + 32;
        int boxH = 22;
        int inputW = w - 90;  // Leave room for paste button
        gg.fill(x + 8, boxY, x + 8 + inputW, boxY + boxH, 0xFF1A2A38);
        gg.fill(x + 8, boxY, x + 8 + inputW, boxY + 1, 0xFF5CA8FF);
        gg.fill(x + 8, boxY + boxH - 1, x + 8 + inputW, boxY + boxH, 0xFF5CA8FF);
        
        // Display the URL or placeholder
        int textColor = webhookUrlInput.isEmpty() ? 0xFF888888 : 0xFFFFFFFF;

        // draw text; when editing we expanded the dialog so text should fit entirely
        gg.drawString(font, displayUrl, x + 12, boxY + 7, textColor, false);
        if (editingWebhookUrl && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = x + 12 + font.width(displayUrl);
            if (cursorX < x + 8 + inputW - 12) {
                gg.fill(cursorX, boxY + 5, cursorX + 1, boxY + boxH - 5, 0xFFFFFFFF);
            }
        }

        // when not editing we still show a truncated tail if URL too long to fit
        if (!editingWebhookUrl) {
            String truncatedUrl;
            int maxVisible = 60;
            if (displayUrl.length() > maxVisible) {
                truncatedUrl = "..." + displayUrl.substring(displayUrl.length() - (maxVisible - 3));
                gg.drawString(font, truncatedUrl, x + 12, boxY + 7, textColor, false);
            }
        }

        // paste button area - right side of text input
        pasteW = 75;
        pasteH = 20;
        pasteX = x + w - pasteW - 8;
        pasteY = boxY;
        // Store for reliable click detection
        lastPasteRenderX = (int)pasteX;
        lastPasteRenderY = (int)pasteY;
        lastPasteRenderW = (int)pasteW;
        lastPasteRenderH = (int)pasteH;
        gg.fill((int)pasteX - 1, (int)pasteY - 1, (int)pasteX + (int)pasteW + 1, (int)pasteY + (int)pasteH + 1, 0xFF2EC4B6);
        gg.fill((int)pasteX, (int)pasteY, (int)pasteX + (int)pasteW, (int)pasteY + (int)pasteH, 0xFF3A3A3A);
        gg.drawCenteredString(font, "Paste", (int)(pasteX + pasteW / 2), (int)(pasteY + 5), 0xFFFFFFFF);

        gg.drawString(font, "Enter: save | Esc: cancel | Click Paste or Ctrl+V | Backspace", x + 8, y2 + 62, 0xFFB7C6D8, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
