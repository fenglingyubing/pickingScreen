package com.fenglingyubing.pickupfilter.client.event;

import com.fenglingyubing.pickupfilter.client.PickupFilterClient;
import com.fenglingyubing.pickupfilter.client.gui.PickupFilterMatcherScreen;
import com.fenglingyubing.pickupfilter.client.settings.ClientSettings;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.network.ClientConfigSnapshotStore;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import com.fenglingyubing.pickupfilter.network.RequestConfigSnapshotPacket;
import com.fenglingyubing.pickupfilter.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class InventoryGuiButtonHandler {
    private static final int BUTTON_ID = 0x50464D; // "PFM"

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (!(gui instanceof GuiInventory)) {
            return;
        }

        List<GuiButton> buttons = event.getButtonList();
        for (GuiButton button : buttons) {
            if (button != null && button.id == BUTTON_ID) {
                return;
            }
        }

        int xSize = 176;
        int ySize = 166;
        if (gui instanceof GuiContainer) {
            int[] size = readGuiContainerSize((GuiContainer) gui);
            if (size != null && size.length == 2) {
                xSize = size[0];
                ySize = size[1];
            }
        }

        int guiLeft = (gui.width - xSize) / 2;
        int guiTop = (gui.height - ySize) / 2;

        int targetX = guiLeft + xSize - 18 - 4;
        int targetY = guiTop + ySize - 18 - 4 - 18;

        boolean placedInside = isPointInRect(guiLeft, guiTop, xSize, ySize, targetX, targetY) && targetY >= guiTop + 4;
        if (placedInside) {
            while (overlapsAny(buttons, targetX, targetY, 18, 18) && targetY > guiTop + 4) {
                targetY -= 20;
            }
            placedInside = !overlapsAny(buttons, targetX, targetY, 18, 18) && targetY >= guiTop + 4;
        }

        if (!placedInside) {
            boolean hasRightSpace = guiLeft + xSize + 4 + 18 <= gui.width - 4;
            boolean hasBottomSpace = guiTop + ySize + 4 + 18 <= gui.height - 4;

            if (hasRightSpace) {
                targetX = guiLeft + xSize + 4;
                targetY = guiTop + 18;
            } else if (hasBottomSpace) {
                targetX = guiLeft + xSize - 18;
                targetY = guiTop + ySize + 4;
            } else {
                targetX = guiLeft + xSize - 22;
                targetY = guiTop + ySize - 22;
                GuiButton lowestRightButton = findLowestRightSmallButton(buttons, guiLeft, guiTop, xSize, ySize);
                if (lowestRightButton != null) {
                    targetX = getButtonX(lowestRightButton);
                    targetY = getButtonY(lowestRightButton) + lowestRightButton.height + 6;
                    targetY = Math.min(targetY, gui.height - 22);
                }
            }
        }

        ClientSettings settings = PickupFilterClient.getClientSettings();
        int offsetX = settings == null ? 0 : settings.getInventoryButtonOffsetX();
        int offsetY = settings == null ? 0 : settings.getInventoryButtonOffsetY();
        targetX += offsetX;
        targetY += offsetY;

        targetX = clamp(targetX, 4, gui.width - 18 - 4);
        targetY = clamp(targetY, 4, gui.height - 18 - 4);

        buttons.add(new GuiButton(BUTTON_ID, targetX, targetY, 18, 18, "筛"));
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        GuiScreen gui = event.getGui();
        GuiButton button = event.getButton();
        if (!(gui instanceof GuiInventory)) {
            return;
        }
        if (button == null || button.id != BUTTON_ID) {
            return;
        }

        Minecraft.getMinecraft().displayGuiScreen(new PickupFilterMatcherScreen(gui));
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (!(gui instanceof GuiInventory)) {
            return;
        }
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) {
            return;
        }
        if (Keyboard.getEventKey() != Keyboard.KEY_Z) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }

        ItemStack hovered = getHoveredStack(gui);
        if (hovered == null || hovered.isEmpty() || hovered.getItem() == null) {
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_GRAY + "未指向物品格子"), true);
            return;
        }

        if (!ClientConfigSnapshotStore.hasReceivedSnapshot()) {
            PickupFilterNetwork.CHANNEL.sendToServer(new RequestConfigSnapshotPacket());
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_GRAY + "配置同步中…稍后再按 Z"), true);
            return;
        }

        Item item = hovered.getItem();
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) {
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "无法读取物品注册名"), true);
            return;
        }

        FilterRule rule = FilterRule.fromItemStack(hovered);
        if (rule == null) {
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "无法生成规则"), true);
            return;
        }
        ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
        FilterMode activeMode = snapshot == null ? FilterMode.DISABLED : snapshot.getMode();
        List<FilterRule> existing = snapshot == null ? null : snapshot.getRulesForMode(activeMode);
        boolean alreadyExists = existing != null && containsEquivalentRule(existing, rule, hovered);
        if (alreadyExists) {
            String metaText = rule.getMetadata() == FilterRule.ANY_METADATA ? "*" : Integer.toString(rule.getMetadata());
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_GRAY + "已在规则中："
                    + TextFormatting.AQUA + registryName.getNamespace()
                    + TextFormatting.DARK_GRAY + ":"
                    + TextFormatting.AQUA + registryName.getPath()
                    + TextFormatting.DARK_GRAY + " @"
                    + TextFormatting.AQUA + metaText), true);
            return;
        }

        List<FilterRule> merged = mergeRules(existing, rule);

        ClientConfigSnapshotStore.applyLocalRulesForMode(activeMode, merged);
        PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(activeMode, merged));
        String listName = activeMode == FilterMode.DESTROY_MATCHING ? "销毁列表" : "拾取列表";
        String metaText = rule.getMetadata() == FilterRule.ANY_METADATA ? "*" : Integer.toString(rule.getMetadata());
        mc.player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "已添加到" + listName + "："
                + TextFormatting.AQUA + registryName.getNamespace()
                + TextFormatting.GRAY + ":"
                + TextFormatting.AQUA + registryName.getPath()
                + TextFormatting.DARK_GRAY + " @"
                + TextFormatting.AQUA + metaText
                + TextFormatting.DARK_GRAY + "（Z 快捷）"), true);
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

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (!(gui instanceof GuiInventory)) {
            return;
        }

        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        int xSize = 176;
        int ySize = 166;
        if (gui instanceof GuiContainer) {
            int[] size = readGuiContainerSize((GuiContainer) gui);
            if (size != null && size.length == 2) {
                xSize = size[0];
                ySize = size[1];
            }
        }
        int guiLeft = (gui.width - xSize) / 2;
        int guiTop = (gui.height - ySize) / 2;

        ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
        FilterMode mode = snapshot == null ? FilterMode.DISABLED : snapshot.getMode();
        int pickupCount = snapshot == null || snapshot.getPickupRules() == null ? 0 : snapshot.getPickupRules().size();
        int destroyCount = snapshot == null || snapshot.getDestroyRules() == null ? 0 : snapshot.getDestroyRules().size();

        int boxW = 110;
        int boxH = 28;
        int[] tooltipRect = getTooltipRect(gui, mouseX, mouseY, getHoveredStack(gui));
        int[] pos = findOverlayPosition(gui, guiLeft, guiTop, xSize, ySize, boxW, boxH, tooltipRect);
        if (pos == null || pos.length != 2) {
            return;
        }
        int x = pos[0];
        int y = pos[1];

        int bg = 0x88000000;
        gui.drawRect(x, y, x + boxW, y + boxH, bg);
        String line1 = TextFormatting.GREEN + "拾取筛" + TextFormatting.GRAY + "：" + TextFormatting.AQUA + getModeNameChinese(mode);
        String line2 = TextFormatting.DARK_GRAY + "拾取" + TextFormatting.GRAY + pickupCount
                + TextFormatting.DARK_GRAY + " / 销毁" + TextFormatting.GRAY + destroyCount;
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(line1, x + 4, y + 4, 0xFFFFFF);
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(line2, x + 4, y + 4 + 12, 0xFFFFFF);
    }

    private static int[] getTooltipRect(GuiScreen gui, int mouseX, int mouseY, ItemStack hoveredStack) {
        if (gui == null || hoveredStack == null || hoveredStack.isEmpty()) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.fontRenderer == null || mc.gameSettings == null) {
            return null;
        }
        ITooltipFlag flag = mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL;
        List<String> lines = hoveredStack.getTooltip(mc.player, flag);
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        int maxWidth = 0;
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            maxWidth = Math.max(maxWidth, mc.fontRenderer.getStringWidth(line));
        }

        int height = lines.size() == 1 ? 8 : 8 + (lines.size() - 1) * 10;
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + maxWidth > gui.width) {
            x = mouseX - 28 - maxWidth;
        }
        if (y + height + 6 > gui.height) {
            y = gui.height - height - 6;
        }
        if (y < 4) {
            y = 4;
        }

        int padL = 4;
        int padT = 6;
        int padR = 4;
        int padB = 6;
        return new int[]{x - padL, y - padT, maxWidth + padL + padR, height + padT + padB};
    }

    private static int[] findOverlayPosition(
            GuiScreen gui,
            int guiLeft,
            int guiTop,
            int xSize,
            int ySize,
            int boxW,
            int boxH,
            int[] tooltipRect
    ) {
        if (gui == null) {
            return null;
        }

        int[][] candidates = new int[][]{
                // Prefer outside: right side, then bottom.
                new int[]{guiLeft + xSize + 6, guiTop + 30},
                new int[]{guiLeft + 6, guiTop + ySize + 6},
                // Alternate outside: left side / above.
                new int[]{guiLeft - 6 - boxW, guiTop + 30},
                new int[]{guiLeft + 6, guiTop - 6 - boxH},
                // Fallback: inside corners.
                new int[]{guiLeft + 6, guiTop + 6},
                new int[]{guiLeft + xSize - boxW - 6, guiTop + 6},
                new int[]{guiLeft + 6, guiTop + ySize - boxH - 6},
                new int[]{guiLeft + xSize - boxW - 6, guiTop + ySize - boxH - 6},
        };

        for (int[] candidate : candidates) {
            if (candidate == null || candidate.length != 2) {
                continue;
            }
            int x = clamp(candidate[0], 4, gui.width - boxW - 4);
            int y = clamp(candidate[1], 4, gui.height - boxH - 4);
            if (tooltipRect != null && tooltipRect.length == 4 && rectIntersects(x, y, boxW, boxH, tooltipRect[0], tooltipRect[1], tooltipRect[2], tooltipRect[3])) {
                continue;
            }
            return new int[]{x, y};
        }

        return tooltipRect == null ? new int[]{clamp(guiLeft + xSize + 6, 4, gui.width - boxW - 4), clamp(guiTop + 30, 4, gui.height - boxH - 4)} : null;
    }

    private static boolean rectIntersects(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
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

    private static boolean overlapsAny(List<GuiButton> buttons, int x, int y, int w, int h) {
        if (buttons == null) {
            return false;
        }
        for (GuiButton button : buttons) {
            if (button == null) {
                continue;
            }
            int bx = getButtonX(button);
            int by = getButtonY(button);
            if (x < bx + button.width && x + w > bx && y < by + button.height && y + h > by) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPointInRect(int x, int y, int w, int h, int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ItemStack getHoveredStack(GuiScreen gui) {
        if (!(gui instanceof GuiContainer)) {
            return ItemStack.EMPTY;
        }

        Slot hovered = null;
        try {
            Field field = ReflectionHelper.findField(GuiContainer.class, "hoveredSlot", "theSlot", "field_147006_u");
            Object value = field.get(gui);
            if (value instanceof Slot) {
                hovered = (Slot) value;
            }
        } catch (Exception ignored) {
            hovered = null;
        }

        if (hovered == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = hovered.getStack();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private static GuiButton findLowestRightSmallButton(List<GuiButton> buttons, int guiLeft, int guiTop, int xSize, int ySize) {
        if (buttons == null) {
            return null;
        }
        int rightEdge = guiLeft + xSize;
        int leftBound = rightEdge - 34;
        int topBound = guiTop - 8;
        int bottomBound = guiTop + ySize + 60;

        GuiButton best = null;
        for (GuiButton button : buttons) {
            if (button == null) {
                continue;
            }
            if (button.width != 18 || button.height != 18) {
                continue;
            }
            int x = getButtonX(button);
            int y = getButtonY(button);
            if (x < leftBound || x > rightEdge + 60) {
                continue;
            }
            if (y < topBound || y > bottomBound) {
                continue;
            }
            if (best == null || y > getButtonY(best)) {
                best = button;
            }
        }
        return best;
    }

    private static int getButtonX(GuiButton button) {
        Integer value = readIntField(button, "x", "xPosition");
        return value == null ? 0 : value;
    }

    private static int getButtonY(GuiButton button) {
        Integer value = readIntField(button, "y", "yPosition");
        return value == null ? 0 : value;
    }

    private static Integer readIntField(Object target, String... fieldNames) {
        if (target == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (fieldName == null || fieldName.isEmpty()) {
                continue;
            }
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                boolean oldAccessible = field.isAccessible();
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof Integer) {
                        return (Integer) value;
                    }
                } finally {
                    try {
                        field.setAccessible(oldAccessible);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static int[] readGuiContainerSize(GuiContainer gui) {
        try {
            Field xSizeField = GuiContainer.class.getDeclaredField("xSize");
            Field ySizeField = GuiContainer.class.getDeclaredField("ySize");
            boolean oldXAccessible = xSizeField.isAccessible();
            boolean oldYAccessible = ySizeField.isAccessible();
            int xSize;
            int ySize;
            try {
                xSizeField.setAccessible(true);
                ySizeField.setAccessible(true);
                xSize = (int) xSizeField.get(gui);
                ySize = (int) ySizeField.get(gui);
            } finally {
                try {
                    xSizeField.setAccessible(oldXAccessible);
                } catch (Exception ignored) {
                }
                try {
                    ySizeField.setAccessible(oldYAccessible);
                } catch (Exception ignored) {
                }
            }
            if (xSize <= 0 || ySize <= 0) {
                return null;
            }
            return new int[]{xSize, ySize};
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<FilterRule> mergeRules(List<FilterRule> existing, FilterRule adding) {
        List<FilterRule> merged = new ArrayList<>();
        if (existing != null) {
            for (FilterRule rule : existing) {
                if (rule != null && !merged.contains(rule)) {
                    merged.add(rule);
                }
            }
        }
        if (adding != null && !merged.contains(adding)) {
            merged.add(adding);
        }
        if (merged.size() > 200) {
            merged = merged.subList(0, 200);
        }
        return merged;
    }
}
