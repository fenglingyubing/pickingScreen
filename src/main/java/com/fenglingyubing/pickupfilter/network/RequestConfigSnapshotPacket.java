package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.PickupFilterMod;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.config.PlayerFilterConfigStore;
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
                PlayerFilterConfigStore store = PickupFilterMod.instance.getPlayerConfigStore();
                if (store == null) {
                    return;
                }

                PlayerFilterConfigStore.Snapshot snapshot = store.getSnapshot(player);
                FilterMode mode = snapshot == null ? FilterMode.DISABLED : snapshot.getMode();

                List<String> pickupSerialized = new ArrayList<>();
                List<FilterRule> pickupRules = snapshot == null ? null : snapshot.getPickupRules();
                if (pickupRules != null) {
                    for (FilterRule rule : pickupRules) {
                        if (rule != null) {
                            pickupSerialized.add(rule.serialize());
                        }
                    }
                }

                List<String> destroySerialized = new ArrayList<>();
                List<FilterRule> destroyRules = snapshot == null ? null : snapshot.getDestroyRules();
                if (destroyRules != null) {
                    for (FilterRule rule : destroyRules) {
                        if (rule != null) {
                            destroySerialized.add(rule.serialize());
                        }
                    }
                }

                PickupFilterNetwork.CHANNEL.sendTo(new ConfigSnapshotPacket(
                        mode == null ? FilterMode.DISABLED.getId() : mode.getId(),
                        pickupSerialized,
                        destroySerialized
                ), player);
            });

            return null;
        }
    }
}
