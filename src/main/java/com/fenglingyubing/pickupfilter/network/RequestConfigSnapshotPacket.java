package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.PickupFilterMod;
import com.fenglingyubing.pickupfilter.config.ConfigManager;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class RequestConfigSnapshotPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<RequestConfigSnapshotPacket, IMessage> {
        @Override
        public IMessage onMessage(RequestConfigSnapshotPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }

            player.getServerWorld().addScheduledTask(() -> {
                if (PickupFilterMod.instance == null) {
                    return;
                }
                ConfigManager configManager = PickupFilterMod.instance.getConfigManager();
                if (configManager == null) {
                    return;
                }

                FilterMode mode = configManager.getCurrentMode();
                List<FilterRule> rules = configManager.getFilterRules();
                List<String> serialized = new ArrayList<>();
                for (FilterRule rule : rules) {
                    if (rule != null) {
                        serialized.add(rule.serialize());
                    }
                }

                PickupFilterNetwork.CHANNEL.sendTo(new ConfigSnapshotPacket(mode == null ? FilterMode.DISABLED.getId() : mode.getId(), serialized), player);
            });

            return null;
        }
    }
}

