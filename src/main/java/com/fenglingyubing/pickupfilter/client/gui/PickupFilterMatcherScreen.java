package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.config.FilterMode;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PickupFilterMatcherScreen extends GuiScreen {
    private static final int X_SIZE = 176;
    private static final int HEADER_H = 22;
    private static final int SLOT = 18;

    private static final int RULE_COLS = 9;
    private static final int RULE_ROWS = 3;
    private static final int RULES_PER_PAGE = RULE_COLS * RULE_ROWS;

    private static final int INV_COLS = 9;
    private static final int INV_MAIN_ROWS = 3;
    private static final int INV_HOTBAR_ROWS = 1;

    private static final int COLOR_DIM = 0xAA000000;
    private static final int COLOR_PANEL = 0xFFE7E6E3;
    private static final int COLOR_PANEL_INNER = 0xFFF4F3F1;
    private static final int COLOR_SHADOW = 0x33000000;
    private static final int COLOR_BORDER = 0xFF2E2B2A;
    private static final int COLOR_TEXT = 0xFF1A1716;
    private static final int COLOR_MUTED = 0xFF6F6A67;
    private static final int COLOR_ACCENT = 0xFF1E9E73;
    private static final int COLOR_SLOT_BG = 0xFFB9B5B0;
    private static final int COLOR_SLOT_EDGE = 0xFF7A7672;

    private final GuiScreen parent;

    private GuiButton applyButton;
    private GuiButton clearButton;
    private GuiButton backButton;
    private GuiButton openConfigButton;
    private GuiButton prevPageButton;
    private GuiButton nextPageButton;

    private String status = TextFormatting.DARK_GRAY + "同步中…";
    private int lastSnapshotRevision = -1;
    private boolean dirty;
    private FilterMode currentMode = FilterMode.DISABLED;

    private int guiLeft;
    private int guiTop;
    private int ySize;

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

        ySize = 222;
        guiLeft = (this.width - X_SIZE) / 2;
        guiTop = (this.height - ySize) / 2;

        rulesX = guiLeft + 7;
        rulesY = guiTop + HEADER_H + 10;

        invLabelY = rulesY + RULE_ROWS * SLOT + 12;
        invX = guiLeft + 7;
        invY = invLabelY + 10;
        hotbarY = invY + INV_MAIN_ROWS * SLOT + 4;

        int headerButtonY = guiTop + 2;
        backButton = addButton(new GuiButton(4, guiLeft + 6, headerButtonY, 18, 18, "<"));
        openConfigButton = addButton(new GuiButton(3, guiLeft + X_SIZE - 6 - 18, headerButtonY, 18, 18, "配"));
        clearButton = addButton(new GuiButton(2, guiLeft + X_SIZE - 6 - 18 * 2 - 2, headerButtonY, 18, 18, "清"));
        applyButton = addButton(new GuiButton(1, guiLeft + X_SIZE - 6 - 18 * 3 - 4, headerButtonY, 18, 18, "存"));

        int pagerY = rulesY - 12;
        prevPageButton = addButton(new GuiButton(5, guiLeft + X_SIZE - 6 - 14 * 2 - 2, pagerY, 14, 14, "<"));
        nextPageButton = addButton(new GuiButton(6, guiLeft + X_SIZE - 6 - 14, pagerY, 14, 14, ">"));

        ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
        if (snapshot != null && snapshot.getRules() != null && !snapshot.getRules().isEmpty()) {
            currentMode = snapshot.getMode() == null ? FilterMode.DISABLED : snapshot.getMode();
            loadFromSnapshot(snapshot);
        }
        requestSnapshot();
        lastSnapshotRevision = ClientConfigSnapshotStore.getRevision();
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
            List<FilterRule> toSend = composeRulesToSend();
            PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(toSend));
            status = TextFormatting.GRAY + "已发送配置更新（规则 " + toSend.size() + " 条）…";
            dirty = false;
            requestSnapshot();
            return;
        }

        if (button.id == 2) {
            itemRules.clear();
            pageIndex = 0;
            dirty = true;
            autoSave(TextFormatting.DARK_GRAY + "已清空物品列表（保留高级规则 " + hiddenRulesCount + " 条）");
            return;
        }

        if (button.id == 3) {
            mc.displayGuiScreen(new PickupFilterConfigScreen());
            return;
        }

        if (button.id == 4) {
            mc.displayGuiScreen(parent);
            return;
        }

        if (button.id == 5) {
            if (pageIndex > 0) {
                pageIndex--;
            }
            return;
        }

        if (button.id == 6) {
            int totalPages = getTotalPages();
            if (pageIndex + 1 < totalPages) {
                pageIndex++;
            }
        }
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
            int count = snapshot == null || snapshot.getRules() == null ? 0 : snapshot.getRules().size();
            status = TextFormatting.DARK_GRAY + "已同步：规则 " + count + " 条";
            if (!dirty && snapshot != null) {
                currentMode = snapshot.getMode() == null ? FilterMode.DISABLED : snapshot.getMode();
                loadFromSnapshot(snapshot);
            }
        }
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
        if (applyButton != null) {
            applyButton.enabled = dirty;
        }
        if (clearButton != null) {
            clearButton.enabled = !itemRules.isEmpty();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0 && isMouseOverRulesGrid()) {
            if (wheel < 0) {
                int totalPages = getTotalPages();
                if (pageIndex + 1 < totalPages) {
                    pageIndex++;
                }
            } else if (pageIndex > 0) {
                pageIndex--;
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
                    dirty = true;
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
        dirty = true;
        autoSave(TextFormatting.GRAY + "已添加："
                + TextFormatting.AQUA + registryName.getNamespace()
                + TextFormatting.GRAY + ":"
                + TextFormatting.AQUA + registryName.getPath()
                + TextFormatting.DARK_GRAY + " @"
                + TextFormatting.AQUA + stack.getMetadata()
                + TextFormatting.DARK_GRAY + "（右键上方格子可移除）");
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

        drawRect(guiLeft + 2, guiTop + 2, guiLeft + X_SIZE + 2, guiTop + ySize + 2, COLOR_SHADOW);
        drawRect(guiLeft, guiTop, guiLeft + X_SIZE, guiTop + ySize, COLOR_PANEL);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + X_SIZE - 1, guiTop + ySize - 1, COLOR_PANEL_INNER);
        drawRect(guiLeft, guiTop, guiLeft + X_SIZE, guiTop + 1, COLOR_BORDER);
        drawRect(guiLeft, guiTop + ySize - 1, guiLeft + X_SIZE, guiTop + ySize, COLOR_BORDER);
        drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, COLOR_BORDER);
        drawRect(guiLeft + X_SIZE - 1, guiTop, guiLeft + X_SIZE, guiTop + ySize, COLOR_BORDER);

        drawRect(guiLeft + 1, guiTop + 1, guiLeft + X_SIZE - 1, guiTop + HEADER_H, 0xFFF0EFED);
        drawRect(guiLeft + 1, guiTop + HEADER_H, guiLeft + X_SIZE - 1, guiTop + HEADER_H + 1, 0xFFCEC9C4);

        drawCenteredString(fontRenderer, TextFormatting.BOLD + "拾取筛", guiLeft + X_SIZE / 2, guiTop + 7, COLOR_TEXT);
        String modeLabel = TextFormatting.DARK_GRAY + "当前：" + TextFormatting.GRAY + getListNameForMode(currentMode);
        drawString(fontRenderer, modeLabel, guiLeft + 6, guiTop + 7, COLOR_MUTED);

        drawRulesGrid(mouseX, mouseY);
        drawInventory(mouseX, mouseY);

        String pageText = (pageIndex + 1) + "/" + getTotalPages();
        drawString(fontRenderer, TextFormatting.DARK_GRAY + pageText, guiLeft + X_SIZE - 6 - fontRenderer.getStringWidth(pageText) - 32, rulesY - 12, COLOR_MUTED);

        int hintY = guiTop + ySize - 12;
        String hint = hiddenRulesCount > 0
                ? TextFormatting.DARK_GRAY + "提示：左键从下方添加；右键移除；自动保存；未显示高级规则 " + hiddenRulesCount + " 条"
                : TextFormatting.DARK_GRAY + "提示：左键从下方添加；右键移除；自动保存";
        drawString(fontRenderer, hint, guiLeft + 6, hintY, COLOR_MUTED);
        drawString(fontRenderer, status == null ? "" : status, guiLeft + 6, hintY - 10, COLOR_MUTED);

        GlStateManager.disableLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawRulesGrid(int mouseX, int mouseY) {
        drawString(fontRenderer, TextFormatting.BOLD + "匹配列表", rulesX, rulesY - 12, COLOR_TEXT);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "（右键移除）", rulesX + 52, rulesY - 12, COLOR_MUTED);

        for (int slotIndex = 0; slotIndex < RULES_PER_PAGE; slotIndex++) {
            int col = slotIndex % RULE_COLS;
            int row = slotIndex / RULE_COLS;
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
                    String text = "?";
                    fontRenderer.drawString(text, x + 6, y + 6, COLOR_MUTED);
                }

                if (isPointInRect(x, y, SLOT, SLOT, mouseX, mouseY)) {
                    drawRect(x, y, x + SLOT, y + SLOT, 0x331E9E73);
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
        drawString(fontRenderer, TextFormatting.BOLD + "背包", invX, invLabelY, COLOR_TEXT);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "（左键添加到上方）", invX + 28, invLabelY, COLOR_MUTED);

        for (int displayIndex = 0; displayIndex < 36; displayIndex++) {
            int row;
            int col = displayIndex % INV_COLS;
            if (displayIndex < 27) {
                row = displayIndex / INV_COLS;
            } else {
                row = INV_MAIN_ROWS + (displayIndex - 27) / INV_COLS;
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
                drawRect(x, y, x + SLOT, y + SLOT, 0x331E9E73);
                if (stack != null && !stack.isEmpty()) {
                    renderToolTip(stack, mouseX, mouseY);
                }
            }
        }

        drawRect(invX, hotbarY - 3, invX + INV_COLS * SLOT, hotbarY - 2, 0x33000000);
    }

    private int getInventoryDisplayIndexAt(int mouseX, int mouseY) {
        int gridW = INV_COLS * SLOT;
        int mainH = INV_MAIN_ROWS * SLOT;
        int totalH = mainH + 4 + INV_HOTBAR_ROWS * SLOT;

        if (mouseX < invX || mouseX >= invX + gridW) {
            return -1;
        }
        if (mouseY < invY || mouseY >= invY + totalH) {
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
            return row * INV_COLS + col;
        }

        int relHotbarY = mouseY - hotbarY;
        int row = relHotbarY / SLOT;
        if (row < 0 || row >= INV_HOTBAR_ROWS) {
            return -1;
        }
        return 27 + row * INV_COLS + col;
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

    private void loadFromSnapshot(ClientConfigSnapshotStore.Snapshot snapshot) {
        hiddenRules.clear();
        itemRules.clear();
        hiddenRulesCount = 0;

        List<FilterRule> rules = snapshot == null ? null : snapshot.getRules();
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
        PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(toSend));
        dirty = false;
        requestSnapshot();
    }

    private static String getListNameForMode(FilterMode mode) {
        if (mode == null) {
            return "拾取匹配列表";
        }
        switch (mode) {
            case DESTROY_MATCHING:
                return "销毁匹配列表";
            case PICKUP_MATCHING:
                return "拾取匹配列表";
            case DISABLED:
            default:
                return "拾取匹配列表（当前模式：关闭）";
        }
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

    private int getRuleSlotAt(int mouseX, int mouseY) {
        if (!isPointInRect(rulesX, rulesY, RULE_COLS * SLOT, RULE_ROWS * SLOT, mouseX, mouseY)) {
            return -1;
        }
        int relX = mouseX - rulesX;
        int relY = mouseY - rulesY;
        int col = relX / SLOT;
        int row = relY / SLOT;
        int index = row * RULE_COLS + col;
        return index < 0 || index >= RULES_PER_PAGE ? -1 : index;
    }

    private boolean isMouseOverRulesGrid() {
        int mx = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        return isPointInRect(rulesX, rulesY, RULE_COLS * SLOT, RULE_ROWS * SLOT, mx, my);
    }

    private void drawSlot(int x, int y) {
        drawRect(x, y, x + SLOT, y + SLOT, COLOR_SLOT_BG);
        drawRect(x, y, x + SLOT, y + 1, COLOR_SLOT_EDGE);
        drawRect(x, y, x + 1, y + SLOT, COLOR_SLOT_EDGE);
        drawRect(x + SLOT - 1, y, x + SLOT, y + SLOT, 0xFF9E9993);
        drawRect(x, y + SLOT - 1, x + SLOT, y + SLOT, 0xFF9E9993);
        drawRect(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x25FFFFFF);
    }

    private static boolean isPointInRect(int x, int y, int w, int h, int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private int getTotalPages() {
        int total = Math.max(1, (itemRules.size() + RULES_PER_PAGE - 1) / RULES_PER_PAGE);
        return total;
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
}
