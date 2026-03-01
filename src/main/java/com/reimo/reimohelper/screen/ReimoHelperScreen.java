package com.reimo.reimohelper.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.feature.MacroStatsStore;
import com.reimo.reimohelper.feature.RewarpManager;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.hud.InventoryPreviewHud;
import com.reimo.reimohelper.hud.StatusHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReimoHelperScreen extends Screen {
    private enum MenuTab {
        GENERAL,
        MACRO,
        REWARP,
        HUD,
        FAILSAFE,
        DISCORD
    }

    private enum UiMode {
        NORMAL,
        HUD_EDIT,
        WEBHOOK_EDIT,
        COLOR_EDIT,
        KEY_CAPTURE,
        MACRO_TYPE_PICK
    }

    private enum ColorField {
        ACCENT,
        PANEL,
        CARD,
        BACKGROUND,
        PRIMARY,
        SECONDARY,
        WARNING
    }

    private enum DragTarget {
        NONE,
        INVENTORY,
        STATUS
    }

    private static final class UiLayout {
        int panelX;
        int panelY;
        int panelW;
        int panelH;
        int sidebarX;
        int sidebarW;
        int contentX;
        int contentW;
        int contentY;
        int contentPad;
        int headerY;
        int footerStatusY;
        int footerHintY;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");

    private static final int COLOR_TEXT_PRIMARY = 0xFFEAEAEA;
    private static final int COLOR_TEXT_SECONDARY = 0xFF9E9E9E;
    private static final int COLOR_WARNING = 0xFFFF4444;

    // Imported from tools/menu-builder export
    private static final int DESIGN_MENU_W = 620;
    private static final int DESIGN_MENU_H = 360;
    private static final int DESIGN_SIDEBAR_W = 200;
    private static final int DESIGN_CONTENT_PAD = 18;
    private static final int COLOR_TAUNAHI_ACCENT = 0xFFC83552; // #c83552
    private static final int COLOR_SURFACE = 0xFF101421; // #101421
    private static final int COLOR_SURFACE_SOFT = 0xFF151A2C; // #151a2c
    private static final int COLOR_SURFACE_WIDGET = 0xFF1B2135; // #1b2135
    private static final String TEXT_BRAND = "ReimoHelper";
    private static final String TEXT_META_TEMPLATE = "Menu: Right Shift | Toggle: %s";
    private static final String TEXT_HINT_GENERAL = "Open HUD tab for layout editor";

    private static final int BUTTON_W = 218;
    private static final int BUTTON_H = 24;
    private static final int ROW_GAP = 7;

    private static final int[] MACRO_TOGGLE_KEYS = new int[] {
        InputConstants.KEY_GRAVE,
        InputConstants.KEY_F6,
        InputConstants.KEY_F7,
        InputConstants.KEY_F8,
        InputConstants.KEY_F9,
        InputConstants.KEY_F10,
        InputConstants.KEY_F11,
        InputConstants.KEY_F12
    };

    private final ReimoHelperConfig config = ReimoHelperConfig.getInstance();

    private final List<AbstractWidget> generalButtons = new ArrayList<>();
    private final List<AbstractWidget> macroButtons = new ArrayList<>();
    private final List<AbstractWidget> rewarpButtons = new ArrayList<>();
    private final List<AbstractWidget> hudButtons = new ArrayList<>();
    private final List<AbstractWidget> failsafeButtons = new ArrayList<>();
    private final List<AbstractWidget> discordButtons = new ArrayList<>();
    private final List<AbstractWidget> macroTypeButtons = new ArrayList<>();

    private MenuTab activeTab = MenuTab.GENERAL;
    private UiMode uiMode = UiMode.NORMAL;

    private float openProgress;

    private final UiLayout layout = new UiLayout();

    private int webhookModalX;
    private int webhookModalY;
    private int webhookModalW;
    private int webhookModalH;

    private int colorModalX;
    private int colorModalY;
    private int colorModalW;
    private int colorModalH;

    private Button macroKeySelectButton;
    private Button macroTypeButton;
    private Button topCloseButton;

    private Toggle ungrabToggle;
    private Toggle previewToggle;
    private Toggle evacuateToggle;
    private Toggle pestToggle;
    private Toggle statusHudToggle;
    private Toggle debugHudToggle;
    private Toggle webhookToggle;
    private Toggle failsafesEnabledToggle;

    private Button hudEditButton;
    private ValueSlider soundSlider;
    private ValueSlider failsafeVolumeSlider;
    private ValueSlider webhookIntervalSlider;
    private Button webhookUrlButton;
    private Button setRewarpButton;
    private StyledButton rewarpCommandButton;
    private StyledButton rewarpLocationButton;

    private Button webhookSaveBtn;
    private Button webhookCancelBtn;
    private Button pasteBtn;

    private Button colorSaveBtn;
    private Button colorCancelBtn;

    private SidebarItem tabGeneralButton;
    private SidebarItem tabMacroButton;
    private SidebarItem tabRewarpButton;
    private SidebarItem tabHudButton;
    private SidebarItem tabFailsafeButton;
    private SidebarItem tabDiscordButton;
    private Button closeButton;
    private StyledButton statsPlayerButton;
    private StyledButton statsTotalTimeButton;
    private StyledButton statsSessionButton;
    private StyledButton statsRuntimeButton;

    private String webhookUrlInput = "";
    private String lastClipboardBackend = "";

    private ColorField currentColorField = ColorField.ACCENT;
    private String accentColorInput;
    private String panelColorInput;
    private String cardColorInput;
    private String backgroundColorInput;
    private String textPrimaryColorInput;
    private String textSecondaryColorInput;
    private String warningColorInput;

    private DragTarget dragTarget = DragTarget.NONE;
    private int dragOffsetX;
    private int dragOffsetY;

    private String status = "Ready";
    private int statusColor = COLOR_TEXT_SECONDARY;
    private long keyCaptureStartedAt;
    private boolean macroTypeDropdownOpen;
    private int macroLabelX;
    private int macroLabelYMacroKey;
    private int macroLabelYMacroType;
    private int macroLabelYUngrab;
    private int rewarpLabelX;
    private int rewarpLabelYCommand;
    private int rewarpLabelYLocation;
    private int failsafeLabelX;
    private int failsafeLabelYVolume;
    private int failsafeLabelYWebhookInterval;
    private int failsafeLabelYWebhookUrl;

    private class StyledButton extends Button {
        private String label;

        protected StyledButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
            super(x, y, w, h, Component.empty(), onPress, (button) -> Component.empty());
            this.label = msg.getString();
        }

        @Override
        public void setMessage(Component msg) {
            this.label = msg.getString();
            super.setMessage(Component.empty());
        }

        @Override
        protected void renderContents(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
            if (this.active) {
                int base = this.isMouseOver(mouseX, mouseY) ? 0xFF23293E : 0xFF1A2031;
                ReimoHelperScreen.this.fillRoundedRect(gg, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 10, base);
                drawMenuCenteredString(gg, label, this.getX() + this.getWidth() / 2,
                    this.getY() + (this.getHeight() - 8) / 2, 0xFFE6E9F2);
            } else {
                // Inactive styled buttons are label-only cells for row layouts.
                drawMenuString(gg, label, this.getX() + 8, this.getY() + (this.getHeight() - 8) / 2, 0xFFD7DAE3, false);
            }
        }
    }

    private class SidebarItem extends Button {
        private final MenuTab tab;
        private String label;

        protected SidebarItem(int x, int y, int w, int h, Component msg, MenuTab tab) {
            super(x, y, w, h, Component.empty(), b -> {}, (button) -> Component.empty());
            this.tab = tab;
            this.label = msg.getString();
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
                setTab(tab);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        protected void renderContents(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
            boolean active = activeTab == tab;
            if (active) {
                gg.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x221A2031);
                gg.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.getHeight(), COLOR_TAUNAHI_ACCENT);
            }
            int txtCol = active ? COLOR_TAUNAHI_ACCENT : 0xFFA8AEBE;
            drawMenuString(gg, label, this.getX() + 8, this.getY() + (this.getHeight() - 8) / 2, txtCol, false);
        }
    }

    private class Toggle extends Button {
        private boolean on;
        private final String label;
        private final java.util.function.Consumer<Boolean> callback;

        protected Toggle(int x, int y, int w, int h, String label, boolean initial,
                         java.util.function.Consumer<Boolean> callback) {
            super(x, y, w, h, Component.empty(), b -> {}, (button) -> Component.empty());
            this.on = initial;
            this.label = label;
            this.callback = callback;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            int pillX = this.getX() + this.getWidth() - 52;
            int pillY = this.getY() + 2;
            int pillW = 44;
            int pillH = this.getHeight() - 4;
            boolean inSwitch = event.x() >= pillX && event.x() <= pillX + pillW
                && event.y() >= pillY && event.y() <= pillY + pillH;
            if (event.button() == 0 && inSwitch) {
                on = !on;
                callback.accept(on);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        protected void renderContents(GuiGraphics gg, int mx, int my, float pt) {
            drawMenuString(gg, label, this.getX() + 4, this.getY() + (this.getHeight() - 8) / 2, 0xFFD7DAE3, false);

            int pillX = this.getX() + this.getWidth() - 52;
            int pillW = 44;
            int pillH = this.getHeight() - 8;
            int pillY = this.getY() + (this.getHeight() - pillH) / 2;
            int onColor = scaleRgb(COLOR_TAUNAHI_ACCENT, 0.60f);
            int offColor = 0xFF252B3E;
            gg.fill(pillX, pillY, pillX + pillW, pillY + pillH, on ? onColor : offColor);

            int knob = pillH - 4;
            int knobX = pillX + 4 + (on ? (pillW - knob - 8) : 0);
            int knobY = pillY + 2;
            gg.fill(knobX, knobY, knobX + knob, knobY + knob, on ? 0xFFF3E7EB : 0xFFC3C8D8);
        }
    }

    private abstract class ValueSlider extends AbstractSliderButton {
        private final Component label;

        protected ValueSlider(int x, int y, int w, int h, Component label, double initial) {
            super(x, y, w, h, label, initial);
            this.label = label;
        }

        @Override
        protected void updateMessage() {
            setMessage(label);
        }

        public void drawValue(GuiGraphics gg) {
            String txt = getValueString();
            int textW = menuWidth(txt);
            int tx = this.getX() + this.getWidth() - textW - 4;
            int ty = this.getY() + (this.getHeight() - 8) / 2;
            drawMenuString(gg, txt, tx, ty, COLOR_TAUNAHI_ACCENT, false);
        }

        protected abstract String getValueString();
    }

    public ReimoHelperScreen() {
        super(Component.literal("ReimoHelper"));
        openProgress = 0f;
    }

    @Override
    protected void init() {
        clearWidgets();
        generalButtons.clear();
        macroButtons.clear();
        rewarpButtons.clear();
        hudButtons.clear();
        failsafeButtons.clear();
        discordButtons.clear();
        macroTypeButtons.clear();

        computeLayout();
        buildWidgets();

        macroTypeDropdownOpen = false;
        setUiMode(UiMode.NORMAL);
        applyTabVisibility();
        updateTabLabels();
    }

    private void computeLayout() {
        layout.panelW = Math.max(360, Math.min(DESIGN_MENU_W, width - 48));
        layout.panelH = Math.max(260, Math.min(DESIGN_MENU_H, height - 48));
        layout.panelX = (width - layout.panelW) / 2;
        layout.panelY = (height - layout.panelH) / 2;

        layout.sidebarW = Math.max(140, Math.min(DESIGN_SIDEBAR_W, layout.panelW - 320));
        layout.sidebarX = layout.panelX;

        layout.contentX = layout.panelX + layout.sidebarW + 28;
        layout.contentW = (layout.panelX + layout.panelW) - layout.contentX - 12;
        layout.contentY = layout.panelY + 88;
        layout.contentPad = DESIGN_CONTENT_PAD;

        layout.headerY = layout.panelY + 18;
        layout.footerStatusY = layout.panelY + layout.panelH - 52;
        layout.footerHintY = layout.panelY + layout.panelH - 38;

        webhookModalW = Math.min(520, width - 24);
        webhookModalH = 112;
        webhookModalX = width - webhookModalW - 12;
        webhookModalY = 12;

        colorModalW = 360;
        colorModalH = 194;
        colorModalX = width - colorModalW - 12;
        colorModalY = 12;
    }

    private void buildWidgets() {
        int step = BUTTON_H + ROW_GAP;
        int panelRight = layout.panelX + layout.panelW;
        int panelBottom = layout.panelY + layout.panelH;
        int controlPad = layout.contentPad;

        int controlX = layout.contentX + controlPad;
        int controlW = Math.max(120, layout.contentW - controlPad * 2);
        int controlBW = controlW;

        topCloseButton = addRenderableWidget(createButton(
            panelRight - 24, layout.panelY + 6, 16, 16,
            Component.literal("X"), b -> onClose()
        ));

        int sidebarY = layout.panelY + 72;
        int tabW = layout.sidebarW - 16;
        tabGeneralButton = addRenderableWidget(new SidebarItem(layout.sidebarX + 8, sidebarY, tabW, BUTTON_H,
            Component.literal("General"), MenuTab.GENERAL));
        tabMacroButton = addRenderableWidget(new SidebarItem(layout.sidebarX + 8, sidebarY + step, tabW, BUTTON_H,
            Component.literal("Macro"), MenuTab.MACRO));
        tabRewarpButton = addRenderableWidget(new SidebarItem(layout.sidebarX + 8, sidebarY + (step * 2), tabW, BUTTON_H,
            Component.literal("Rewarp"), MenuTab.REWARP));
        tabHudButton = addRenderableWidget(new SidebarItem(layout.sidebarX + 8, sidebarY + (step * 3), tabW, BUTTON_H,
            Component.literal("HUD"), MenuTab.HUD));
        tabFailsafeButton = addRenderableWidget(new SidebarItem(layout.sidebarX + 8, sidebarY + (step * 4), tabW, BUTTON_H,
            Component.literal("Failsafe"), MenuTab.FAILSAFE));
        tabDiscordButton = addRenderableWidget(new SidebarItem(layout.sidebarX + 8, sidebarY + (step * 5), tabW, BUTTON_H,
            Component.literal("Discord"), MenuTab.DISCORD));

        int curY = layout.contentY;
        int splitGap = 8;
        int labelW = Math.max(140, Math.min(230, controlBW / 3));
        int rightW = controlBW - labelW - splitGap;
        statsPlayerButton = addGeneralButton(createReadOnlyCell(
            controlX, curY, controlBW, BUTTON_H, "Player: -"
        ));
        curY += step + 8;
        statsTotalTimeButton = addGeneralButton(createReadOnlyCell(
            controlX, curY, controlBW, BUTTON_H, "Total macro time: 0h 0m"
        ));
        curY += step + 8;
        statsSessionButton = addGeneralButton(createReadOnlyCell(
            controlX, curY, controlBW, BUTTON_H, "Macro sessions: 0"
        ));
        curY += step + 8;
        statsRuntimeButton = addGeneralButton(createReadOnlyCell(
            controlX, curY, controlBW, BUTTON_H, "Current runtime: 0m 0s"
        ));

        int macroY = layout.contentY;
        macroLabelX = controlX + 8;
        macroLabelYMacroKey = macroY + (BUTTON_H - 8) / 2;
        macroKeySelectButton = addMacroButton(createButton(
            controlX + labelW + splitGap, macroY, rightW, BUTTON_H,
            Component.literal("Macro Key: " + keyName(config.macroToggleKey)),
            b -> onMacroKeyButton()
        ));
        macroY += step + 8;
        macroLabelYMacroType = macroY + (BUTTON_H - 8) / 2;
        macroTypeButton = addMacroButton(createButton(
            controlX + labelW + splitGap, macroY, rightW, BUTTON_H,
            Component.literal("Macro type: " + config.macroType.getDisplayName()),
            b -> onMacroTypeButton()
        ));
        macroY += step + 8;
        macroLabelYUngrab = macroY + (BUTTON_H - 8) / 2;
        ungrabToggle = addMacroButton(new Toggle(
            controlX, macroY, controlBW, BUTTON_H,
            "Auto Ungrab", config.autoUngrabMouse,
            v -> {
                config.autoUngrabMouse = v;
                updateStatus("Auto ungrab " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
            }
        ));

        int rewarpY = layout.contentY;
        setRewarpButton = addRewarpButton(createButton(
            controlX, rewarpY, controlBW, BUTTON_H,
            Component.literal("Set Rewarp Position"),
            b -> onSetRewarp()
        ));
        rewarpY += step + 8;
        rewarpLabelX = controlX + 8;
        rewarpLabelYCommand = rewarpY + (BUTTON_H - 8) / 2;
        rewarpCommandButton = addRewarpButton(createReadOnlyCell(
            controlX + labelW + splitGap, rewarpY, rightW, BUTTON_H, config.rewarpCommand
        ));
        rewarpY += step + 8;
        rewarpLabelYLocation = rewarpY + (BUTTON_H - 8) / 2;
        rewarpLocationButton = addRewarpButton(createReadOnlyCell(
            controlX + labelW + splitGap, rewarpY, rightW, BUTTON_H, "(not set)"
        ));

        int hudX = layout.contentX + controlPad;
        int hudW = layout.contentW - controlPad * 2;
        int hudRow = 0;

        previewToggle = addHudButton(new Toggle(
            hudX, layout.contentY + hudRow * (step + 6), hudW, BUTTON_H,
            "Inventory Preview", config.inventoryPreviewEnabled,
            v -> {
                config.inventoryPreviewEnabled = v;
                updateStatus("Inventory preview " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
            }
        ));
        hudRow++;

        pestToggle = addHudButton(new Toggle(
            hudX, layout.contentY + hudRow * (step + 6), hudW, BUTTON_H,
            "Highlight Pests", config.highlightPests,
            v -> {
                config.highlightPests = v;
                updateStatus("Pest highlight " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
            }
        ));
        hudRow++;

        statusHudToggle = addHudButton(new Toggle(
            hudX, layout.contentY + hudRow * (step + 6), hudW, BUTTON_H,
            "Status HUD", config.showStatusHud,
            v -> {
                config.showStatusHud = v;
                updateStatus("Status HUD " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
            }
        ));
        hudRow++;

        debugHudToggle = addHudButton(new Toggle(
            hudX, layout.contentY + hudRow * (step + 6), hudW, BUTTON_H,
            "Debug HUD", config.showDebugHud,
            v -> {
                config.showDebugHud = v;
                updateStatus("Debug HUD " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
            }
        ));
        hudRow++;

        hudEditButton = addHudButton(createButton(
            hudX, layout.contentY + hudRow * (step + 10), hudW, BUTTON_H,
            Component.literal("Edit HUD Layout"),
            b -> onHudEdit()
        ));

        int failsafeX = layout.contentX + controlPad;
        int failsafeW = layout.contentW - controlPad * 2;
        int failsafeGap = 8;
        int failsafeLabelW = Math.max(132, Math.min(190, failsafeW / 3));
        int failsafeRightW = failsafeW - failsafeLabelW - failsafeGap;
        failsafeLabelX = failsafeX + 8;
        int failsafeStep = step + 8;

        failsafesEnabledToggle = addFailsafeButton(new Toggle(
            failsafeX, layout.contentY, failsafeW, BUTTON_H,
            "Failsafes Enabled", config.failsafesEnabled,
            v -> {
                config.failsafesEnabled = v;
                updateStatus("Failsafes " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
            }
        ));

        evacuateToggle = addFailsafeButton(new Toggle(
            failsafeX, layout.contentY + failsafeStep, failsafeW, BUTTON_H,
            "Evacuate on Reboot", config.autoEvacuateOnServerReboot,
            v -> {
                config.autoEvacuateOnServerReboot = v;
                updateStatus("Evacuate reboot " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
            }
        ));
        int failsafeVolumeY = layout.contentY + failsafeStep * 2;
        failsafeLabelYVolume = failsafeVolumeY + (BUTTON_H - 8) / 2;
        failsafeVolumeSlider = addFailsafeButton(new ValueSlider(
            failsafeX + failsafeLabelW + failsafeGap, failsafeVolumeY, failsafeRightW, BUTTON_H,
            Component.literal(""),
            Math.max(0.0, Math.min(1.0, config.failsafeSoundPercent / 100.0))
        ) {
            @Override
            protected String getValueString() {
                return ((int) Math.round(this.value * 100.0)) + "%";
            }

            @Override
            protected void applyValue() {
                config.failsafeSoundPercent = (int) Math.round(this.value * 100.0);
                updateStatus("Failsafe sound set to " + config.failsafeSoundPercent + "%", config.uiTextPrimaryColor);
                config.save();
            }
        });

        webhookToggle = addDiscordButton(new Toggle(
            failsafeX, layout.contentY, failsafeW, BUTTON_H,
            "Enable Webhook", config.discordWebhookEnabled,
            v -> {
                config.discordWebhookEnabled = v;
                updateStatus("Discord webhook " + (v ? "enabled" : "disabled"), config.uiTextPrimaryColor);
                config.save();
                applyTabVisibility();
            }
        ));

        int webhookIntervalY = layout.contentY + failsafeStep;
        failsafeLabelYWebhookInterval = webhookIntervalY + (BUTTON_H - 8) / 2;
        webhookIntervalSlider = addDiscordButton(new ValueSlider(
            failsafeX + failsafeLabelW + failsafeGap, webhookIntervalY, failsafeRightW, BUTTON_H,
            Component.literal(""),
            Math.max(0.0, Math.min(1.0, (config.discordWebhookIntervalSeconds - 10) / 590.0))
        ) {
            @Override
            protected String getValueString() {
                int interval = 10 + (int) Math.round(this.value * 590.0);
                return interval + "s";
            }

            @Override
            protected void applyValue() {
                config.discordWebhookIntervalSeconds = 10 + (int) Math.round(this.value * 590.0);
                updateStatus("Webhook interval set to " + config.discordWebhookIntervalSeconds + "s", config.uiTextPrimaryColor);
                config.save();
            }
        });

        int webhookUrlY = layout.contentY + failsafeStep * 2;
        failsafeLabelYWebhookUrl = webhookUrlY + (BUTTON_H - 8) / 2;
        webhookUrlButton = addDiscordButton(createButton(
            failsafeX + failsafeLabelW + failsafeGap, webhookUrlY, failsafeRightW, BUTTON_H,
            Component.literal(config.discordWebhookUrl.isEmpty() ? "(empty)" : "Change URL"),
            b -> onEditWebhookUrl()
        ));

        int typeW = rightW;
        int typeX = controlX + labelW + splitGap;
        int typeY = macroTypeButton.getY() + BUTTON_H + 6;
        int typeStep = BUTTON_H + 4;
        for (ReimoHelperConfig.MacroType value : ReimoHelperConfig.MacroType.values()) {
            StyledButton option = addRenderableWidget(createButton(
                typeX, typeY, typeW, BUTTON_H,
                Component.literal(value.getDisplayName()),
                b -> onPickMacroType(value)
            ));
            option.visible = false;
            option.active = false;
            macroTypeButtons.add(option);
            typeY += typeStep;
        }

        int inputW = webhookModalW - 98;
        webhookSaveBtn = addRenderableWidget(createButton(
            webhookModalX + webhookModalW - 136, webhookModalY + webhookModalH - 28, 60, 20,
            Component.literal("Save"), b -> onWebhookSave()
        ));
        webhookCancelBtn = addRenderableWidget(createButton(
            webhookModalX + webhookModalW - 68, webhookModalY + webhookModalH - 28, 60, 20,
            Component.literal("Cancel"), b -> onWebhookCancel()
        ));
        pasteBtn = addRenderableWidget(createButton(
            webhookModalX + 8 + inputW + 8, webhookModalY + 32, 75, 20,
            Component.literal("Paste"), b -> onPasteWebhook()
        ));

        colorSaveBtn = addRenderableWidget(createButton(
            colorModalX + 10, colorModalY + colorModalH - 44, 60, 20,
            Component.literal("Save"), b -> onColorSave()
        ));
        colorCancelBtn = addRenderableWidget(createButton(
            colorModalX + colorModalW - 70, colorModalY + colorModalH - 44, 60, 20,
            Component.literal("Cancel"), b -> onColorCancel()
        ));

        int closeW = Math.min(220, controlBW);
        closeButton = addRenderableWidget(createButton(
            layout.contentX + (layout.contentW - closeW) / 2,
            panelBottom - 30,
            closeW,
            BUTTON_H,
            Component.literal("Close"),
            b -> onClose()
        ));
    }

    private StyledButton createButton(int x, int y, int w, int h, Component text, Button.OnPress onPress) {
        return new StyledButton(x, y, w, h, text, onPress);
    }

    private StyledButton createReadOnlyCell(int x, int y, int w, int h, String text) {
        StyledButton button = createButton(x, y, w, h, Component.literal(text), b -> {});
        button.active = false;
        return button;
    }

    private <T extends AbstractWidget> T addGeneralButton(T button) {
        generalButtons.add(addRenderableWidget(button));
        return button;
    }

    private <T extends AbstractWidget> T addHudButton(T button) {
        hudButtons.add(addRenderableWidget(button));
        return button;
    }

    private <T extends AbstractWidget> T addMacroButton(T button) {
        macroButtons.add(addRenderableWidget(button));
        return button;
    }

    private <T extends AbstractWidget> T addRewarpButton(T button) {
        rewarpButtons.add(addRenderableWidget(button));
        return button;
    }

    private <T extends AbstractWidget> T addFailsafeButton(T button) {
        failsafeButtons.add(addRenderableWidget(button));
        return button;
    }

    private <T extends AbstractWidget> T addDiscordButton(T button) {
        discordButtons.add(addRenderableWidget(button));
        return button;
    }

    private void setTab(MenuTab tab) {
        if (activeTab == tab) {
            return;
        }
        macroTypeDropdownOpen = false;
        if (uiMode != UiMode.NORMAL) {
            setUiMode(UiMode.NORMAL);
        }
        activeTab = tab;
        dragTarget = DragTarget.NONE;
        applyTabVisibility();
        updateTabLabels();
    }

    private void setUiMode(UiMode newMode) {
        boolean modeChanged = uiMode != newMode;

        if (modeChanged && uiMode == UiMode.HUD_EDIT) {
            dragTarget = DragTarget.NONE;
            config.save();
            updateStatus("HUD layout saved", config.uiTextPrimaryColor);
        }

        uiMode = newMode;
        if (uiMode != UiMode.NORMAL) {
            macroTypeDropdownOpen = false;
        }

        if (uiMode == UiMode.NORMAL) {
            if (hudEditButton != null) {
                hudEditButton.setMessage(Component.literal("Edit HUD Layout"));
            }
        } else if (modeChanged && uiMode == UiMode.HUD_EDIT) {
            if (hudEditButton != null) {
                hudEditButton.setMessage(Component.literal("Finish HUD Edit"));
            }
            updateStatus("Drag HUD boxes. Scroll over box to resize.", config.uiTextSecondaryColor);
        } else if (modeChanged && uiMode == UiMode.MACRO_TYPE_PICK) {
            updateStatus("Pick a macro type", config.uiTextSecondaryColor);
        }

        syncModalWidgetVisibility();

        applyTabVisibility();
    }

    private void syncModalWidgetVisibility() {
        boolean webhookEdit = uiMode == UiMode.WEBHOOK_EDIT;
        boolean colorEdit = uiMode == UiMode.COLOR_EDIT;
        boolean modalOpen = webhookEdit || colorEdit || uiMode == UiMode.KEY_CAPTURE;

        if (webhookSaveBtn != null) {
            webhookSaveBtn.visible = webhookEdit;
            webhookSaveBtn.active = webhookEdit;
        }
        if (webhookCancelBtn != null) {
            webhookCancelBtn.visible = webhookEdit;
            webhookCancelBtn.active = webhookEdit;
        }
        if (pasteBtn != null) {
            pasteBtn.visible = webhookEdit;
            pasteBtn.active = webhookEdit;
        }

        if (colorSaveBtn != null) {
            colorSaveBtn.visible = colorEdit;
            colorSaveBtn.active = colorEdit;
        }
        if (colorCancelBtn != null) {
            colorCancelBtn.visible = colorEdit;
            colorCancelBtn.active = colorEdit;
        }

        if (topCloseButton != null) {
            topCloseButton.visible = !modalOpen;
            topCloseButton.active = !modalOpen;
        }
        if (closeButton != null) {
            closeButton.visible = !modalOpen;
            closeButton.active = !modalOpen;
        }
        if (tabGeneralButton != null) {
            tabGeneralButton.visible = !modalOpen;
        }
        if (tabMacroButton != null) {
            tabMacroButton.visible = !modalOpen;
        }
        if (tabRewarpButton != null) {
            tabRewarpButton.visible = !modalOpen;
        }
        if (tabHudButton != null) {
            tabHudButton.visible = !modalOpen;
        }
        if (tabFailsafeButton != null) {
            tabFailsafeButton.visible = !modalOpen;
        }
        if (tabDiscordButton != null) {
            tabDiscordButton.visible = !modalOpen;
        }

        for (AbstractWidget button : macroTypeButtons) {
            button.visible = false;
            button.active = false;
        }
    }

    private void applyTabVisibility() {
        boolean normal = uiMode == UiMode.NORMAL;
        boolean hudEdit = uiMode == UiMode.HUD_EDIT;
        boolean macroTypePick = normal && activeTab == MenuTab.MACRO && macroTypeDropdownOpen;

        boolean general = normal && activeTab == MenuTab.GENERAL;
        boolean macro = normal && activeTab == MenuTab.MACRO;
        boolean rewarp = normal && activeTab == MenuTab.REWARP;
        boolean hud = normal && activeTab == MenuTab.HUD;
        boolean failsafe = normal && activeTab == MenuTab.FAILSAFE;
        boolean discord = normal && activeTab == MenuTab.DISCORD;

        setButtonsVisible(generalButtons, general);
        setButtonsVisible(macroButtons, macro);
        setButtonsVisible(rewarpButtons, rewarp);
        setButtonsVisible(hudButtons, hud);
        setButtonsVisible(failsafeButtons, failsafe);
        setButtonsVisible(discordButtons, discord);
        setButtonsVisible(macroTypeButtons, macroTypePick);

        if (webhookIntervalSlider != null) {
            boolean webhookControlsVisible = discord && config.discordWebhookEnabled;
            webhookIntervalSlider.visible = webhookControlsVisible;
            webhookIntervalSlider.active = webhookControlsVisible;
        }
        if (webhookUrlButton != null) {
            boolean webhookButtonVisible = discord && config.discordWebhookEnabled;
            webhookUrlButton.visible = webhookButtonVisible;
            webhookUrlButton.active = webhookButtonVisible;
        }

        if (hudEditButton != null && hudEdit) {
            hudEditButton.visible = false;
            hudEditButton.active = false;
        }

        boolean navigationActive = normal;
        if (tabGeneralButton != null) tabGeneralButton.active = navigationActive;
        if (tabMacroButton != null) tabMacroButton.active = navigationActive;
        if (tabRewarpButton != null) tabRewarpButton.active = navigationActive;
        if (tabHudButton != null) tabHudButton.active = navigationActive;
        if (tabFailsafeButton != null) tabFailsafeButton.active = navigationActive;
        if (tabDiscordButton != null) tabDiscordButton.active = navigationActive;

        if (uiMode == UiMode.WEBHOOK_EDIT || uiMode == UiMode.COLOR_EDIT || uiMode == UiMode.KEY_CAPTURE) {
            setButtonsVisible(generalButtons, false);
            setButtonsVisible(macroButtons, false);
            setButtonsVisible(rewarpButtons, false);
            setButtonsVisible(hudButtons, false);
            setButtonsVisible(failsafeButtons, false);
            setButtonsVisible(discordButtons, false);
        }

        if (soundSlider != null) {
            soundSlider.visible = false;
            soundSlider.active = false;
        }
        if (failsafeVolumeSlider != null) {
            failsafeVolumeSlider.visible = failsafe;
            failsafeVolumeSlider.active = failsafe;
        }
    }

    private void setButtonsVisible(List<AbstractWidget> buttons, boolean visible) {
        for (AbstractWidget button : buttons) {
            button.visible = visible;
            button.active = visible;
        }
    }

    private void updateTabLabels() {
        tabGeneralButton.setLabel("General");
        tabMacroButton.setLabel("Macro");
        tabRewarpButton.setLabel("Rewarp");
        tabHudButton.setLabel("HUD");
        tabFailsafeButton.setLabel("Failsafe");
        tabDiscordButton.setLabel("Discord");
    }

    private void onHudEdit() {
        if (activeTab != MenuTab.HUD) {
            return;
        }
        setUiMode(uiMode == UiMode.HUD_EDIT ? UiMode.NORMAL : UiMode.HUD_EDIT);
    }

    private void onMacroTypeButton() {
        macroTypeDropdownOpen = !macroTypeDropdownOpen;
        applyTabVisibility();
        if (macroTypeDropdownOpen) {
            updateStatus("Pick a macro type", config.uiTextSecondaryColor);
        }
    }

    private void onPickMacroType(ReimoHelperConfig.MacroType type) {
        config.macroType = type;
        if (macroTypeButton != null) {
            macroTypeButton.setMessage(Component.literal("Macro type: " + config.macroType.getDisplayName()));
        }
        config.save();
        macroTypeDropdownOpen = false;
        applyTabVisibility();
        updateStatus("Macro type set to " + type.getDisplayName(), config.uiTextPrimaryColor);
    }

    private void onMacroKeyButton() {
        keyCaptureStartedAt = System.currentTimeMillis();
        setUiMode(UiMode.KEY_CAPTURE);
        updateStatus("Press a key to set macro toggle (Esc to cancel)", config.uiTextSecondaryColor);
    }

    private void openColorEditor() {
        currentColorField = ColorField.ACCENT;
        accentColorInput = String.format("%08X", config.uiAccentColor);
        panelColorInput = String.format("%08X", config.uiPanelColor);
        cardColorInput = String.format("%08X", config.uiCardColor);
        backgroundColorInput = String.format("%08X", config.uiBackgroundColor);
        textPrimaryColorInput = String.format("%08X", config.uiTextPrimaryColor);
        textSecondaryColorInput = String.format("%08X", config.uiTextSecondaryColor);
        warningColorInput = String.format("%08X", config.uiWarningColor);
        setUiMode(UiMode.COLOR_EDIT);
        updateStatus("Edit colours (hex ARGB)", config.uiTextSecondaryColor);
    }

    private void onColorSave() {
        try {
            config.uiAccentColor = (int) Long.parseLong(accentColorInput, 16);
            config.uiPanelColor = (int) Long.parseLong(panelColorInput, 16);
            config.uiCardColor = (int) Long.parseLong(cardColorInput, 16);
            config.uiBackgroundColor = (int) Long.parseLong(backgroundColorInput, 16);
            config.uiTextPrimaryColor = (int) Long.parseLong(textPrimaryColorInput, 16);
            config.uiTextSecondaryColor = (int) Long.parseLong(textSecondaryColorInput, 16);
            config.uiWarningColor = (int) Long.parseLong(warningColorInput, 16);
            config.save();
            setUiMode(UiMode.NORMAL);
            updateStatus("Colours saved", config.uiTextPrimaryColor);
        } catch (Exception ignored) {
            updateStatus("Invalid hex value", COLOR_WARNING);
        }
    }

    private void onColorCancel() {
        setUiMode(UiMode.NORMAL);
        updateStatus("Colour editing cancelled", config.uiTextSecondaryColor);
    }

    private String getCurrentColorInput() {
        return switch (currentColorField) {
            case ACCENT -> accentColorInput;
            case PANEL -> panelColorInput;
            case CARD -> cardColorInput;
            case BACKGROUND -> backgroundColorInput;
            case PRIMARY -> textPrimaryColorInput;
            case SECONDARY -> textSecondaryColorInput;
            case WARNING -> warningColorInput;
        };
    }

    private void setCurrentColorInput(String v) {
        switch (currentColorField) {
            case ACCENT -> accentColorInput = v;
            case PANEL -> panelColorInput = v;
            case CARD -> cardColorInput = v;
            case BACKGROUND -> backgroundColorInput = v;
            case PRIMARY -> textPrimaryColorInput = v;
            case SECONDARY -> textSecondaryColorInput = v;
            case WARNING -> warningColorInput = v;
        }
    }

    private void onEditWebhookUrl() {
        webhookUrlInput = config.discordWebhookUrl == null ? "" : config.discordWebhookUrl;
        setUiMode(UiMode.WEBHOOK_EDIT);
        updateStatus("Webhook URL editor active", config.uiTextSecondaryColor);
    }

    private void onWebhookSave() {
        if (!isValidWebhookUrl(webhookUrlInput)) {
            updateStatus("Invalid webhook URL", COLOR_WARNING);
            return;
        }
        config.discordWebhookUrl = webhookUrlInput;
        config.save();
        webhookUrlButton.setMessage(Component.literal(webhookUrlInput.isEmpty() ? "(empty)" : "Change URL"));
        setUiMode(UiMode.NORMAL);
        updateStatus("Webhook URL saved", config.uiTextPrimaryColor);
    }

    private void onWebhookCancel() {
        webhookUrlButton.setMessage(Component.literal(config.discordWebhookUrl.isEmpty() ? "(empty)" : "Change URL"));
        setUiMode(UiMode.NORMAL);
        updateStatus("Webhook URL editing cancelled", config.uiTextSecondaryColor);
    }

    private void onPasteWebhook() {
        String clipboard = getClipboardAsString();
        if (clipboard != null && !clipboard.isEmpty()) {
            webhookUrlInput = clipboard.trim();
            updateStatus("Pasted webhook URL (" + (lastClipboardBackend == null || lastClipboardBackend.isEmpty() ? "unknown" : lastClipboardBackend) + ")", config.uiTextPrimaryColor);
        } else {
            updateStatus("Clipboard empty", 0xFFFFAA66);
        }
    }

    private boolean isValidWebhookUrl(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        try {
            new java.net.URL(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void onSetRewarp() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        RewarpManager.getInstance().setRewarpLocation(minecraft.player.getOnPos());
        updateStatus("Rewarp saved", config.uiTextPrimaryColor);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        int mods = event.modifiers();

        if (uiMode == UiMode.KEY_CAPTURE) {
            if (key == 256) {
                setUiMode(UiMode.NORMAL);
                updateStatus("Macro key selection cancelled", 0xFFFFAA66);
                return true;
            }
            config.macroToggleKey = key;
            config.save();
            if (macroKeySelectButton != null) {
                macroKeySelectButton.setMessage(Component.literal("Macro Key: " + keyName(key)));
            }
            setUiMode(UiMode.NORMAL);
            updateStatus("Macro toggle key set: " + keyName(key), config.uiTextPrimaryColor);
            return true;
        }

        if (uiMode == UiMode.WEBHOOK_EDIT) {
            if (key == 256) {
                onWebhookCancel();
                return true;
            }
            if (key == 257 || key == 335) {
                onWebhookSave();
                return true;
            }
            if (key == 259) {
                if (!webhookUrlInput.isEmpty()) {
                    webhookUrlInput = webhookUrlInput.substring(0, webhookUrlInput.length() - 1);
                }
                return true;
            }
            if (key == 86 && (mods & 2) != 0) {
                onPasteWebhook();
                return true;
            }
            return true;
        }

        if (uiMode == UiMode.COLOR_EDIT) {
            if (key == 256) {
                onColorCancel();
                return true;
            }
            if (key == 257 || key == 335) {
                onColorSave();
                return true;
            }
            if (key == 259) {
                String field = getCurrentColorInput();
                if (!field.isEmpty()) {
                    setCurrentColorInput(field.substring(0, field.length() - 1));
                }
                return true;
            }
            if (key == 265) {
                currentColorField = ColorField.values()[(currentColorField.ordinal() + ColorField.values().length - 1) % ColorField.values().length];
                return true;
            }
            if (key == 264) {
                currentColorField = ColorField.values()[(currentColorField.ordinal() + 1) % ColorField.values().length];
                return true;
            }
            return true;
        }

        if (uiMode == UiMode.MACRO_TYPE_PICK) {
            if (key == 256) {
                setUiMode(UiMode.NORMAL);
                updateStatus("Macro type selection cancelled", config.uiTextSecondaryColor);
                return true;
            }
            return true;
        }

        if (uiMode == UiMode.HUD_EDIT && key == 256) {
            setUiMode(UiMode.NORMAL);
            return true;
        }

        if (uiMode == UiMode.NORMAL && macroTypeDropdownOpen && key == 256) {
            macroTypeDropdownOpen = false;
            applyTabVisibility();
            updateStatus("Macro type selection cancelled", config.uiTextSecondaryColor);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        int codePoint = event.codepoint();
        char c = (char) codePoint;

        if (uiMode == UiMode.WEBHOOK_EDIT) {
            if (codePoint >= 32 && codePoint <= 126 && webhookUrlInput.length() < 280) {
                webhookUrlInput += c;
            }
            return true;
        }

        if (uiMode == UiMode.COLOR_EDIT) {
            if (Character.toString(c).matches("[0-9A-Fa-f]") && getCurrentColorInput().length() < 8) {
                setCurrentColorInput(getCurrentColorInput() + c);
            }
            return true;
        }

        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (uiMode == UiMode.NORMAL && activeTab == MenuTab.MACRO && macroTypeDropdownOpen && event.button() == 0) {
            boolean clickedTrigger = macroTypeButton != null && macroTypeButton.isMouseOver(mouseX, mouseY);
            boolean clickedOption = isInsideMacroTypeDropdown(mouseX, mouseY);
            if (!clickedTrigger && !clickedOption) {
                macroTypeDropdownOpen = false;
                applyTabVisibility();
            }
        }

        if (uiMode == UiMode.HUD_EDIT && event.button() == 0) {
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

        if (uiMode == UiMode.HUD_EDIT && event.button() == 0 && dragTarget != DragTarget.NONE) {
            if (dragTarget == DragTarget.INVENTORY) {
                int w = InventoryPreviewHud.getPreviewWidth(config) + 6;
                int h = InventoryPreviewHud.getPreviewHeight(config) + 6;
                config.inventoryPreviewX = clamp((int) mouseX - dragOffsetX, 4, width - w - 4);
                config.inventoryPreviewY = clamp((int) mouseY - dragOffsetY, 4, height - h - 4);
                return true;
            }
            if (dragTarget == DragTarget.STATUS) {
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
        if (uiMode == UiMode.HUD_EDIT && dragTarget != DragTarget.NONE) {
            dragTarget = DragTarget.NONE;
            config.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (uiMode == UiMode.HUD_EDIT) {
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

    @Override
    public void render(GuiGraphics gg, int mx, int my, float pt) {
        refreshDynamicMenuText();

        int panelX = layout.panelX;
        int panelY = layout.panelY;
        int panelRight = panelX + layout.panelW;
        int panelBottom = panelY + layout.panelH;
        int panelCenterX = panelX + layout.panelW / 2;

        int bgCol = config.uiBackgroundColor;
        int alpha = (bgCol >>> 24);
        alpha = (int) (alpha * openProgress);
        bgCol = (alpha << 24) | (bgCol & 0x00FFFFFF);

        gg.fill(0, 0, width, height, bgCol);
        fillRoundedRect(gg, panelX, panelY, layout.panelW, layout.panelH, 16, COLOR_SURFACE);

        fillRoundedRect(gg, layout.sidebarX + 8, panelY + 8, layout.sidebarW - 12, layout.panelH - 16, 14, COLOR_SURFACE_SOFT);
        int contentBoxX = layout.contentX - 8;
        int contentBoxY = panelY + 8;
        int contentBoxW = panelRight - contentBoxX - 10;
        int contentBoxH = panelBottom - contentBoxY - 42;
        fillRoundedRect(gg, contentBoxX, contentBoxY, contentBoxW, contentBoxH, 14, COLOR_SURFACE_SOFT);

        int dividerX = layout.sidebarX + layout.sidebarW + 12;
        gg.fill(dividerX - 1, panelY + 16, dividerX + 1, panelBottom - 16, alpha(COLOR_TAUNAHI_ACCENT, 0x66));

        gg.fill(panelX + 24, panelY + 56, panelRight - 24, panelY + 57, 0x22303A54);

        drawMenuString(gg, TEXT_BRAND, panelX + 18, layout.headerY, 0xFFF0F1F5, false);
        drawMenuString(gg, String.format(TEXT_META_TEMPLATE, keyName(config.macroToggleKey)), panelX + 18, layout.headerY + 14, 0xFF9EA6BC, false);
        drawMenuString(gg, activeTabTitle(), layout.contentX + 12, panelY + 22, 0xFFF0F1F5, false);

        drawWidgetRows(gg);
        if (uiMode == UiMode.NORMAL && activeTab == MenuTab.MACRO) {
            drawMacroRowLabels(gg);
        } else if (uiMode == UiMode.NORMAL && activeTab == MenuTab.REWARP) {
            drawRewarpRowLabels(gg);
        } else if (uiMode == UiMode.NORMAL && activeTab == MenuTab.FAILSAFE) {
            drawFailsafeRowLabels(gg);
        } else if (uiMode == UiMode.NORMAL && activeTab == MenuTab.DISCORD && config.discordWebhookEnabled) {
            drawDiscordRowLabels(gg);
        }

        if (uiMode == UiMode.HUD_EDIT) {
            drawHudEditor(gg);
        }

        if (uiMode == UiMode.WEBHOOK_EDIT) {
            drawWebhookUrlInput(gg);
        }

        if (uiMode == UiMode.COLOR_EDIT) {
            drawColorEditor(gg);
        }

        if (uiMode == UiMode.KEY_CAPTURE) {
            drawKeyCaptureOverlay(gg);
        }

        super.render(gg, mx, my, pt);

        if (soundSlider != null && soundSlider.visible) {
            soundSlider.drawValue(gg);
        }
        if (webhookIntervalSlider != null && webhookIntervalSlider.visible) {
            webhookIntervalSlider.drawValue(gg);
        }
        if (failsafeVolumeSlider != null && failsafeVolumeSlider.visible) {
            failsafeVolumeSlider.drawValue(gg);
        }

        if (openProgress < 1f) {
            openProgress = Math.min(1f, openProgress + pt * 2);
        }

        drawActiveTabOutline(gg);
        // Footer text intentionally removed.
    }

    private void drawKeyCaptureOverlay(GuiGraphics gg) {
        int overlayW = 360;
        int overlayH = 80;
        int ox = (width - overlayW) / 2;
        int oy = (height - overlayH) / 2;
        gg.fill(ox - 4, oy - 4, ox + overlayW + 4, oy + overlayH + 4, 0xDD000000);
        gg.fill(ox, oy, ox + overlayW, oy + overlayH, 0xD8121821);
        drawMenuCenteredString(gg, "Press any key to set macro toggle (Esc to cancel)", width / 2, oy + 18, config.uiTextPrimaryColor);
        drawMenuCenteredString(gg, "Listening...", width / 2, oy + 40, config.uiTextSecondaryColor);
    }

    private void drawActiveTabOutline(GuiGraphics gg) {
        Button activeBtn = switch (activeTab) {
            case GENERAL -> tabGeneralButton;
            case MACRO -> tabMacroButton;
            case REWARP -> tabRewarpButton;
            case HUD -> tabHudButton;
            case FAILSAFE -> tabFailsafeButton;
            case DISCORD -> tabDiscordButton;
        };
        if (activeBtn == null) {
            return;
        }

        int ax = activeBtn.getX();
        int ay = activeBtn.getY();
        int aw = activeBtn.getWidth();
        int ah = activeBtn.getHeight();
        int col = COLOR_TAUNAHI_ACCENT;

        gg.fill(ax - 2, ay - 2, ax + aw + 2, ay - 1, col);
        gg.fill(ax - 2, ay + ah + 1, ax + aw + 2, ay + ah + 2, col);
        gg.fill(ax - 2, ay - 1, ax - 1, ay + ah + 1, col);
        gg.fill(ax + aw + 1, ay - 1, ax + aw + 2, ay + ah + 1, col);
    }

    private void drawMacroRowLabels(GuiGraphics gg) {
        drawMenuString(gg, "Macro key", macroLabelX, macroLabelYMacroKey, 0xFFD7DAE3, false);
        drawMenuString(gg, "Macro type", macroLabelX, macroLabelYMacroType, 0xFFD7DAE3, false);
    }

    private void drawRewarpRowLabels(GuiGraphics gg) {
        drawMenuString(gg, "Rewarp command", rewarpLabelX, rewarpLabelYCommand, 0xFFD7DAE3, false);
        drawMenuString(gg, "Rewarp location", rewarpLabelX, rewarpLabelYLocation, 0xFFD7DAE3, false);
    }

    private void drawFailsafeRowLabels(GuiGraphics gg) {
        drawMenuString(gg, "Failsafe sound", failsafeLabelX, failsafeLabelYVolume, 0xFFD7DAE3, false);
    }

    private void drawDiscordRowLabels(GuiGraphics gg) {
        drawMenuString(gg, "Webhook Interval", failsafeLabelX, failsafeLabelYWebhookInterval, 0xFFD7DAE3, false);
        drawMenuString(gg, "Webhook URL", failsafeLabelX, failsafeLabelYWebhookUrl, 0xFFD7DAE3, false);
    }

    private String activeTabTitle() {
        return switch (activeTab) {
            case GENERAL -> "General";
            case MACRO -> "Macro";
            case REWARP -> "Rewarp";
            case HUD -> "HUD";
            case FAILSAFE -> "Failsafe";
            case DISCORD -> "Discord";
        };
    }

    private void refreshDynamicMenuText() {
        if (statsPlayerButton != null) {
            String player = minecraft != null && minecraft.player != null
                ? minecraft.player.getName().getString()
                : MacroStatsStore.getInstance().getLastPlayerName();
            if (player == null || player.isBlank()) {
                player = minecraft != null ? minecraft.getUser().getName() : "-";
            }
            statsPlayerButton.setMessage(Component.literal("Player: " + player));
        }
        if (statsTotalTimeButton != null) {
            long total = MacroStatsStore.getInstance().getTotalMacroSeconds();
            statsTotalTimeButton.setMessage(Component.literal("Total macro time: " + formatDuration(total)));
        }
        if (statsSessionButton != null) {
            int sessions = MacroStatsStore.getInstance().getTotalMacroSessions();
            statsSessionButton.setMessage(Component.literal("Macro sessions: " + sessions));
        }
        if (statsRuntimeButton != null) {
            long currentRuntime = MacroHandler.getInstance().getMacroRuntimeSeconds();
            statsRuntimeButton.setMessage(Component.literal("Current runtime: " + formatDuration(currentRuntime)));
        }
        if (rewarpLocationButton != null) {
            String loc = "Not set";
            if (RewarpManager.getInstance().isRewarpSet()) {
                net.minecraft.core.BlockPos p = RewarpManager.getInstance().getRewarpLocation();
                if (p != null) {
                    loc = p.getX() + ", " + p.getY() + ", " + p.getZ();
                }
            }
            rewarpLocationButton.setMessage(Component.literal(loc));
        }
        if (rewarpCommandButton != null) {
            rewarpCommandButton.setMessage(Component.literal(config.rewarpCommand == null || config.rewarpCommand.isBlank() ? "/warp garden" : config.rewarpCommand));
        }
    }

    private String formatDuration(long totalSeconds) {
        long h = totalSeconds / 3600L;
        long m = (totalSeconds % 3600L) / 60L;
        long s = totalSeconds % 60L;
        if (h > 0L) {
            return h + "h " + m + "m";
        }
        return m + "m " + s + "s";
    }

    private String buildContextHint() {
        String rewarp = RewarpManager.getInstance().isRewarpSet() ? "Rewarp set" : "Rewarp not set";
        return switch (uiMode) {
            case HUD_EDIT -> "Drag HUD boxes to move, scroll over a box to resize | " + rewarp;
            case WEBHOOK_EDIT -> "Enter saves, Esc cancels, Ctrl+V pastes";
            case COLOR_EDIT -> "Up/Down selects field, Enter saves, Esc cancels";
            case KEY_CAPTURE -> "Press any key to bind macro toggle, Esc cancels";
            case MACRO_TYPE_PICK -> "Pick a macro type, Esc cancels";
            case NORMAL -> switch (activeTab) {
                case GENERAL -> "Player and macro stats";
                case MACRO -> "Set macro key, type and mouse ungrab";
                case REWARP -> "Set rewarp point and review command/location";
                case HUD -> "Use Edit HUD Layout to adjust HUD positions | " + rewarp;
                case FAILSAFE -> "Failsafe toggles and alert sound";
                case DISCORD -> "Webhook toggle and interval settings";
            };
        };
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
        drawMenuString(gg, "Inventory HUD", invX + 4, invY - 10, 0xFFD1F5F0, false);

        gg.fill(stX, stY, stX + stW, stY + stH, 0x332E66C7);
        gg.fill(stX, stY, stX + stW, stY + 2, 0xFF5CA8FF);
        gg.fill(stX, stY + stH - 2, stX + stW, stY + stH, 0xFF5CA8FF);
        gg.fill(stX + stW - 6, stY + stH - 6, stX + stW - 2, stY + stH - 2, 0xFFFFFFFF);
        drawMenuString(gg, "Status HUD", stX + 4, stY - 10, 0xFFD6E8FF, false);
    }

    private void drawWebhookUrlInput(GuiGraphics gg) {
        int x = webhookModalX;
        int y = webhookModalY;
        int w = webhookModalW;
        int h = webhookModalH;

        // subtle border instead of heavy black outline
        gg.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x332F3B57);
        fillRoundedRect(gg, x, y, w, h, 12, COLOR_SURFACE_SOFT);
        gg.fill(x + 10, y + 8, x + w - 10, y + 9, alpha(COLOR_TAUNAHI_ACCENT, 0x88));

        drawMenuString(gg, "Discord Webhook URL", x + 10, y + 10, 0xFFFFFFFF, false);

        int boxY = y + 34;
        int boxH = BUTTON_H;
        int inputW = w - 98;
        fillRoundedRect(gg, x + 8, boxY, inputW, boxH, 10, COLOR_SURFACE_WIDGET);

        boolean valid = isValidWebhookUrl(webhookUrlInput);
        int textColor = webhookUrlInput.isEmpty() ? 0xFF888888 : (valid ? 0xFFFFFFFF : COLOR_WARNING);
        String display = webhookUrlInput.isEmpty() ? "paste your webhook URL here" : fitToWidth(webhookUrlInput, inputW - 10);
        drawMenuString(gg, display, x + 12, boxY + (boxH - 8) / 2, textColor, false);

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = x + 12 + menuWidth(display);
            if (cursorX < x + 8 + inputW - 6) {
                gg.fill(cursorX, boxY + 5, cursorX + 1, boxY + boxH - 5, 0xFFFFFFFF);
            }
        }

        drawMenuString(gg, "Enter: save | Esc: cancel | Paste/Ctrl+V", x + 8, y + h - 14, config.uiTextSecondaryColor, false);
    }

    private void drawColorEditor(GuiGraphics gg) {
        int x = colorModalX;
        int y = colorModalY;
        int w = colorModalW;
        int h = colorModalH;

        gg.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xDD000000);
        gg.fill(x, y, x + w, y + h, COLOR_SURFACE_SOFT);
        gg.fill(x, y, x + w, y + 2, COLOR_TAUNAHI_ACCENT);
        gg.fill(x, y + h - 2, x + w, y + h, COLOR_TAUNAHI_ACCENT);

        drawMenuString(gg, "UI Colours (hex ARGB)", x + 10, y + 10, 0xFFFFFFFF, false);

        String[] labels = {"Accent", "Panel", "Card", "Background", "Text primary", "Text secondary", "Warning"};
        String[] values = {
            accentColorInput,
            panelColorInput,
            cardColorInput,
            backgroundColorInput,
            textPrimaryColorInput,
            textSecondaryColorInput,
            warningColorInput
        };

        for (int i = 0; i < labels.length; i++) {
            int ty = y + 28 + i * 16;
            int col = (currentColorField.ordinal() == i) ? 0xFFFFFF : 0xAAB2C5;
            drawMenuString(gg, labels[i] + ":", x + 10, ty, col, false);
            drawMenuString(gg, values[i], x + 112, ty, col, false);
        }

        drawMenuString(gg, "Use up/down to select, type hex, Enter to save, Esc to cancel", x + 10, y + h - 20, config.uiTextSecondaryColor, false);
    }

    private void drawWidgetRows(GuiGraphics gg) {
        if (uiMode != UiMode.NORMAL) {
            return;
        }
        List<AbstractWidget> widgets = switch (activeTab) {
            case GENERAL -> generalButtons;
            case MACRO -> macroButtons;
            case REWARP -> rewarpButtons;
            case HUD -> hudButtons;
            case FAILSAFE -> failsafeButtons;
            case DISCORD -> discordButtons;
        };
        java.util.Set<Integer> rowYs = new java.util.TreeSet<>();
        for (AbstractWidget widget : widgets) {
            if (!widget.visible) {
                continue;
            }
            rowYs.add(widget.getY());
        }

        int rowX = layout.contentX + layout.contentPad - 4;
        int rowW = layout.contentW - layout.contentPad * 2 + 8;
        for (Integer rowY : rowYs) {
            int y = rowY - 3;
            int h = BUTTON_H + 6;
            fillRoundedRect(gg, rowX, y, rowW, h, 12, COLOR_SURFACE_WIDGET);
        }
    }

    private void fillRoundedRect(GuiGraphics gg, int x, int y, int w, int h, int r, int col) {
        int rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (rr == 0) {
            gg.fill(x, y, x + w, y + h, col);
            return;
        }
        // Fill center body first.
        gg.fill(x, y + rr, x + w, y + h - rr, col);

        // Draw top/bottom rounded caps symmetrically so lower corners match upper ones.
        double radius = rr - 0.5d;
        for (int i = 0; i < rr; i++) {
            double dy = (rr - 1 - i) + 0.5d;
            int inset = (int) Math.floor(radius - Math.sqrt(Math.max(0.0d, radius * radius - dy * dy)));
            int left = x + inset;
            int right = x + w - inset;
            gg.fill(left, y + i, right, y + i + 1, col);
            gg.fill(left, y + h - 1 - i, right, y + h - i, col);
        }
    }

    private FormattedCharSequence menuText(String text) {
        return Component.literal(text).getVisualOrderText();
    }

    private int menuWidth(String text) {
        return font.width(menuText(text));
    }

    private void drawMenuString(GuiGraphics gg, String text, int x, int y, int color, boolean shadow) {
        gg.drawString(font, menuText(text), x, y, color, shadow);
    }

    private void drawMenuCenteredString(GuiGraphics gg, String text, int centerX, int y, int color) {
        FormattedCharSequence seq = menuText(text);
        gg.drawString(font, seq, centerX - (font.width(seq) / 2), y, color, false);
    }

    private String fitToWidth(String input, int maxWidth) {
        if (font.width(input) <= maxWidth) {
            return input;
        }
        String ellipsis = "...";
        int idx = input.length();
        while (idx > 0) {
            String candidate = ellipsis + input.substring(idx);
            if (font.width(candidate) <= maxWidth) {
                return candidate;
            }
            idx--;
        }
        return ellipsis;
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

    private boolean isInsideMacroTypeDropdown(double mx, double my) {
        for (AbstractWidget widget : macroTypeButtons) {
            if (widget.visible && widget.isMouseOver(mx, my)) {
                return true;
            }
        }
        return false;
    }

    private int debugLineCount() {
        int base = 7;
        if (config.showDebugHud) {
            base += 3;
        }
        return base;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int alpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private int scaleRgb(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >>> 16) & 0xFF) * factor);
        int g = (int) (((color >>> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (clamp(r, 0, 255) << 16) | (clamp(g, 0, 255) << 8) | clamp(b, 0, 255);
    }

    private void updateStatus(String newStatus, int color) {
        status = newStatus;
        statusColor = color;
    }

    private String keyName(int keyCode) {
        return switch (keyCode) {
            case InputConstants.KEY_GRAVE -> "~";
            case InputConstants.KEY_F6 -> "F6";
            case InputConstants.KEY_F7 -> "F7";
            case InputConstants.KEY_F8 -> "F8";
            case InputConstants.KEY_F9 -> "F9";
            case InputConstants.KEY_F10 -> "F10";
            case InputConstants.KEY_F11 -> "F11";
            case InputConstants.KEY_F12 -> "F12";
            default -> "Key" + keyCode;
        };
    }

    private void cycleMacroKey() {
        int current = config.macroToggleKey;
        int idx = 0;
        for (int i = 0; i < MACRO_TOGGLE_KEYS.length; i++) {
            if (MACRO_TOGGLE_KEYS[i] == current) {
                idx = i;
                break;
            }
        }
        idx = (idx + 1) % MACRO_TOGGLE_KEYS.length;
        config.macroToggleKey = MACRO_TOGGLE_KEYS[idx];
        if (macroKeySelectButton != null) {
            macroKeySelectButton.setMessage(Component.literal("Macro Key: " + keyName(config.macroToggleKey)));
        }
        config.save();
    }

    private String getClipboardAsString() {
        try {
            java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
            java.awt.datatransfer.Clipboard clipboard = toolkit.getSystemClipboard();
            java.awt.datatransfer.DataFlavor flavor = java.awt.datatransfer.DataFlavor.stringFlavor;
            if (clipboard.isDataFlavorAvailable(flavor)) {
                Object data = clipboard.getData(flavor);
                if (data instanceof String str) {
                    LOGGER.info("Clipboard read via AWT");
                    lastClipboardBackend = "awt";
                    return str;
                }
            }
        } catch (Exception ignored) {
            LOGGER.info("AWT clipboard read failed: {}", ignored.toString());
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                String session = System.getenv("XDG_SESSION_TYPE");
                String waylandDisplay = System.getenv("WAYLAND_DISPLAY");

                if (session != null && session.equalsIgnoreCase("wayland") && waylandDisplay != null && !waylandDisplay.isEmpty()) {
                    String out = tryCommand(new String[]{"wl-paste", "-n"});
                    if (out != null && !out.isEmpty()) {
                        lastClipboardBackend = "wl-paste";
                        return out;
                    }
                }

                String out = tryCommand(new String[]{"xclip", "-o", "-selection", "clipboard"});
                if (out != null && !out.isEmpty()) {
                    lastClipboardBackend = "xclip";
                    return out;
                }

                out = tryCommand(new String[]{"xclip", "-o", "-selection", "primary"});
                if (out != null && !out.isEmpty()) {
                    lastClipboardBackend = "xclip-primary";
                    return out;
                }

                out = tryCommand(new String[]{"xsel", "-b", "-o"});
                if (out != null && !out.isEmpty()) {
                    lastClipboardBackend = "xsel";
                    return out;
                }

                out = tryCommand(new String[]{"xsel", "-p", "-o"});
                if (out != null && !out.isEmpty()) {
                    lastClipboardBackend = "xsel-primary";
                    return out;
                }
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
            try (java.io.InputStream is = p.getInputStream();
                 java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (sb.length() > 8192) {
                        break;
                    }
                }
                String out = sb.toString().trim();
                p.destroy();
                if (!out.isEmpty()) {
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

    @Override
    public void onClose() {
        config.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
