package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.PickupFilterCommon;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterModeCycle;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.network.CycleModePacket;
import com.fenglingyubing.pickupfilter.network.ClientConfigSnapshotStore;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import com.fenglingyubing.pickupfilter.network.RequestConfigSnapshotPacket;
import com.fenglingyubing.pickupfilter.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

public class PickupFilterConfigScreen extends GuiScreen {
    private static final int COLOR_BG = 0xFF05070B;
    private static final int COLOR_PANEL = 0xE012171C;
    private static final int COLOR_ACCENT = 0xFF67F3A4;
    private static final int COLOR_MUTED = 0xFF9AA7B3;
    private static final int COLOR_TEXT = 0xFFE7EEF5;
    private static final int COLOR_TEXT_SOFT = 0xFFCBD6E0;
    private static final int COLOR_OUTLINE_SOFT = 0x4D0B0F14;
    private static final int COLOR_OUTLINE_HARD = 0x80242C35;

    private final FilterRulesEditor editor = new FilterRulesEditor();
    private FilterMode mode = FilterMode.DISABLED;

    private RuleListSlot listSlot;
    private GuiTextField ruleInput;

    private GuiButton addButton;
    private GuiButton addHandButton;
    private GuiButton removeButton;
    private GuiButton applyButton;
    private GuiButton cycleModeButton;
    private GuiButton helpButton;
    private GuiButton clientSettingsButton;
    private GuiButton clearRadiusButton;

    private String status = TextFormatting.DARK_GRAY + "同步中…";
    private int lastSnapshotRevision = -1;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int panelWidth = Math.min(520, this.width - 30);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 26;
        int panelHeight = this.height - 52;

        int listX = panelX + 14;
        int listY = panelY + 84;
        int listWidth = panelWidth - 28;
        int listHeight = panelHeight - 162;
        listSlot = new RuleListSlot(listX, listY, listWidth, listHeight);

        ruleInput = new GuiTextField(10, fontRenderer, listX, panelY + panelHeight - 70, listWidth, 18);
        ruleInput.setMaxStringLength(128);
        ruleInput.setEnableBackgroundDrawing(false);
        ruleInput.setTextColor(COLOR_TEXT);
        ruleInput.setDisabledTextColour(COLOR_MUTED);

        int buttonRowY = panelY + panelHeight - 46;
        int half = (listWidth - 6) / 2;
        addButton = addButton(new NeonButton(1, listX, buttonRowY, half, 20, "添加规则").accent());
        addHandButton = addButton(new NeonButton(2, listX + half + 6, buttonRowY, half, 20, "添加手持物品"));

        int smallY = panelY + 32;
        int buttonX = listX;
        int gap = 6;

        int cycleW = Math.min(140, listWidth);
        cycleModeButton = addButton(new NeonButton(3, buttonX, smallY, cycleW, 20, "模式：加载中"));
        buttonX += cycleW + gap;

        int removeW = Math.min(90, Math.max(0, listWidth - (cycleW + gap)));
        removeButton = addButton(new NeonButton(4, buttonX, smallY, removeW, 20, "删除选中").danger());
        buttonX += removeW + gap;

        int applyW = Math.min(70, Math.max(0, listWidth - (cycleW + gap + removeW + gap)));
        applyButton = addButton(new NeonButton(5, buttonX, smallY, applyW, 20, "应用").accent());
        buttonX += applyW + gap;

        int helpW = Math.max(0, listWidth - (cycleW + gap + removeW + gap + applyW + gap));
        if (helpW >= 50) {
            helpButton = addButton(new NeonButton(6, buttonX, smallY, helpW, 20, "帮助"));
        } else {
            helpButton = null;
        }

        int clientRowY = panelY + 56;
        int halfClientW = Math.min(200, (listWidth - gap) / 2);
        clientSettingsButton = addButton(new NeonButton(7, listX, clientRowY, halfClientW, 18, "背包按钮：调整位置").subtle());
        clearRadiusButton = addButton(new NeonButton(8, listX + halfClientW + gap, clientRowY, listWidth - (halfClientW + gap), 18, "清除范围：加载中").subtle());

        lastSnapshotRevision = ClientConfigSnapshotStore.getRevision();
        requestSnapshot();
        updateClearRadiusButton();
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

