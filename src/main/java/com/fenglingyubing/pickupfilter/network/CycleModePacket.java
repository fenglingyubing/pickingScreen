package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.PickupFilterMod;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.config.PlayerFilterConfigStore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class CycleModePacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<CycleModePacket, IMessage> {
        @Override
        public IMessage onMessage(CycleModePacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }

            player.getServerWorld().addScheduledTask(() -> {
                if (player.world == null || player.world.isRemote) {
                    return;
                }

                if (PickupFilterMod.instance == null) {
                    return;
                }

                PlayerFilterConfigStore store = PickupFilterMod.instance.getPlayerConfigStore();
                if (store == null) {
                    return;
                }
                FilterMode newMode = store.cycleToNextMode(player);

                player.sendMessage(new TextComponentTranslation(
                        "pickupfilter.message.mode_changed",
                        new TextComponentTranslation(newMode.getTranslationKey())
                ));

                List<FilterRule> rules = store.getRules(player);
                List<String> serialized = new ArrayList<>();
                for (FilterRule rule : rules) {
                    if (rule != null) {
                        serialized.add(rule.serialize());
                    }
                }
                PickupFilterNetwork.CHANNEL.sendTo(new ConfigSnapshotPacket(newMode == null ? FilterMode.DISABLED.getId() : newMode.getId(), serialized), player);
            });

            return null;
        }
    }
}
