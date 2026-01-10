package com.fenglingyubing.pickupfilter.client.gui;

import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.network.ClientConfigSnapshotStore;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import com.fenglingyubing.pickupfilter.network.RequestConfigSnapshotPacket;
import com.fenglingyubing.pickupfilter.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PickupFilterMatcherScreen extends GuiScreen {
    private static final int COLOR_BG = 0xFF05070B;
    private static final int COLOR_PANEL = 0xE012171C;
    private static final int COLOR_ACCENT = 0xFF67F3A4;
    private static final int COLOR_MUTED = 0xFF9AA7B3;
    private static final int COLOR_TEXT = 0xFFE7EEF5;
    private static final int COLOR_SLOT = 0xFF0E1217;
    private static final int COLOR_SLOT_EDGE = 0xFF1E2A36;

    private final GuiScreen parent;
    private final Map<ItemKey, ItemStack> selected = new LinkedHashMap<>();

    private SelectedListSlot selectedListSlot;
    private GuiButton applyButton;
    private GuiButton clearButton;
    private GuiButton backButton;
    private GuiButton openConfigButton;

    private String status = TextFormatting.DARK_GRAY + "同步中…";
    private int lastSnapshotRevision = -1;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int listX;
    private int listY;
    private int listW;

    private int invX;
    private int invY;
    private int invCell = 20;
    private int invCols = 9;
    private int invMainRows = 3;
    private int invHotbarRows = 1;

    public PickupFilterMatcherScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        panelW = Math.min(560, this.width - 30);
        panelX = (this.width - panelW) / 2;
        panelY = 18;
        panelH = this.height - 36;

        int leftW = Math.min(240, panelW - 220);
        listX = panelX + leftW + 16;
        listW = panelX + panelW - listX - 14;

        invX = panelX + 14;
        invY = panelY + 62;

        listY = panelY + 62;
        int listH = panelH - 140;
        selectedListSlot = new SelectedListSlot(listX, listY, listW, listH);

        int buttonsY = panelY + panelH - 46;
        int gap = 6;
        int buttonW = (panelW - 28 - gap * 3) / 4;

        applyButton = addButton(new GuiButton(1, panelX + 14, buttonsY, buttonW, 20, "应用到拾取筛"));
        clearButton = addButton(new GuiButton(2, panelX + 14 + (buttonW + gap), buttonsY, buttonW, 20, "清空选择"));
        openConfigButton = addButton(new GuiButton(3, panelX + 14 + (buttonW + gap) * 2, buttonsY, buttonW, 20, "打开配置"));
        backButton = addButton(new GuiButton(4, panelX + 14 + (buttonW + gap) * 3, buttonsY, buttonW, 20, "返回背包"));

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
            if (selected.isEmpty()) {
                status = TextFormatting.RED + "还没有选择物品";
                return;
            }
            ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
            List<FilterRule> merged = mergeRules(snapshot == null ? null : snapshot.getRules(), toFilterRules());
            PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(merged));
            status = TextFormatting.GRAY + "已发送配置更新（规则 " + merged.size() + " 条）…";
            requestSnapshot();
            return;
        }

        if (button.id == 2) {
            selected.clear();
            if (selectedListSlot != null) {
                selectedListSlot.setSelectedIndex(-1);
            }
            status = TextFormatting.DARK_GRAY + "已清空选择";
            return;
        }

        if (button.id == 3) {
            mc.displayGuiScreen(new PickupFilterConfigScreen());
            return;
        }

        if (button.id == 4) {
            mc.displayGuiScreen(parent);
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
        }
        if (applyButton != null) {
            applyButton.enabled = !selected.isEmpty();
        }
        if (clearButton != null) {
            clearButton.enabled = !selected.isEmpty();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (selectedListSlot != null) {
            selectedListSlot.handleMouseInput();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }

        int index = getInventoryDisplayIndexAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }

        ItemStack stack = getDisplayedInventoryStack(index);
        if (stack == null || stack.isEmpty()) {
            status = TextFormatting.DARK_GRAY + "该格子没有物品";
            return;
        }
        Item item = stack.getItem();
        if (item == null) {
            status = TextFormatting.RED + "无法读取物品";
            return;
        }
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) {
            status = TextFormatting.RED + "无法读取物品注册名";
            return;
        }

        ItemKey key = new ItemKey(registryName, stack.getMetadata());
        if (selected.containsKey(key)) {
            status = TextFormatting.DARK_GRAY + "已存在于选择列表（双击右侧可删除）";
            return;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        selected.put(key, copy);
        status = TextFormatting.GRAY + "已添加："
                + TextFormatting.AQUA + registryName.getNamespace()
                + TextFormatting.GRAY + ":"
                + TextFormatting.AQUA + registryName.getPath()
                + TextFormatting.DARK_GRAY + " @"
                + TextFormatting.AQUA + stack.getMetadata();
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
        drawRect(0, 0, width, height, COLOR_BG);

        drawGradientRect(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL, 0xE0080A0D);
        drawRect(panelX, panelY, panelX + panelW, panelY + 1, COLOR_ACCENT);

        drawCenteredString(fontRenderer, TextFormatting.BOLD + "拾取筛 · 物品匹配", this.width / 2, panelY + 10, COLOR_ACCENT);
        drawCenteredString(fontRenderer, TextFormatting.DARK_GRAY + "左侧点击背包物品 → 右侧列表；右侧双击删除；最后点“应用到拾取筛”", this.width / 2, panelY + 24, COLOR_MUTED);

        drawInventoryGrid(mouseX, mouseY);

        drawString(fontRenderer, TextFormatting.BOLD + "匹配列表", listX, listY - 14, COLOR_TEXT);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "（双击删除）", listX + 52, listY - 14, COLOR_MUTED);

        if (selectedListSlot != null) {
            selectedListSlot.drawScreen(mouseX, mouseY, partialTicks);
        }

        drawString(fontRenderer, TextFormatting.GRAY + "选择：" + TextFormatting.AQUA + selected.size() + TextFormatting.GRAY + " 项", panelX + 14, panelY + panelH - 86, COLOR_TEXT);
        drawString(fontRenderer, status == null ? "" : status, panelX + 14, panelY + panelH - 98, COLOR_MUTED);

        GlStateManager.disableLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawInventoryGrid(int mouseX, int mouseY) {
        int gridW = invCols * invCell;
        int mainH = invMainRows * invCell;
        int hotbarY = invY + mainH + 12;

        drawString(fontRenderer, TextFormatting.BOLD + "背包", invX, invY - 14, COLOR_TEXT);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "（点击图标即可添加）", invX + 34, invY - 14, COLOR_MUTED);

        for (int displayIndex = 0; displayIndex < 36; displayIndex++) {
            int row;
            int col = displayIndex % invCols;
            if (displayIndex < 27) {
                row = displayIndex / invCols;
            } else {
                row = invMainRows + (displayIndex - 27) / invCols;
            }

            int x = invX + col * invCell;
            int y = invY + row * invCell + (displayIndex >= 27 ? 12 : 0);

            drawRect(x, y, x + invCell - 2, y + invCell - 2, COLOR_SLOT);
            drawRect(x, y, x + invCell - 2, y + 1, COLOR_SLOT_EDGE);
            drawRect(x, y, x + 1, y + invCell - 2, COLOR_SLOT_EDGE);
            drawRect(x + invCell - 3, y, x + invCell - 2, y + invCell - 2, 0xFF0A0D11);
            drawRect(x, y + invCell - 3, x + invCell - 2, y + invCell - 2, 0xFF0A0D11);

            ItemStack stack = getDisplayedInventoryStack(displayIndex);
            if (stack != null && !stack.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                itemRender.renderItemAndEffectIntoGUI(stack, x + 1, y + 1);
                itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x + 1, y + 1, null);
                RenderHelper.disableStandardItemLighting();
            }

            boolean hovered = mouseX >= x && mouseX < x + invCell - 2 && mouseY >= y && mouseY < y + invCell - 2;
            if (hovered) {
                drawRect(x, y, x + invCell - 2, y + invCell - 2, 0x331BE6A3);
                if (stack != null && !stack.isEmpty()) {
                    renderToolTip(stack, mouseX, mouseY);
                }
            }
        }

        drawRect(invX, hotbarY - 7, invX + gridW - 2, hotbarY - 6, 0x66000000);
        drawString(fontRenderer, TextFormatting.DARK_GRAY + "快捷栏", invX, hotbarY - 14, COLOR_MUTED);
    }

    private int getInventoryDisplayIndexAt(int mouseX, int mouseY) {
        int gridW = invCols * invCell;
        int mainH = invMainRows * invCell;
        int hotbarY = invY + mainH + 12;
        int totalH = mainH + 12 + invHotbarRows * invCell;

        if (mouseX < invX || mouseX >= invX + gridW - 2) {
            return -1;
        }
        if (mouseY < invY || mouseY >= invY + totalH - 2) {
            return -1;
        }

        int relX = mouseX - invX;
        int col = relX / invCell;

        if (mouseY < hotbarY) {
            int relY = mouseY - invY;
            int row = relY / invCell;
            if (row < 0 || row >= invMainRows) {
                return -1;
            }
            return row * invCols + col;
        }

        int relHotbarY = mouseY - hotbarY;
        int row = relHotbarY / invCell;
        if (row < 0 || row >= invHotbarRows) {
            return -1;
        }
        return 27 + row * invCols + col;
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

    private List<FilterRule> toFilterRules() {
        List<FilterRule> rules = new ArrayList<>();
        for (ItemKey key : selected.keySet()) {
            if (key == null || key.registryName == null) {
                continue;
            }
            rules.add(new FilterRule(key.registryName.getNamespace(), key.registryName.getPath(), key.meta, false));
        }
        return rules;
    }

    private static List<FilterRule> mergeRules(List<FilterRule> existing, List<FilterRule> adding) {
        List<FilterRule> merged = new ArrayList<>();
        if (existing != null) {
            for (FilterRule rule : existing) {
                if (rule != null && !merged.contains(rule)) {
                    merged.add(rule);
                }
            }
        }
        if (adding != null) {
            for (FilterRule rule : adding) {
                if (rule != null && !merged.contains(rule)) {
                    merged.add(rule);
                }
            }
        }
        if (merged.size() > 200) {
            merged = merged.subList(0, 200);
        }
        return merged;
    }

    private final class SelectedListSlot extends GuiSlot {
        private final int x;
        private final int w;
        private int selectedIndex = -1;

        SelectedListSlot(int x, int y, int width, int height) {
            super(PickupFilterMatcherScreen.this.mc, width, height, y, y + height, 22);
            this.x = x;
            this.w = width;
        }

        void setSelectedIndex(int index) {
            selectedIndex = index;
        }

        @Override
        protected int getSize() {
            return selected.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            selectedIndex = index;
            if (doubleClick) {
                removeSelected();
            }
        }

        private void removeSelected() {
            if (selectedIndex < 0 || selectedIndex >= selected.size()) {
                status = TextFormatting.RED + "未选择条目";
                return;
            }
            ItemKey key = new ArrayList<>(selected.keySet()).get(selectedIndex);
            selected.remove(key);
            if (selectedIndex >= selected.size()) {
                selectedIndex = selected.size() - 1;
            }
            status = TextFormatting.DARK_GRAY + "已删除：双击可继续删除";
        }

        @Override
        protected boolean isSelected(int index) {
            return index == selectedIndex;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSlot(int entryId, int insideLeft, int yPos, int insideSlotHeight, int mouseXIn, int mouseYIn, float partialTicks) {
            List<Map.Entry<ItemKey, ItemStack>> entries = new ArrayList<>(selected.entrySet());
            if (entryId < 0 || entryId >= entries.size()) {
                return;
            }

            Map.Entry<ItemKey, ItemStack> entry = entries.get(entryId);
            ItemKey key = entry.getKey();
            ItemStack stack = entry.getValue();

            int bg = isSelected(entryId) ? 0x221BE6A3 : 0x14000000;
            drawRect(x, yPos, x + w, yPos + insideSlotHeight - 2, bg);
            drawRect(x, yPos + insideSlotHeight - 3, x + w, yPos + insideSlotHeight - 2, 0x20000000);

            if (stack != null && !stack.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                itemRender.renderItemAndEffectIntoGUI(stack, x + 4, yPos + 2);
                itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x + 4, yPos + 2, null);
                RenderHelper.disableStandardItemLighting();
            }

            String text = key == null ? "" : (key.registryName.getNamespace() + ":" + key.registryName.getPath() + " @" + key.meta);
            int color = isSelected(entryId) ? COLOR_ACCENT : COLOR_TEXT;
            fontRenderer.drawString(text, x + 26, yPos + 7, color);
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

    private static final class ItemKey {
        private final ResourceLocation registryName;
        private final int meta;

        ItemKey(ResourceLocation registryName, int meta) {
            this.registryName = registryName;
            this.meta = meta;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ItemKey)) {
                return false;
            }
            ItemKey other = (ItemKey) o;
            if (meta != other.meta) {
                return false;
            }
            return registryName != null && registryName.equals(other.registryName);
        }

        @Override
        public int hashCode() {
            int result = registryName == null ? 0 : registryName.hashCode();
            result = 31 * result + meta;
            return result;
        }
    }
}