        if (button.id == 1) {
            if (editor.addRuleFromUserInput(ruleInput.getText())) {
                ruleInput.setText("");
                status = TextFormatting.GRAY + "已添加并保存…";
                sendRulesUpdate();
            } else {
                status = TextFormatting.RED + "规则无效或已存在";
            }
            return;
        }

        if (button.id == 2) {
            ItemStack held = Minecraft.getMinecraft().player == null ? ItemStack.EMPTY : Minecraft.getMinecraft().player.getHeldItemMainhand();
            if (held == null || held.isEmpty() || held.getItem() == null) {
                status = TextFormatting.RED + "手上没有物品";
                return;
            }
            ResourceLocation registryName = held.getItem().getRegistryName();
            if (registryName == null) {
                status = TextFormatting.RED + "无法读取物品注册名";
                return;
            }
            FilterRule rule = FilterRule.fromItemStack(held);
            if (rule == null) {
                status = TextFormatting.RED + "无法生成规则";
                return;
            }
            if (editor.addRule(rule)) {
                status = TextFormatting.GRAY + "已添加手持物品并保存…";
                sendRulesUpdate();
            } else {
                status = TextFormatting.RED + "规则已存在";
            }
            return;
        }

        if (button.id == 3) {
            PickupFilterNetwork.CHANNEL.sendToServer(new CycleModePacket());
            mode = FilterModeCycle.next(mode);
            status = TextFormatting.GRAY + "已发送模式切换（可稍后同步确认）";
            updateModeButton();
            return;
        }

        if (button.id == 4) {
            if (editor.removeSelected()) {
                status = TextFormatting.GRAY + "已删除并保存…";
                sendRulesUpdate();
            } else {
                status = TextFormatting.RED + "未选择规则";
            }
            return;
        }

        if (button.id == 5) {
            sendRulesUpdate();
            status = TextFormatting.GRAY + "已手动同步…";
            return;
        }

