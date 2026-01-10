package com.fenglingyubing.pickupfilter.client.event;

import com.fenglingyubing.pickupfilter.client.input.KeyBindingManager;
import com.fenglingyubing.pickupfilter.network.ClearDropsPacket;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientEventHandler {
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!KeyBindingManager.consumeClearDropsKeyPress()) {
            return;
        }

        if (Minecraft.getMinecraft().player == null) {
            return;
        }

        PickupFilterNetwork.CHANNEL.sendToServer(new ClearDropsPacket());
    }
}
