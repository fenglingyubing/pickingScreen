package com.fenglingyubing.pickupfilter.client.event;

import com.fenglingyubing.pickupfilter.client.input.KeyBindingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientEventHandler {
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!KeyBindingManager.CLEAR_DROPS_KEY.isPressed()) {
            return;
        }

        if (Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("TODO: 清除附近掉落物"));
        }
    }
}

