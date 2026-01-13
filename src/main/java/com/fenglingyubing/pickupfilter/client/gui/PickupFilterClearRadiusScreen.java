package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.PickupFilterCommon;
import com.fenglingyubing.pickupfilter.event.DropClearArea;
import com.fenglingyubing.pickupfilter.settings.CommonSettings;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class PickupFilterClearRadiusScreen extends GuiScreen {
    private static final int COLOR_BG = 0xFF05070B;
    private static final int COLOR_PANEL = 0xD0131B1F;
    private static final int COLOR_ACCENT = 0xFF67F3A4;
    private static final int COLOR_SUB = 0xFF9AA7B3;
    private static final int COLOR_TEXT = 0xFFE7EEF5;

    private static final int MIN = 0;
    private static final int MAX = 16;

    private final GuiScreen parent;

    private GuiButton minusButton;
    private GuiButton plusButton;
    private GuiButton resetButton;
    private GuiButton doneButton;

    public PickupFilterClearRadiusScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int panelWidth = Math.min(360, this.width - 30);
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        int centerX = panelX + panelWidth / 2;
        int controlsY = panelY + 128;

        int btnW = 44;
        int btnH = 22;
        int gap = 10;
        minusButton = addButton(new GuiButton(1, centerX - btnW - gap / 2, controlsY, btnW, btnH, "－"));
        plusButton = addButton(new GuiButton(2, centerX + gap / 2, controlsY, btnW, btnH, "＋"));

        int rowY = panelY + panelHeight - 38;
        int half = (panelWidth - 18 - 8) / 2;
        resetButton = addButton(new GuiButton(3, panelX + 9, rowY, half, 20, "恢复默认 (" + DropClearArea.DEFAULT_CHUNK_RADIUS + ")"));
        doneButton = addButton(new GuiButton(4, panelX + 9 + half + 8, rowY, half, 20, "完成"));

        updateButtonStates();
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

        CommonSettings settings = PickupFilterCommon.getCommonSettings();
        int current = settings.getClearDropsChunkRadius();
        if (button.id == 1) {
            settings.setClearDropsChunkRadius(current - 1);
        } else if (button.id == 2) {
            settings.setClearDropsChunkRadius(current + 1);
        } else if (button.id == 3) {
            settings.resetClearDropsChunkRadius();
        } else if (button.id == 4) {
            mc.displayGuiScreen(parent);
            return;
        }

        updateButtonStates();
    }

    private void updateButtonStates() {
        CommonSettings settings = PickupFilterCommon.getCommonSettings();
        int current = settings.getClearDropsChunkRadius();
        if (minusButton != null) {
            minusButton.enabled = current > MIN;
        }
        if (plusButton != null) {
            plusButton.enabled = current < MAX;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawBackdrop(partialTicks);

        int panelWidth = Math.min(360, this.width - 30);
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        drawGradientRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL, 0xD00A0F11);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, COLOR_ACCENT);

        drawCenteredString(fontRenderer, TextFormatting.BOLD + "拾取筛 · 清除范围", this.width / 2, panelY + 16, COLOR_ACCENT);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "按 K 清除附近掉落物/地上箭矢", this.width / 2, panelY + 34, COLOR_SUB);

        CommonSettings settings = PickupFilterCommon.getCommonSettings();
        int chunkRadius = settings.getClearDropsChunkRadius();
        drawCenteredString(fontRenderer, "当前半径：" + TextFormatting.AQUA + chunkRadius + TextFormatting.RESET + " 区块", this.width / 2, panelY + 64, COLOR_TEXT);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "范围：" + MIN + " ~ " + MAX + "（默认 " + DropClearArea.DEFAULT_CHUNK_RADIUS + "）", this.width / 2, panelY + 82, COLOR_SUB);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "提示：以玩家所在区块为中心，向四周扩展", this.width / 2, panelY + 98, COLOR_SUB);

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

