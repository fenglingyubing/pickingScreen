package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.client.PickupFilterClient;
import com.fenglingyubing.pickupfilter.client.settings.ClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class PickupFilterIntroScreen extends GuiScreen {
    private static final int COLOR_BG = 0xFF05070B;
    private static final int COLOR_PANEL = 0xD0131B1F;
    private static final int COLOR_ACCENT = 0xFF67F3A4;
    private static final int COLOR_SUB = 0xFF9AA7B3;

    private GuiButton openConfigButton;
    private GuiButton closeButton;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int panelWidth = Math.min(320, this.width - 40);
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - 180) / 2 + 80;

        openConfigButton = addButton(new GuiButton(1, x, y, panelWidth, 20, "打开配置 (P)"));
        closeButton = addButton(new GuiButton(2, x, y + 26, panelWidth, 20, "我知道了"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        ClientSettings settings = PickupFilterClient.getClientSettings();
        settings.markIntroShown();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }
        if (button.id == 1) {
            Minecraft.getMinecraft().displayGuiScreen(new PickupFilterConfigScreen());
        } else if (button.id == 2) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawBackdrop(partialTicks);

        int panelWidth = Math.min(360, this.width - 30);
        int panelHeight = 214;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        drawGradientRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL, 0xD00A0F11);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, COLOR_ACCENT);

        String title = TextFormatting.BOLD + "拾取筛 / PICKUP FILTER";
        drawCenteredString(fontRenderer, title, this.width / 2, panelY + 18, COLOR_ACCENT);
        drawCenteredString(fontRenderer, "首次使用说明", this.width / 2, panelY + 38, COLOR_SUB);

        int left = panelX + 18;
        int top = panelY + 64;
        int line = 0;

        drawString(fontRenderer, TextFormatting.GRAY + "1) " + TextFormatting.RESET + "按 " + TextFormatting.AQUA + "O" + TextFormatting.RESET + " 切换模式", left, top + line++ * 12, 0xFFE7EEF5);
        drawString(fontRenderer, TextFormatting.GRAY + "2) " + TextFormatting.RESET + "按 " + TextFormatting.AQUA + "K" + TextFormatting.RESET + " 清除附近掉落物", left, top + line++ * 12, 0xFFE7EEF5);
        drawString(fontRenderer, TextFormatting.GRAY + "3) " + TextFormatting.RESET + "按 " + TextFormatting.AQUA + "P" + TextFormatting.RESET + " 打开配置界面，添加/删除过滤规则", left, top + line++ * 12, 0xFFE7EEF5);

        line++;
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "规则输入示例：minecraft:stone@0、minecraft:*、*:dirt", left, top + line++ * 12, 0xFF9AA7B3);

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
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}

