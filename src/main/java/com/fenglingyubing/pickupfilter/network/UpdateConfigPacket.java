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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UpdateConfigPacket implements IMessage {
    private String targetModeId;
    private List<String> rules;

    public UpdateConfigPacket() {
    }

    public UpdateConfigPacket(FilterMode targetMode, List<FilterRule> rules) {
        this.targetModeId = (targetMode == null ? FilterMode.DISABLED : targetMode).getId();
        List<String> serialized = new ArrayList<>();
        if (rules != null) {
            for (FilterRule rule : rules) {
                if (rule != null) {
                    serialized.add(rule.serialize());
                }
            }
        }
        this.rules = serialized;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        targetModeId = ConfigSnapshotPacket.readString(buf, 32);
        int size = ConfigSnapshotPacket.readClampedInt(buf, 512);
        rules = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            rules.add(ConfigSnapshotPacket.readString(buf, 256));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ConfigSnapshotPacket.writeString(buf, targetModeId == null ? FilterMode.DISABLED.getId() : targetModeId, 32);
        List<String> safeRules = rules == null ? new ArrayList<>() : rules;
        buf.writeInt(Math.min(safeRules.size(), 512));
        for (int i = 0; i < safeRules.size() && i < 512; i++) {
            ConfigSnapshotPacket.writeString(buf, safeRules.get(i), 256);
        }
    }

    public static class Handler implements IMessageHandler<UpdateConfigPacket, IMessage> {
        @Override
        public IMessage onMessage(UpdateConfigPacket message, MessageContext ctx) {
            if (ctx == null || ctx.getServerHandler() == null) {
                return null;
            }
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

                List<FilterRule> parsed = new ArrayList<>();
                Set<FilterRule> seen = new LinkedHashSet<>();
                if (message.rules != null) {
                    for (String serialized : message.rules) {
                        FilterRule rule = FilterRule.deserialize(serialized);
                        if (rule != null && seen.add(rule)) {
                            parsed.add(rule);
                        }
                    }
                }

                if (parsed.size() > 200) {
                    parsed = parsed.subList(0, 200);
                }

                FilterMode targetMode = FilterMode.fromId(message.targetModeId);
                store.setRulesForMode(player, targetMode, parsed);
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.GRAY + "拾取筛：已保存"
                                + (targetMode == FilterMode.DESTROY_MATCHING ? "销毁" : "拾取")
                                + "列表（" + parsed.size() + " 条）"
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
}
