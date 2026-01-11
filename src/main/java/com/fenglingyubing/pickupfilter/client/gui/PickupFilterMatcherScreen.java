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

    private static final ResourceLocation CHEST_GUI = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int GUI_W = 176;
    private static final int GUI_H = 114 + GRID_ROWS * SLOT;
    private static final int GUI_TOP_H = GRID_ROWS * SLOT + 17;
    private static final int GUI_TEXT = 0x404040;
    private static final int GUI_TEXT_MUTED = 0x606060;

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
    private int panelW = GUI_W;
    private int panelH = GUI_H;

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

        panelW = GUI_W;
        panelH = GUI_H;

        int actionsH = 20 * 2 + 6;
        int statusH = 26;
        int totalH = panelH + 10 + actionsH + 8 + statusH;

        panelX = (this.width - panelW) / 2;
        panelY = (this.height - totalH) / 2;
        if (panelY < 10) {
            panelY = 10;
        }

        rulesX = panelX + 8;
        rulesY = panelY + 18;
        rulesLabelY = panelY + 6;

        invX = panelX + 8;
        invLabelY = panelY + panelH - 96 + 2;
        invY = invLabelY + 11;
        hotbarY = invY + INV_MAIN_ROWS * SLOT + 4;

        int tabY = Math.max(4, panelY - 24);
        int tabX = panelX + 8;
        int tabsGap = 4;
        int tabsToPagerGap = 4;
        int pagerBlockW = 20 * 2;
        int tabsAvailableW = panelW - 8 - pagerBlockW - tabsToPagerGap;
        int tabW = (tabsAvailableW - tabsGap) / 2;
        tabPickupButton = addButton(new GuiButton(21, tabX, tabY, tabW, 20, "拾取列表"));
        tabDestroyButton = addButton(new GuiButton(22, tabX + tabW + tabsGap, tabY, tabW, 20, "销毁列表"));

        int pagerY = tabY;
        nextPageButton = addButton(new GuiButton(24, panelX + panelW - 20, pagerY, 20, 20, ">"));
        prevPageButton = addButton(new GuiButton(23, panelX + panelW - 40, pagerY, 20, 20, "<"));

        int actionsY = panelY + panelH + 10;
        int gap = 6;
        int w = (panelW - gap) / 2;
        applyButton = addButton(new GuiButton(31, panelX, actionsY, w, 20, "同步到服务器"));
        clearButton = addButton(new GuiButton(32, panelX + w + gap, actionsY, w, 20, "清空列表"));
        openConfigButton = addButton(new GuiButton(33, panelX, actionsY + 26, w, 20, "打开配置"));
        backButton = addButton(new GuiButton(34, panelX + w + gap, actionsY + 26, w, 20, "返回背包"));

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

        FilterRule rule = FilterRule.fromItemStack(stack);
        if (rule == null) {
            status = TextFormatting.RED + "无法生成规则";
            return;
        }
        if (!canDisplayRule(rule)) {
            status = TextFormatting.RED + "该物品无法添加（注册名异常）";
            return;
        }

        if (containsEquivalentRule(itemRules, rule, stack) || containsEquivalentRule(hiddenRules, rule, stack)) {
            status = TextFormatting.DARK_GRAY + "已在列表中";
            return;
        }

        if (itemRules.size() >= 200) {
            status = TextFormatting.RED + "规则已达上限（200）";
            return;
        }

        itemRules.add(rule);
        String metaText = rule.getMetadata() == FilterRule.ANY_METADATA ? "*" : Integer.toString(rule.getMetadata());
        autoSave(TextFormatting.GRAY + "已添加："
                + TextFormatting.AQUA + registryName.getNamespace()
                + TextFormatting.GRAY + ":"
                + TextFormatting.AQUA + registryName.getPath()
                + TextFormatting.DARK_GRAY + " @"
                + TextFormatting.AQUA + metaText
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

        drawPanel();
        drawHeader();
        drawRulesGrid(mouseX, mouseY);
        drawInventory(mouseX, mouseY);
        drawFooter();

        GlStateManager.disableLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawHoveredTooltip(mouseX, mouseY);
    }

    private void drawPanel() {
        mc.getTextureManager().bindTexture(CHEST_GUI);
        drawTexturedModalRect(panelX, panelY, 0, 0, panelW, GUI_TOP_H);
        drawTexturedModalRect(panelX, panelY + GUI_TOP_H, 0, 126, panelW, 96);
    }

    private void drawHeader() {
        String title = "拾取筛";
        drawCenteredString(fontRenderer, title, panelX + panelW / 2, panelY + 6, GUI_TEXT);
        String invLabel = "背包";
        drawString(fontRenderer, invLabel, invX, invLabelY, GUI_TEXT);
    }

    private void drawFooter() {
        int footerY = panelY + panelH + 10 + 20 * 2 + 6 + 8;

        String hint = TextFormatting.DARK_GRAY + "提示：左键从背包添加；右键从列表移除；自动保存";
        if (hiddenRulesCount > 0) {
            hint += TextFormatting.DARK_GRAY + "；未显示高级规则 " + hiddenRulesCount + " 条";
        }

        String pageText = (pageIndex + 1) + "/" + getTotalPages();
        int pageX = prevPageButton == null ? panelX + panelW - 34 : prevPageButton.x - fontRenderer.getStringWidth(pageText) - 4;
        drawString(fontRenderer, pageText, pageX, panelY + 6, GUI_TEXT_MUTED);

        drawString(fontRenderer, status == null ? "" : status, panelX, footerY, GUI_TEXT_MUTED);
        drawString(fontRenderer, hint, panelX, footerY + 12, GUI_TEXT_MUTED);
    }

    private void drawRulesGrid(int mouseX, int mouseY) {
        for (int slotIndex = 0; slotIndex < RULES_PER_PAGE; slotIndex++) {
            int col = slotIndex % GRID_COLS;
            int row = slotIndex / GRID_COLS;
            int x = rulesX + col * SLOT;
            int y = rulesY + row * SLOT;

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
                    fontRenderer.drawString("?", x + 6, y + 6, GUI_TEXT_MUTED);
                }

                if (isPointInRect(x, y, SLOT, SLOT, mouseX, mouseY)) {
                    drawSlotHighlight(x, y);
                }
            }
        }
    }

    private void drawInventory(int mouseX, int mouseY) {
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

            ItemStack stack = getDisplayedInventoryStack(displayIndex);
            if (stack != null && !stack.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                itemRender.renderItemAndEffectIntoGUI(stack, x + 1, y + 1);
                itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x + 1, y + 1, null);
                RenderHelper.disableStandardItemLighting();
            }

            if (isPointInRect(x, y, SLOT, SLOT, mouseX, mouseY)) {
                drawSlotHighlight(x, y);
            }
        }
    }

    private void drawSlotHighlight(int x, int y) {
        int left = x;
        int top = y;
        int right = x + SLOT;
        int bottom = y + SLOT;
        drawGradientRect(left, top, right, bottom, 0x30FFFFFF, 0x18FFFFFF);
        drawRect(left, top, right, top + 1, 0x60FFE8A0);
        drawRect(left, bottom - 1, right, bottom, 0x60FFE8A0);
        drawRect(left, top, left + 1, bottom, 0x60FFE8A0);
        drawRect(right - 1, top, right, bottom, 0x60FFE8A0);
    }

    private void drawHoveredTooltip(int mouseX, int mouseY) {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();

        int ruleSlot = getRuleSlotAt(mouseX, mouseY);
        if (ruleSlot >= 0) {
            int index = pageIndex * RULES_PER_PAGE + ruleSlot;
            if (index >= 0 && index < itemRules.size()) {
                FilterRule rule = itemRules.get(index);
                ItemStack icon = createIcon(rule);
                if (icon != null && !icon.isEmpty()) {
                    renderToolTip(icon, mouseX, mouseY);
                } else if (rule != null) {
                    List<String> lines = new ArrayList<>();
                    lines.add(TextFormatting.GRAY + "规则：" + TextFormatting.AQUA + rule.serialize());
                    lines.add(TextFormatting.DARK_GRAY + "右键移除");
                    drawHoveringText(lines, mouseX, mouseY);
                }
                return;
            }
        }

        int invSlot = getInventoryDisplayIndexAt(mouseX, mouseY);
        if (invSlot >= 0) {
            ItemStack stack = getDisplayedInventoryStack(invSlot);
            if (stack != null && !stack.isEmpty()) {
                renderToolTip(stack, mouseX, mouseY);
            }
        }
    }

    private static boolean containsEquivalentRule(List<FilterRule> rules, FilterRule candidate, ItemStack stack) {
        if (rules == null || rules.isEmpty() || candidate == null || stack == null || stack.isEmpty() || stack.getItem() == null) {
            return false;
        }
        if (rules.contains(candidate)) {
            return true;
        }
        if (stack.getItem().getHasSubtypes() || !stack.isItemStackDamageable()) {
            return false;
        }
        for (FilterRule existing : rules) {
            if (existing == null || existing.isUseWildcard()) {
                continue;
            }
            if (candidate.getModId().equals(existing.getModId()) && candidate.getItemName().equals(existing.getItemName())) {
                return true;
            }
        }
        return false;
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
