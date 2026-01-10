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

        int guiLeft = (gui.width - 176) / 2;
        int guiTop = (gui.height - 166) / 2;

        buttons.add(new GuiButton(BUTTON_ID, guiLeft + 154, guiTop + 4, 18, 18, "筛"));
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
