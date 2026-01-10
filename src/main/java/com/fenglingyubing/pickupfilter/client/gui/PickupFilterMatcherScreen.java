package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.network.ClientConfigSnapshotStore;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import com.fenglingyubing.pickupfilter.network.RequestConfigSnapshotPacket;
import com.fenglingyubing.pickupfilter.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PickupFilterMatcherScreen extends GuiScreen {
    private static final int SLOT = 18;

    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 3;
    private static final int GRID_W = GRID_COLS * SLOT;
    private static final int GRID_H = GRID_ROWS * SLOT;
    private static final int RULES_PER_PAGE = GRID_COLS * GRID_ROWS;

    private static final int INV_MAIN_ROWS = 3;
    private static final int INV_HOTBAR_ROWS = 1;
    private static final int INV_ROWS = INV_MAIN_ROWS + INV_HOTBAR_ROWS;
    private static final int INV_H = INV_ROWS * SLOT + 4;

    private static final int COLOR_DIM = 0xC014161B;
    private static final int COLOR_PANEL = 0xE013171C;
    private static final int COLOR_PANEL_2 = 0xE0101317;
    private static final int COLOR_BORDER = 0xFF2B333D;
    private static final int COLOR_ACCENT = 0xFF43E3A8;
    private static final int COLOR_TEXT = 0xFFE7EEF5;
    private static final int COLOR_MUTED = 0xFF93A3B2;
    private static final int COLOR_SLOT_BG = 0xFF0B0F14;
    private static final int COLOR_SLOT_EDGE = 0xFF2B333D;

    private final GuiScreen parent;

    private GuiButton tabPickupButton;
    private GuiButton tabDestroyButton;
    private GuiButton prevPageButton;
    private GuiButton nextPageButton;

    private GuiButton applyButton;
    private GuiButton clearButton;
    private GuiButton openConfigButton;
    private GuiButton backButton;

    private String status = TextFormatting.DARK_GRAY + "同步中…";
    private int lastSnapshotRevision = -1;

    private FilterMode currentMode = FilterMode.DISABLED;
    private FilterMode editingMode = FilterMode.PICKUP_MATCHING;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private boolean splitLayout;

    private int rulesLabelY;
    private int rulesX;
    private int rulesY;

    private int invLabelY;
    private int invX;
    private int invY;
    private int hotbarY;

    private int pageIndex;
    private int hiddenRulesCount;
    private final List<FilterRule> hiddenRules = new ArrayList<>();
    private final List<FilterRule> itemRules = new ArrayList<>();

    public PickupFilterMatcherScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        panelW = Math.min(520, this.width - 24);
        panelH = Math.min(304, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        splitLayout = panelW >= 420;

        int headerH = 56;
        int footerH = 46;
        int contentTop = panelY + headerH;

        int pad = 18;
        int blockTop = contentTop + 18;

        if (splitLayout) {
            invX = panelX + pad;
            rulesX = panelX + panelW - pad - GRID_W;
            invLabelY = blockTop - 12;
            rulesLabelY = blockTop - 12;
            invY = blockTop;
            rulesY = blockTop;
        } else {
            rulesX = panelX + (panelW - GRID_W) / 2;
            invX = rulesX;
            rulesLabelY = blockTop - 12;
            rulesY = blockTop;
            invLabelY = rulesY + GRID_H + 22;
            invY = invLabelY + 10;
        }
        hotbarY = invY + INV_MAIN_ROWS * SLOT + 4;

        int tabY = panelY + 30;
        int tabX = panelX + pad;
        tabPickupButton = addButton(new GuiButton(21, tabX, tabY, 78, 18, "拾取列表"));
        tabDestroyButton = addButton(new GuiButton(22, tabX + 84, tabY, 78, 18, "销毁列表"));

        int pagerY = rulesLabelY - 2;
        prevPageButton = addButton(new GuiButton(23, rulesX + GRID_W - 30, pagerY, 14, 14, "<"));
        nextPageButton = addButton(new GuiButton(24, rulesX + GRID_W - 14, pagerY, 14, 14, ">"));

        int bottomY = panelY + panelH - 34;
        int gap = 8;
        int buttonsW = panelW - pad * 2 - gap * 3;
        int w = Math.max(70, buttonsW / 4);
        int x = panelX + pad;
        applyButton = addButton(new GuiButton(31, x, bottomY, w, 20, "同步到服务器"));
        x += w + gap;
        clearButton = addButton(new GuiButton(32, x, bottomY, w, 20, "清空列表"));
        x += w + gap;
        openConfigButton = addButton(new GuiButton(33, x, bottomY, w, 20, "打开配置"));
        x += w + gap;
        backButton = addButton(new GuiButton(34, x, bottomY, w, 20, "返回背包"));

        int revisionBefore = ClientConfigSnapshotStore.getRevision();
        ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
        currentMode = snapshot == null ? FilterMode.DISABLED : snapshot.getMode();
        editingMode = defaultEditingModeForCurrentMode(currentMode);
        loadFromSnapshot(snapshot, editingMode);

        lastSnapshotRevision = revisionBefore;
        requestSnapshot();
        refreshButtons();
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

        if (button.id == 21) {
            switchEditingMode(FilterMode.PICKUP_MATCHING);
            return;
        }
        if (button.id == 22) {
            switchEditingMode(FilterMode.DESTROY_MATCHING);
            return;
        }

        if (button.id == 23) {
            if (pageIndex > 0) {
                pageIndex--;
                refreshButtons();
            }
            return;
        }
        if (button.id == 24) {
            int totalPages = getTotalPages();
            if (pageIndex + 1 < totalPages) {
                pageIndex++;
                refreshButtons();
            }
            return;
        }

        if (button.id == 31) {
            List<FilterRule> toSend = composeRulesToSend();
            PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(editingMode, toSend));
            status = TextFormatting.GRAY + "已提交同步（" + toSend.size() + " 条）…";
            requestSnapshot();
            return;
        }

        if (button.id == 32) {
            itemRules.clear();
            pageIndex = 0;
            autoSave(TextFormatting.DARK_GRAY + "已清空列表（保留高级规则 " + hiddenRulesCount + " 条）");
            return;
        }

        if (button.id == 33) {
            mc.displayGuiScreen(new PickupFilterConfigScreen());
            return;
        }

        if (button.id == 34) {
            mc.displayGuiScreen(parent);
        }
    }

    private void switchEditingMode(FilterMode target) {
        FilterMode next = target == FilterMode.DESTROY_MATCHING ? FilterMode.DESTROY_MATCHING : FilterMode.PICKUP_MATCHING;
        if (editingMode == next) {
            return;
        }
        editingMode = next;
        pageIndex = 0;
        loadFromSnapshot(ClientConfigSnapshotStore.getSnapshot(), editingMode);
        status = TextFormatting.DARK_GRAY + "正在编辑：" + getListName(editingMode);
        refreshButtons();
    }

    private static FilterMode defaultEditingModeForCurrentMode(FilterMode mode) {
        if (mode == FilterMode.DESTROY_MATCHING) {
            return FilterMode.DESTROY_MATCHING;
        }
        return FilterMode.PICKUP_MATCHING;
    }

    private void requestSnapshot() {
        PickupFilterNetwork.CHANNEL.sendToServer(new RequestConfigSnapshotPacket());
        status = TextFormatting.DARK_GRAY + "同步中…";
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        int revision = ClientConfigSnapshotStore.getRevision();
        if (revision != lastSnapshotRevision) {
            lastSnapshotRevision = revision;
            ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
            currentMode = snapshot == null ? FilterMode.DISABLED : snapshot.getMode();
            loadFromSnapshot(snapshot, editingMode);
            status = TextFormatting.DARK_GRAY + "已同步：拾取 " + (snapshot == null ? 0 : snapshot.getPickupRules().size())
                    + " / 销毁 " + (snapshot == null ? 0 : snapshot.getDestroyRules().size());
        }

        refreshButtons();
    }

    private void refreshButtons() {
        int totalPages = getTotalPages();
        if (pageIndex >= totalPages) {
            pageIndex = Math.max(0, totalPages - 1);
        }
        if (prevPageButton != null) {
            prevPageButton.enabled = totalPages > 1 && pageIndex > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.enabled = totalPages > 1 && pageIndex + 1 < totalPages;
        }
        if (tabPickupButton != null) {
            tabPickupButton.enabled = editingMode != FilterMode.PICKUP_MATCHING;
        }
        if (tabDestroyButton != null) {
            tabDestroyButton.enabled = editingMode != FilterMode.DESTROY_MATCHING;
        }
        if (clearButton != null) {
            clearButton.enabled = !itemRules.isEmpty();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && isMouseOverRulesGrid()) {
            if (wheel < 0) {
                int totalPages = getTotalPages();
                if (pageIndex + 1 < totalPages) {
                    pageIndex++;
                    refreshButtons();
                }
            } else if (pageIndex > 0) {
                pageIndex--;
                refreshButtons();
            }
            return;
        }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 1) {
            int ruleSlot = getRuleSlotAt(mouseX, mouseY);
            if (ruleSlot >= 0) {
                int index = pageIndex * RULES_PER_PAGE + ruleSlot;
                if (index >= 0 && index < itemRules.size()) {
                    FilterRule removed = itemRules.remove(index);
                    autoSave(TextFormatting.DARK_GRAY + "已移除：" + (removed == null ? "" : removed.serialize()));
                }
                return;
            }
        }

        if (mouseButton != 0) {
            return;
        }

        int invSlot = getInventoryDisplayIndexAt(mouseX, mouseY);
        if (invSlot < 0) {
            return;
        }

        ItemStack stack = getDisplayedInventoryStack(invSlot);
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            status = TextFormatting.DARK_GRAY + "该格子没有物品";
            return;
        }

        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) {
            status = TextFormatting.RED + "无法读取物品注册名";
            return;
        }

        FilterRule rule = new FilterRule(registryName.getNamespace(), registryName.getPath(), stack.getMetadata(), false);
        if (!canDisplayRule(rule)) {
            status = TextFormatting.RED + "该物品无法添加（注册名异常）";
            return;
        }

        if (itemRules.contains(rule) || hiddenRules.contains(rule)) {
            status = TextFormatting.DARK_GRAY + "已在列表中";
            return;
        }

        if (itemRules.size() >= 200) {
            status = TextFormatting.RED + "规则已达上限（200）";
            return;
        }

        itemRules.add(rule);
        autoSave(TextFormatting.GRAY + "已添加："
                + TextFormatting.AQUA + registryName.getNamespace()
                + TextFormatting.GRAY + ":"
                + TextFormatting.AQUA + registryName.getPath()
                + TextFormatting.DARK_GRAY + " @"
                + TextFormatting.AQUA + stack.getMetadata()
                + TextFormatting.DARK_GRAY + "（右键匹配列表可移除）");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(0, 0, width, height, COLOR_DIM);

        drawPanel();
        drawHeader();
        drawRulesGrid(mouseX, mouseY);
        drawInventory(mouseX, mouseY);
        drawFooter();

        GlStateManager.disableLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel() {
        drawRect(panelX + 2, panelY + 2, panelX + panelW + 2, panelY + panelH + 2, 0x55000000);
        drawGradientRect(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL, COLOR_PANEL_2);
        drawRect(panelX, panelY, panelX + panelW, panelY + 1, COLOR_BORDER);
        drawRect(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, COLOR_BORDER);
        drawRect(panelX, panelY, panelX + 1, panelY + panelH, COLOR_BORDER);
        drawRect(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, COLOR_BORDER);
        drawRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 2, COLOR_ACCENT);
    }

    private void drawHeader() {
        String title = TextFormatting.BOLD + "拾取筛" + TextFormatting.RESET + TextFormatting.DARK_GRAY + " - 物品匹配";
        drawCenteredString(fontRenderer, title, panelX + panelW / 2, panelY + 12, COLOR_TEXT);

        String modeLine = TextFormatting.DARK_GRAY + "当前模式："
                + TextFormatting.AQUA + getModeNameChinese(currentMode)
                + TextFormatting.DARK_GRAY + "  |  正在编辑："
                + TextFormatting.AQUA + getListName(editingMode);
        drawString(fontRenderer, modeLine, panelX + 18, panelY + 46, COLOR_MUTED);
    }

    private void drawFooter() {
        int footerY = panelY + panelH - 42;
        drawRect(panelX + 18, footerY - 10, panelX + panelW - 18, footerY - 9, 0x332B333D);

        String hint = TextFormatting.DARK_GRAY + "提示：左键从背包添加；右键从列表移除；自动保存";
        if (hiddenRulesCount > 0) {
            hint += TextFormatting.DARK_GRAY + "；未显示高级规则 " + hiddenRulesCount + " 条";
        }

        drawString(fontRenderer, status == null ? "" : status, panelX + 18, footerY, COLOR_MUTED);
        drawString(fontRenderer, hint, panelX + 18, footerY + 12, COLOR_MUTED);
    }

    private void drawRulesGrid(int mouseX, int mouseY) {
        String label = TextFormatting.BOLD + "匹配列表" + TextFormatting.RESET + TextFormatting.DARK_GRAY + "（右键移除）";
        drawString(fontRenderer, label, rulesX, rulesLabelY, COLOR_TEXT);

        String pageText = (pageIndex + 1) + "/" + getTotalPages();
        drawString(fontRenderer, TextFormatting.DARK_GRAY + pageText, rulesX + GRID_W - fontRenderer.getStringWidth(pageText) - 34, rulesLabelY, COLOR_MUTED);

        for (int slotIndex = 0; slotIndex < RULES_PER_PAGE; slotIndex++) {
            int col = slotIndex % GRID_COLS;
            int row = slotIndex / GRID_COLS;
            int x = rulesX + col * SLOT;
            int y = rulesY + row * SLOT;
            drawSlot(x, y);

            int ruleIndex = pageIndex * RULES_PER_PAGE + slotIndex;
            if (ruleIndex >= 0 && ruleIndex < itemRules.size()) {
                FilterRule rule = itemRules.get(ruleIndex);
                ItemStack icon = createIcon(rule);
                if (icon != null && !icon.isEmpty()) {
                    RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.enableRescaleNormal();
                    itemRender.renderItemAndEffectIntoGUI(icon, x + 1, y + 1);
                    RenderHelper.disableStandardItemLighting();
                } else if (rule != null) {
                    fontRenderer.drawString("?", x + 6, y + 6, COLOR_MUTED);
                }

                if (isPointInRect(x, y, SLOT, SLOT, mouseX, mouseY)) {
                    drawRect(x, y, x + SLOT, y + SLOT, 0x2243E3A8);
                    if (icon != null && !icon.isEmpty()) {
                        renderToolTip(icon, mouseX, mouseY);
                    } else if (rule != null) {
                        List<String> lines = new ArrayList<>();
                        lines.add(TextFormatting.GRAY + "规则：" + TextFormatting.AQUA + rule.serialize());
                        lines.add(TextFormatting.DARK_GRAY + "右键移除");
                        drawHoveringText(lines, mouseX, mouseY);
                    }
                }
            } else if (isPointInRect(x, y, SLOT, SLOT, mouseX, mouseY)) {
                drawRect(x, y, x + SLOT, y + SLOT, 0x22000000);
            }
        }
    }

    private void drawInventory(int mouseX, int mouseY) {
        drawString(fontRenderer, TextFormatting.BOLD + "背包" + TextFormatting.RESET + TextFormatting.DARK_GRAY + "（左键添加）", invX, invLabelY, COLOR_TEXT);

        for (int displayIndex = 0; displayIndex < 36; displayIndex++) {
            int row;
            int col = displayIndex % GRID_COLS;
            if (displayIndex < 27) {
                row = displayIndex / GRID_COLS;
            } else {
                row = INV_MAIN_ROWS + (displayIndex - 27) / GRID_COLS;
            }

            int x = invX + col * SLOT;
            int y = invY + row * SLOT + (displayIndex >= 27 ? 4 : 0);
            drawSlot(x, y);

            ItemStack stack = getDisplayedInventoryStack(displayIndex);
            if (stack != null && !stack.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                itemRender.renderItemAndEffectIntoGUI(stack, x + 1, y + 1);
                itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x + 1, y + 1, null);
                RenderHelper.disableStandardItemLighting();
            }

            if (isPointInRect(x, y, SLOT, SLOT, mouseX, mouseY)) {
                drawRect(x, y, x + SLOT, y + SLOT, 0x2243E3A8);
                if (stack != null && !stack.isEmpty()) {
                    renderToolTip(stack, mouseX, mouseY);
                }
            }
        }

        drawRect(invX, hotbarY - 3, invX + GRID_W, hotbarY - 2, 0x332B333D);
    }

    private void drawSlot(int x, int y) {
        drawRect(x, y, x + SLOT, y + SLOT, COLOR_SLOT_BG);
        drawRect(x, y, x + SLOT, y + 1, COLOR_SLOT_EDGE);
        drawRect(x, y, x + 1, y + SLOT, COLOR_SLOT_EDGE);
        drawRect(x + SLOT - 1, y, x + SLOT, y + SLOT, 0xFF3A4552);
        drawRect(x, y + SLOT - 1, x + SLOT, y + SLOT, 0xFF3A4552);
        drawRect(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x12FFFFFF);
    }

    private int getRuleSlotAt(int mouseX, int mouseY) {
        if (!isPointInRect(rulesX, rulesY, GRID_W, GRID_H, mouseX, mouseY)) {
            return -1;
        }
        int relX = mouseX - rulesX;
        int relY = mouseY - rulesY;
        int col = relX / SLOT;
        int row = relY / SLOT;
        int index = row * GRID_COLS + col;
        return index < 0 || index >= RULES_PER_PAGE ? -1 : index;
    }

    private boolean isMouseOverRulesGrid() {
        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        return isPointInRect(rulesX, rulesY, GRID_W, GRID_H, mx, my);
    }

    private int getInventoryDisplayIndexAt(int mouseX, int mouseY) {
        int totalH = INV_MAIN_ROWS * SLOT + 4 + INV_HOTBAR_ROWS * SLOT;
        if (!isPointInRect(invX, invY, GRID_W, totalH, mouseX, mouseY)) {
            return -1;
        }

        int relX = mouseX - invX;
        int col = relX / SLOT;

        if (mouseY < hotbarY) {
            int relY = mouseY - invY;
            int row = relY / SLOT;
            if (row < 0 || row >= INV_MAIN_ROWS) {
                return -1;
            }
            return row * GRID_COLS + col;
        }

        int relHotbarY = mouseY - hotbarY;
        int row = relHotbarY / SLOT;
        if (row < 0 || row >= INV_HOTBAR_ROWS) {
            return -1;
        }
        return 27 + row * GRID_COLS + col;
    }

    private ItemStack getDisplayedInventoryStack(int displayIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.inventory == null || mc.player.inventory.mainInventory == null) {
            return ItemStack.EMPTY;
        }
        if (displayIndex < 0 || displayIndex >= 36) {
            return ItemStack.EMPTY;
        }

        int invIndex = displayIndex < 27 ? displayIndex + 9 : displayIndex - 27;
        return mc.player.inventory.mainInventory.get(invIndex);
    }

    private void loadFromSnapshot(ClientConfigSnapshotStore.Snapshot snapshot, FilterMode listMode) {
        hiddenRules.clear();
        itemRules.clear();
        hiddenRulesCount = 0;

        if (snapshot == null) {
            return;
        }

        List<FilterRule> rules = snapshot.getRulesForMode(listMode);
        if (rules != null) {
            for (FilterRule rule : rules) {
                if (rule == null) {
                    continue;
                }
                if (canDisplayRule(rule)) {
                    if (!itemRules.contains(rule)) {
                        itemRules.add(rule);
                    }
                } else {
                    if (!hiddenRules.contains(rule)) {
                        hiddenRules.add(rule);
                    }
                }
            }
        }

        hiddenRulesCount = hiddenRules.size();
        pageIndex = Math.min(pageIndex, Math.max(0, getTotalPages() - 1));
    }

    private void autoSave(String newStatus) {
        if (newStatus != null) {
            status = newStatus;
        }
        List<FilterRule> toSend = composeRulesToSend();
        PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(editingMode, toSend));
        requestSnapshot();
    }

    private List<FilterRule> composeRulesToSend() {
        List<FilterRule> merged = new ArrayList<>();
        for (FilterRule rule : hiddenRules) {
            if (rule != null && !merged.contains(rule)) {
                merged.add(rule);
            }
        }
        for (FilterRule rule : itemRules) {
            if (rule != null && !merged.contains(rule)) {
                merged.add(rule);
            }
        }
        if (merged.size() > 200) {
            merged = merged.subList(0, 200);
        }
        return merged;
    }

    private int getTotalPages() {
        return Math.max(1, (itemRules.size() + RULES_PER_PAGE - 1) / RULES_PER_PAGE);
    }

    private static boolean isPointInRect(int x, int y, int w, int h, int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private static boolean canDisplayRule(FilterRule rule) {
        if (rule == null) {
            return false;
        }
        if (rule.isUseWildcard()) {
            return false;
        }
        String modId = rule.getModId();
        String itemName = rule.getItemName();
        if (modId == null || itemName == null) {
            return false;
        }
        if (FilterRule.ANY.equals(modId) || FilterRule.ANY.equals(itemName)) {
            return false;
        }
        return true;
    }

    private static ItemStack createIcon(FilterRule rule) {
        if (!canDisplayRule(rule)) {
            return ItemStack.EMPTY;
        }
        Item item = Item.getByNameOrId(rule.getModId() + ":" + rule.getItemName());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        int meta = rule.getMetadata() == FilterRule.ANY_METADATA ? 0 : rule.getMetadata();
        return new ItemStack(item, 1, meta);
    }

    private static String getListName(FilterMode mode) {
        return mode == FilterMode.DESTROY_MATCHING ? "销毁匹配列表" : "拾取匹配列表";
    }

    private static String getModeNameChinese(FilterMode mode) {
        if (mode == null) {
            return "关闭";
        }
        switch (mode) {
            case DESTROY_MATCHING:
                return "销毁";
            case PICKUP_MATCHING:
                return "拾取";
            case DISABLED:
            default:
                return "关闭";
        }
    }
}

