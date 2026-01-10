package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.PickupFilterMod;
import com.fenglingyubing.pickupfilter.config.ConfigManager;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class UpdateConfigPacket implements IMessage {
    private List<String> rules;

    public UpdateConfigPacket() {
    }

    public UpdateConfigPacket(List<FilterRule> rules) {
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
        int size = Math.max(0, Math.min(buf.readInt(), 512));
        rules = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            rules.add(ConfigSnapshotPacket.readString(buf, 256));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        List<String> safeRules = rules == null ? new ArrayList<>() : rules;
        buf.writeInt(Math.min(safeRules.size(), 512));
        for (int i = 0; i < safeRules.size() && i < 512; i++) {
            ConfigSnapshotPacket.writeString(buf, safeRules.get(i));
        }
    }

    public static class Handler implements IMessageHandler<UpdateConfigPacket, IMessage> {
        @Override
        public IMessage onMessage(UpdateConfigPacket message, MessageContext ctx) {
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

                List<FilterRule> parsed = new ArrayList<>();
                if (message.rules != null) {
                    for (String serialized : message.rules) {
                        FilterRule rule = FilterRule.deserialize(serialized);
                        if (rule != null && !parsed.contains(rule)) {
                            parsed.add(rule);
                        }
                    }
                }

                if (parsed.size() > 200) {
                    parsed = parsed.subList(0, 200);
                }

                configManager.setFilterRules(parsed);
                configManager.saveConfig();
                player.sendMessage(new TextComponentTranslation("pickupfilter.message.config_updated", parsed.size()));
            });

            return null;
        }
    }
}

