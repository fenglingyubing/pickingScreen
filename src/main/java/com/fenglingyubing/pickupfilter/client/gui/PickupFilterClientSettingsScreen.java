package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.client.PickupFilterClient;
import com.fenglingyubing.pickupfilter.client.settings.ClientSettings;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class PickupFilterClientSettingsScreen extends GuiScreen {
    private static final int COLOR_BG = 0xFF05070B;
    private static final int COLOR_PANEL = 0xD0131B1F;
    private static final int COLOR_ACCENT = 0xFF67F3A4;
    private static final int COLOR_SUB = 0xFF9AA7B3;
    private static final int COLOR_TEXT = 0xFFE7EEF5;

    private static final int STEP = 4;

    private final GuiScreen parent;

    private GuiButton upButton;
    private GuiButton downButton;
    private GuiButton leftButton;
    private GuiButton rightButton;
    private GuiButton resetButton;
    private GuiButton doneButton;

    public PickupFilterClientSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int panelWidth = Math.min(360, this.width - 30);
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        int centerX = panelX + panelWidth / 2;
        int controlsY = panelY + 112;
        int size = 20;
        int gap = 6;

        upButton = addButton(new GuiButton(1, centerX - size / 2, controlsY - size - gap, size, size, "↑"));
        downButton = addButton(new GuiButton(2, centerX - size / 2, controlsY + size + gap, size, size, "↓"));
        leftButton = addButton(new GuiButton(3, centerX - size - gap, controlsY, size, size, "←"));
        rightButton = addButton(new GuiButton(4, centerX + gap, controlsY, size, size, "→"));

        int buttonRowY = panelY + panelHeight - 36;
        int buttonW = (panelWidth - 18 - 8) / 2;
        resetButton = addButton(new GuiButton(5, panelX + 9, buttonRowY, buttonW, 20, "重置偏移"));
        doneButton = addButton(new GuiButton(6, panelX + 9 + buttonW + 8, buttonRowY, buttonW, 20, "完成"));

        updateButtonLabels();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }

        ClientSettings settings = PickupFilterClient.getClientSettings();
        if (button.id == 1) {
            settings.adjustInventoryButtonOffset(0, -STEP);
        } else if (button.id == 2) {
            settings.adjustInventoryButtonOffset(0, STEP);
        } else if (button.id == 3) {
            settings.adjustInventoryButtonOffset(-STEP, 0);
        } else if (button.id == 4) {
            settings.adjustInventoryButtonOffset(STEP, 0);
        } else if (button.id == 5) {
            settings.resetInventoryButtonOffset();
        } else if (button.id == 6) {
            mc.displayGuiScreen(parent);
            return;
        }

        updateButtonLabels();
    }

    private void updateButtonLabels() {
        ClientSettings settings = PickupFilterClient.getClientSettings();
        int x = settings.getInventoryButtonOffsetX();
        int y = settings.getInventoryButtonOffsetY();
        if (resetButton != null) {
            resetButton.displayString = "重置偏移 (X " + formatSigned(x) + " / Y " + formatSigned(y) + ")";
        }
    }

    private static String formatSigned(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawBackdrop(partialTicks);

        int panelWidth = Math.min(360, this.width - 30);
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        drawGradientRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL, 0xD00A0F11);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, COLOR_ACCENT);

        drawCenteredString(fontRenderer, TextFormatting.BOLD + "拾取筛 · 客户端设置", this.width / 2, panelY + 16, COLOR_ACCENT);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "背包界面“筛”按钮", this.width / 2, panelY + 34, COLOR_SUB);

        ClientSettings settings = PickupFilterClient.getClientSettings();
        int x = settings.getInventoryButtonOffsetX();
        int y = settings.getInventoryButtonOffsetY();
        drawCenteredString(fontRenderer, "偏移：X " + formatSigned(x) + "  /  Y " + formatSigned(y), this.width / 2, panelY + 58, COLOR_TEXT);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "用方向键按钮调整（步长 " + STEP + " 像素）", this.width / 2, panelY + 74, COLOR_SUB);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "调整后打开背包 (E) 检查位置", this.width / 2, panelY + 88, COLOR_SUB);

        GlStateManager.disableLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackdrop(float partialTicks) {
        drawRect(0, 0, width, height, COLOR_BG);
        ScaledResolution res = new ScaledResolution(mc);
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int lines = 90;
        for (int i = 0; i < lines; i++) {
            int y = (int) ((i / (float) lines) * h);
            int alpha = (i % 2 == 0) ? 18 : 10;
            drawRect(0, y, w, y + 1, (alpha << 24) | 0x001A2228);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}

