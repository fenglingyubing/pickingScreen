package com.fenglingyubing.pickupfilter.client.event;

import com.fenglingyubing.pickupfilter.client.PickupFilterClient;
import com.fenglingyubing.pickupfilter.client.gui.PickupFilterConfigScreen;
import com.fenglingyubing.pickupfilter.client.gui.PickupFilterIntroScreen;
import com.fenglingyubing.pickupfilter.client.input.KeyBindingManager;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.network.ClearDropsPacket;
import com.fenglingyubing.pickupfilter.network.ClientConfigSnapshotStore;
import com.fenglingyubing.pickupfilter.network.CycleModePacket;
import com.fenglingyubing.pickupfilter.network.PickupFilterNetwork;
import com.fenglingyubing.pickupfilter.network.RequestConfigSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientEventHandler {
    private boolean introChecked;
    private boolean snapshotRequested;
    private int lastSnapshotRevision = -1;
    private FilterMode lastKnownMode;

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
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                mc.player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "拾取筛：已发送切换请求，等待同步…"), true);
            }
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

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!snapshotRequested) {
            snapshotRequested = true;
            PickupFilterNetwork.CHANNEL.sendToServer(new RequestConfigSnapshotPacket());
        }

        int revision = ClientConfigSnapshotStore.getRevision();
        if (revision != lastSnapshotRevision) {
            lastSnapshotRevision = revision;
            ClientConfigSnapshotStore.Snapshot snapshot = ClientConfigSnapshotStore.getSnapshot();
            FilterMode mode = snapshot == null ? null : snapshot.getMode();
            if (lastKnownMode == null) {
                lastKnownMode = mode;
            } else if (mode != null && mode != lastKnownMode) {
                lastKnownMode = mode;
                mc.player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GRAY + "拾取筛：模式已切换为 "
                                + TextFormatting.AQUA + getModeNameChinese(mode)
                ), true);
            }
        }

        if (introChecked) {
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

    private static String getModeNameChinese(FilterMode mode) {
        if (mode == null) {
            return "关闭";
        }
        switch (mode) {
            case DESTROY_MATCHING:
                return "销毁匹配掉落物";
            case PICKUP_MATCHING:
                return "拾取匹配掉落物";
            case DISABLED:
            default:
                return "关闭";
        }
    }
}
