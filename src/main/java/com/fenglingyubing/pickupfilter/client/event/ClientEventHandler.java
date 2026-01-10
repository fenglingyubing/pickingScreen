package com.fenglingyubing.pickupfilter.client.event;

import com.fenglingyubing.pickupfilter.client.PickupFilterClient;
import com.fenglingyubing.pickupfilter.client.gui.PickupFilterConfigScreen;
import com.fenglingyubing.pickupfilter.client.gui.PickupFilterIntroScreen;
import com.fenglingyubing.pickupfilter.client.input.KeyBindingManager;
import com.fenglingyubing.pickupfilter.network.ClearDropsPacket;
import com.fenglingyubing.pickupfilter.network.CycleModePacket;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientEventHandler {
    private boolean introChecked;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Minecraft.getMinecraft().player == null) {
            return;
        }

        if (KeyBindingManager.consumeClearDropsKeyPress()) {
            PickupFilterNetwork.CHANNEL.sendToServer(new ClearDropsPacket());
        }

        if (KeyBindingManager.consumeToggleModeKeyPress()) {
            PickupFilterNetwork.CHANNEL.sendToServer(new CycleModePacket());
        }

        if (KeyBindingManager.consumeOpenConfigKeyPress() && Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().displayGuiScreen(new PickupFilterConfigScreen());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (introChecked) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }

        introChecked = true;
        if (!PickupFilterClient.getClientSettings().isIntroShown()) {
            mc.displayGuiScreen(new PickupFilterIntroScreen());
        }
    }
}