        if (button.id == 6) {
            mc.displayGuiScreen(new PickupFilterIntroScreen());
        } else if (button.id == 7) {
            mc.displayGuiScreen(new PickupFilterClientSettingsScreen(this));
        } else if (button.id == 8) {
            mc.displayGuiScreen(new PickupFilterClearRadiusScreen(this));
        }
    }

    private void requestSnapshot() {
        PickupFilterNetwork.CHANNEL.sendToServer(new RequestConfigSnapshotPacket());
        status = TextFormatting.DARK_GRAY + "同步中…";
        updateModeButton();
    }

    private void sendRulesUpdate() {
        List<FilterRule> rules = editor.getRules();
        PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(mode, rules));
        requestSnapshot();
    }

    private void updateModeButton() {
        if (cycleModeButton != null) {
            String modeName = getModeNameChinese(mode);
            String label = "模式：" + modeName;
            cycleModeButton.displayString = label;
        }
    }

    private static String getModeNameChinese(FilterMode mode) {
        if (mode == null) {
            return "关闭";
        }
        switch (mode) {
            case DESTROY_MATCHING:
                return "销毁匹配掉落物";
            case PICKUP_MATCHING:
                return "拾取匹配掉落物";
            case DISABLED:
            default:
                return "关闭";
        }
    }

    private void applySnapshot(FilterMode mode, List<FilterRule> rules) {
        this.mode = mode == null ? FilterMode.DISABLED : mode;
        editor.replaceAll(rules);
        status = TextFormatting.DARK_GRAY + "已同步：规则 " + editor.getRules().size() + " 条";
        updateModeButton();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (listSlot != null) {
            listSlot.handleMouseInput();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (ruleInput != null) {
            ruleInput.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (ruleInput != null && ruleInput.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (ruleInput != null) {
            ruleInput.updateCursorCounter();
        }
        if (removeButton != null) {
            removeButton.enabled = editor.getSelectedIndex() >= 0;
        }
        updateClearRadiusButton();

        int revision = ClientConfigSnapshotStore.getRevision();
        if (revision != lastSnapshotRevision) {
            lastSnapshotRevision = revision;
            ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
            FilterMode activeMode = snapshot == null ? FilterMode.DISABLED : snapshot.getMode();
            List<FilterRule> activeRules = snapshot == null ? null : snapshot.getRulesForMode(activeMode);
            applySnapshot(activeMode, activeRules);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawBackdrop();

        int panelWidth = Math.min(520, this.width - 30);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 26;
        int panelHeight = this.height - 52;

        drawPanel(panelX, panelY, panelWidth, panelHeight);

        drawCenteredString(fontRenderer, TextFormatting.BOLD + "拾取筛 · 配置", this.width / 2, panelY + 10, COLOR_ACCENT);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "输入规则：modid:item@meta  或  modid:*  或  *:item", this.width / 2, panelY + 24, COLOR_MUTED);

        int listX = panelX + 14;
        int listY = panelY + 84;
        int listWidth = panelWidth - 28;
        int listHeight = panelHeight - 162;
        drawInsetPanel(listX, listY, listWidth, listHeight);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "规则列表（" + editor.getRules().size() + "）", listX + 2, panelY + 76, COLOR_MUTED);

        if (listSlot != null) {
            listSlot.drawScreen(mouseX, mouseY, partialTicks);
        }

        if (ruleInput != null) {
            int inputX = listX;
            int inputY = panelY + panelHeight - 70;
            int inputW = listWidth;
            int inputH = 18;
            drawTextFieldChrome(inputX, inputY, inputW, inputH, ruleInput.isFocused());
            ruleInput.drawTextBox();
            if (!ruleInput.isFocused() && (ruleInput.getText() == null || ruleInput.getText().trim().isEmpty())) {
                drawString(fontRenderer, TextFormatting.DARK_GRAY + "例如：minecraft:rotten_flesh@0  /  minecraft:*  /  *:string", inputX + 7, inputY + 5, COLOR_MUTED);
            }
        }

        int panelBottom = panelY + panelHeight;
        drawString(fontRenderer, status == null ? "" : status, listX, panelBottom - 86, COLOR_TEXT_SOFT);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "提示：添加/删除会自动保存；“应用”仅作手动同步", listX, panelBottom - 98, COLOR_MUTED);

        GlStateManager.disableLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackdrop() {
        drawGradientRect(0, 0, width, height, 0xFF071421, 0xFF05070B);

        int edge = 72;
        drawGradientRect(0, 0, width, edge, 0x2400E5FF, 0x00000000);
        drawGradientRect(0, height - edge, width, height, 0x00000000, 0x2A000000);

        int step = 36;
        int line = 1;
        for (int y = -width; y < height + width; y += step) {
            int x1 = 0;
            int y1 = y;
            int x2 = width;
            int y2 = y + width;
            drawLine(x1, y1, x2, y2, line, 0x0CFFFFFF);
        }
    }

    private void drawPanel(int x, int y, int w, int h) {
        drawGradientRect(x, y, x + w, y + h, COLOR_PANEL, 0xE0090B0E);
        drawRect(x, y, x + w, y + 1, COLOR_ACCENT);
        drawRect(x, y + h - 1, x + w, y + h, COLOR_OUTLINE_SOFT);
        drawRect(x, y, x + 1, y + h, COLOR_OUTLINE_SOFT);
        drawRect(x + w - 1, y, x + w, y + h, COLOR_OUTLINE_SOFT);

        int notch = 10;
        drawRect(x, y, x + notch, y + 2, COLOR_ACCENT);
        drawRect(x + w - notch, y, x + w, y + 2, COLOR_ACCENT);
    }

    private void drawInsetPanel(int x, int y, int w, int h) {
        drawGradientRect(x, y, x + w, y + h, 0xB0090E13, 0xB0060709);
        drawRect(x, y, x + w, y + 1, COLOR_OUTLINE_HARD);
        drawRect(x, y + h - 1, x + w, y + h, COLOR_OUTLINE_HARD);
        drawRect(x, y, x + 1, y + h, COLOR_OUTLINE_HARD);
        drawRect(x + w - 1, y, x + w, y + h, COLOR_OUTLINE_HARD);
    }

    private void drawTextFieldChrome(int x, int y, int w, int h, boolean focused) {
        int glow = focused ? 0x2C67F3A4 : 0x12000000;
        drawGradientRect(x, y, x + w, y + h, 0xCC0B1016, 0xCC07090C);
        drawRect(x, y, x + w, y + 1, focused ? COLOR_ACCENT : COLOR_OUTLINE_HARD);
        drawRect(x, y + h - 1, x + w, y + h, COLOR_OUTLINE_HARD);
        drawRect(x, y, x + 1, y + h, COLOR_OUTLINE_HARD);
        drawRect(x + w - 1, y, x + w, y + h, COLOR_OUTLINE_HARD);
        if (glow != 0) {
            drawRect(x - 1, y - 1, x + w + 1, y, glow);
            drawRect(x - 1, y + h, x + w + 1, y + h + 1, glow);
        }
    }

    private void drawLine(int x1, int y1, int x2, int y2, int thickness, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableAlpha();
        GlStateManager.glLineWidth(Math.max(1, thickness));
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(1, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x1, y1, 0.0D).color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F,
                (color >> 24 & 255) / 255.0F).endVertex();
        buffer.pos(x2, y2, 0.0D).color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F,
                (color >> 24 & 255) / 255.0F).endVertex();
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private class RuleListSlot extends GuiSlot {
        private final int x;
        private final int w;

        RuleListSlot(int x, int y, int width, int height) {
            super(PickupFilterConfigScreen.this.mc, width, height, y, y + height, 14);
            this.x = x;
            this.w = width;
            setSlotXBoundsFromLeft(x);
        }

        @Override
        protected int getSize() {
            return editor.getRules().size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            editor.setSelectedIndex(index);
        }

        @Override
        protected boolean isSelected(int index) {
            return editor.getSelectedIndex() == index;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSelectionBox(int insideLeft, int insideTop, int mouseXIn, int mouseYIn, float partialTicks) {
        }

        @Override
        protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
        }

        @Override
        protected void drawSlot(int entryId, int insideLeft, int yPos, int insideSlotHeight, int mouseXIn, int mouseYIn, float partialTicks) {
            List<FilterRule> rules = editor.getRules();
            if (entryId < 0 || entryId >= rules.size()) {
                return;
            }

            int contentRight = x + w - 10;
            boolean hovered = mouseXIn >= x && mouseXIn <= contentRight && mouseYIn >= yPos && mouseYIn <= (yPos + insideSlotHeight);
            boolean selected = isSelected(entryId);

            int rowBg = (entryId % 2 == 0) ? 0x12000000 : 0x1A000000;
            drawRect(x + 2, yPos - 1, contentRight, yPos + insideSlotHeight - 1, rowBg);
            if (hovered && !selected) {
                drawRect(x + 2, yPos - 1, contentRight, yPos + insideSlotHeight - 1, 0x1400E5FF);
            }
            if (selected) {
                drawRect(x + 2, yPos - 1, contentRight, yPos + insideSlotHeight - 1, 0x2267F3A4);
                drawRect(x + 2, yPos - 1, x + 4, yPos + insideSlotHeight - 1, COLOR_ACCENT);
            }

            FilterRule rule = rules.get(entryId);
            String text = rule == null ? "" : rule.serialize();
            int maxWidth = Math.max(0, w - 14);
            if (fontRenderer.getStringWidth(text) > maxWidth) {
                String ellipsis = "…";
                int available = Math.max(0, maxWidth - fontRenderer.getStringWidth(ellipsis));
                text = fontRenderer.trimStringToWidth(text, available) + ellipsis;
            }
            int color = selected ? COLOR_ACCENT : COLOR_TEXT;
            if (selected || hovered) {
                fontRenderer.drawStringWithShadow(text, x + 8, yPos + 3, color);
            } else {
                fontRenderer.drawString(text, x + 8, yPos + 3, color);
            }
        }

        @Override
        public int getListWidth() {
            return w;
        }

        @Override
        protected int getScrollBarX() {
            return x + w - 6;
        }

        @Override
        public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
            super.drawScreen(mouseXIn, mouseYIn, partialTicks);
            drawScrollbar();
        }

        private void drawScrollbar() {
            int barW = 6;
            int barX1 = getScrollBarX();
            int barX2 = barX1 + barW;
            int barY1 = this.top;
            int barY2 = this.bottom;

            drawRect(barX1, barY1, barX2, barY2, 0x26000000);
            drawRect(barX1, barY1, barX2, barY1 + 1, COLOR_OUTLINE_HARD);
            drawRect(barX1, barY2 - 1, barX2, barY2, COLOR_OUTLINE_HARD);

            int contentHeight = getSize() * this.slotHeight;
            int viewHeight = this.bottom - this.top;
            int maxScroll = Math.max(0, contentHeight - viewHeight);
            if (maxScroll <= 0) {
                return;
            }

            int thumbH = Math.max(18, (viewHeight * viewHeight) / contentHeight);
            thumbH = Math.min(viewHeight - 8, thumbH);
            int thumbY = (int) (this.amountScrolled * (viewHeight - thumbH) / (float) maxScroll) + barY1;
            thumbY = Math.max(barY1, Math.min(barY2 - thumbH, thumbY));

            drawGradientRect(barX1, thumbY, barX2, thumbY + thumbH, 0xCC141B23, 0xCC0F141A);
            drawRect(barX1, thumbY, barX2, thumbY + 1, 0x7A67F3A4);
        }
    }

    private static class NeonButton extends GuiButton {
        private int baseBg = 0xCC0E1319;
        private int baseBg2 = 0xCC090B0E;
        private int border = COLOR_OUTLINE_HARD;
        private int text = COLOR_TEXT;
        private int hoverGlow = 0x1E00E5FF;
        private boolean isAccent;
        private boolean isDanger;
        private boolean isSubtle;

        NeonButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
        }

        NeonButton accent() {
            isAccent = true;
            return this;
        }

        NeonButton danger() {
            isDanger = true;
            return this;
        }

        NeonButton subtle() {
            isSubtle = true;
            return this;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) {
                return;
            }

            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            int bg1 = baseBg;
            int bg2 = baseBg2;
            int top = border;
            int t = text;

            if (isSubtle) {
                bg1 = 0xB30B1016;
                bg2 = 0xB307090C;
                top = COLOR_OUTLINE_HARD;
                t = COLOR_TEXT_SOFT;
            }

            if (!this.enabled) {
                bg1 = 0x80101214;
                bg2 = 0x800A0C0F;
                top = 0x80242C35;
                t = 0xFF6D7884;
            }

            if (isDanger) {
                top = 0xFFE05C5C;
            } else if (isAccent) {
                top = COLOR_ACCENT;
            }

            drawGradientRect(this.x, this.y, this.x + this.width, this.y + this.height, bg1, bg2);
            drawRect(this.x, this.y, this.x + this.width, this.y + 1, top);
            drawRect(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, COLOR_OUTLINE_SOFT);
            drawRect(this.x, this.y, this.x + 1, this.y + this.height, COLOR_OUTLINE_SOFT);
            drawRect(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, COLOR_OUTLINE_SOFT);

            if (this.hovered && this.enabled) {
                drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y, hoverGlow);
                drawRect(this.x - 1, this.y + this.height, this.x + this.width + 1, this.y + this.height + 1, hoverGlow);
            }

            int strWidth = mc.fontRenderer.getStringWidth(this.displayString);
            int maxW = Math.max(0, this.width - 10);
            String s = this.displayString;
            if (strWidth > maxW) {
                s = mc.fontRenderer.trimStringToWidth(s, maxW - mc.fontRenderer.getStringWidth("…")) + "…";
            }
            if (this.hovered && this.enabled) {
                mc.fontRenderer.drawStringWithShadow(s, this.x + (this.width - mc.fontRenderer.getStringWidth(s)) / 2, this.y + (this.height - 8) / 2, t);
            } else {
                mc.fontRenderer.drawString(s, this.x + (this.width - mc.fontRenderer.getStringWidth(s)) / 2, this.y + (this.height - 8) / 2, t);
            }
        }
    }

    private void updateClearRadiusButton() {
        if (clearRadiusButton == null) {
            return;
        }
        int radius = PickupFilterCommon.getCommonSettings().getClearDropsChunkRadius();
        clearRadiusButton.displayString = "清除范围：" + radius + " 区块 (0~16)";
    }
}
