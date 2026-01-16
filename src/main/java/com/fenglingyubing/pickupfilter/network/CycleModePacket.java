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
            if (ctx == null || ctx.getServerHandler() == null) {
                return null;
            }
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

                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.GRAY + "拾取筛：模式已切换为 "
                                + net.minecraft.util.text.TextFormatting.AQUA + getModeNameChinese(newMode)
                ));

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
