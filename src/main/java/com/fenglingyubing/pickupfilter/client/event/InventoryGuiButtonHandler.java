package com.fenglingyubing.pickupfilter.client.event;

import com.fenglingyubing.pickupfilter.client.gui.PickupFilterMatcherScreen;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.network.ClientConfigSnapshotStore;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import com.fenglingyubing.pickupfilter.network.RequestConfigSnapshotPacket;
import com.fenglingyubing.pickupfilter.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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

        int targetX = guiLeft + xSize - 22;
        int targetY = guiTop + ySize - 22;

        GuiButton lowestRightButton = findLowestRightSmallButton(buttons, guiLeft, guiTop, xSize, ySize);
        if (lowestRightButton != null) {
            targetX = getButtonX(lowestRightButton);
            targetY = getButtonY(lowestRightButton) + lowestRightButton.height + 2;
            targetY = Math.min(targetY, gui.height - 22);
        }

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
        if (Keyboard.getEventKey() != Keyboard.KEY_A) {
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
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_GRAY + "配置同步中…稍后再按 A"), true);
            return;
        }

        Item item = hovered.getItem();
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) {
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "无法读取物品注册名"), true);
            return;
        }

        FilterRule rule = new FilterRule(registryName.getNamespace(), registryName.getPath(), hovered.getMetadata(), false);
        ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
        List<FilterRule> existing = snapshot == null ? null : snapshot.getRules();
        boolean alreadyExists = existing != null && existing.contains(rule);
        if (alreadyExists) {
            mc.player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_GRAY + "已在规则中："
                    + TextFormatting.AQUA + registryName.getNamespace()
                    + TextFormatting.DARK_GRAY + ":"
                    + TextFormatting.AQUA + registryName.getPath()
                    + TextFormatting.DARK_GRAY + " @"
                    + TextFormatting.AQUA + hovered.getMetadata()), true);
            return;
        }

        List<FilterRule> merged = mergeRules(existing, rule);

        PickupFilterNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(merged));
        PickupFilterNetwork.CHANNEL.sendToServer(new RequestConfigSnapshotPacket());
        mc.player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "已添加到拾取筛："
                + TextFormatting.AQUA + registryName.getNamespace()
                + TextFormatting.GRAY + ":"
                + TextFormatting.AQUA + registryName.getPath()
                + TextFormatting.DARK_GRAY + " @"
                + TextFormatting.AQUA + hovered.getMetadata()
                + TextFormatting.DARK_GRAY + "（A 快捷）"), true);
    }

    private static ItemStack getHoveredStack(GuiScreen gui) {
        if (!(gui instanceof GuiContainer)) {
            return ItemStack.EMPTY;
        }

        Slot hovered = null;
        try {
            Field field = GuiContainer.class.getDeclaredField("hoveredSlot");
            field.setAccessible(true);
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
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Integer) {
                    return (Integer) value;
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
            xSizeField.setAccessible(true);
            ySizeField.setAccessible(true);
            int xSize = (int) xSizeField.get(gui);
            int ySize = (int) ySizeField.get(gui);
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
