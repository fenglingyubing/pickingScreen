package com.fenglingyubing.pickupfilter.client.gui;

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

    private String status = TextFormatting.DARK_GRAY + "同步中…";
    private int lastSnapshotRevision = -1;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int panelWidth = Math.min(420, this.width - 30);
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

        int buttonRowY = panelY + panelHeight - 46;
        int half = (listWidth - 6) / 2;
        addButton = addButton(new GuiButton(1, listX, buttonRowY, half, 20, "添加规则"));
        addHandButton = addButton(new GuiButton(2, listX + half + 6, buttonRowY, half, 20, "添加手持物品"));

        int smallY = panelY + 32;
        int buttonX = listX;
        int gap = 6;

        int cycleW = Math.min(140, listWidth);
        cycleModeButton = addButton(new GuiButton(3, buttonX, smallY, cycleW, 20, "模式：加载中"));
        buttonX += cycleW + gap;

        int removeW = Math.min(90, Math.max(0, listWidth - (cycleW + gap)));
        removeButton = addButton(new GuiButton(4, buttonX, smallY, removeW, 20, "删除选中"));
        buttonX += removeW + gap;

        int applyW = Math.min(70, Math.max(0, listWidth - (cycleW + gap + removeW + gap)));
        applyButton = addButton(new GuiButton(5, buttonX, smallY, applyW, 20, "应用"));
        buttonX += applyW + gap;

        int helpW = Math.max(0, listWidth - (cycleW + gap + removeW + gap + applyW + gap));
        if (helpW >= 50) {
            helpButton = addButton(new GuiButton(6, buttonX, smallY, helpW, 20, "帮助"));
        } else {
            helpButton = null;
        }

        int clientRowY = panelY + 56;
        int clientW = Math.min(160, listWidth);
        clientSettingsButton = addButton(new GuiButton(7, listX, clientRowY, clientW, 18, "背包按钮：调整位置"));

        lastSnapshotRevision = ClientConfigSnapshotStore.getRevision();
        requestSnapshot();
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
        drawRect(0, 0, width, height, COLOR_BG);

        int panelWidth = Math.min(420, this.width - 30);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 26;
        int panelHeight = this.height - 52;

        drawGradientRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL, 0xE0080A0D);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, COLOR_ACCENT);

        drawCenteredString(fontRenderer, TextFormatting.BOLD + "拾取筛 · 配置", this.width / 2, panelY + 10, COLOR_ACCENT);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "输入规则：modid:item@meta  或  modid:*  或  *:item", this.width / 2, panelY + 24, COLOR_MUTED);

        if (listSlot != null) {
            listSlot.drawScreen(mouseX, mouseY, partialTicks);
        }

        if (ruleInput != null) {
            ruleInput.drawTextBox();
        }

        int listX = panelX + 14;
        int panelBottom = panelY + panelHeight;
        drawString(fontRenderer, status == null ? "" : status, listX, panelBottom - 86, COLOR_MUTED);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "提示：添加/删除会自动保存；“应用”仅作手动同步", listX, panelBottom - 98, COLOR_MUTED);

        GlStateManager.disableLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private class RuleListSlot extends GuiSlot {
        private final int x;
        private final int w;

        RuleListSlot(int x, int y, int width, int height) {
            super(PickupFilterConfigScreen.this.mc, width, height, y, y + height, 12);
            this.x = x;
            this.w = width;
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
        protected void drawSlot(int entryId, int insideLeft, int yPos, int insideSlotHeight, int mouseXIn, int mouseYIn, float partialTicks) {
            List<FilterRule> rules = editor.getRules();
            if (entryId < 0 || entryId >= rules.size()) {
                return;
            }

            FilterRule rule = rules.get(entryId);
            String text = rule == null ? "" : rule.serialize();
            int color = isSelected(entryId) ? COLOR_ACCENT : COLOR_TEXT;
            fontRenderer.drawString(text, x + 6, yPos + 2, color);
        }

        @Override
        public int getListWidth() {
            return w;
        }

        @Override
        protected int getScrollBarX() {
            return x + w - 6;
        }
    }
}
