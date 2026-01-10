package com.fenglingyubing.pickupfilter.client.event;

import com.fenglingyubing.pickupfilter.client.gui.PickupFilterMatcherScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
}

