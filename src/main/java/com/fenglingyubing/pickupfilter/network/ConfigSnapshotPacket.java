package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConfigSnapshotPacket implements IMessage {
    private String modeId;
    private List<String> pickupRules;
    private List<String> destroyRules;
    private static final int MAX_RULES = 200;

    public ConfigSnapshotPacket() {
    }

    public ConfigSnapshotPacket(String modeId, List<String> pickupRules, List<String> destroyRules) {
        this.modeId = modeId;
        this.pickupRules = pickupRules;
        this.destroyRules = destroyRules;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        modeId = readString(buf, 32);
        pickupRules = readRulesList(buf);
        destroyRules = readRulesList(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeString(buf, modeId == null ? FilterMode.DISABLED.getId() : modeId, 32);
        writeRulesList(buf, pickupRules);
        writeRulesList(buf, destroyRules);
    }

    public static class Handler implements IMessageHandler<ConfigSnapshotPacket, IMessage> {
        @Override
        public IMessage onMessage(ConfigSnapshotPacket message, MessageContext ctx) {
            if (ctx == null || ctx.side == null || !ctx.side.isClient()) {
                return null;
            }
            IThreadListener threadListener = FMLCommonHandler.instance().getWorldThread(ctx.netHandler);
            if (threadListener == null) {
                return null;
            }
            threadListener.addScheduledTask(() -> {
                FilterMode mode = FilterMode.fromId(message.modeId);
                ClientConfigSnapshotStore.update(
                        mode,
                        parseRules(message.pickupRules),
                        parseRules(message.destroyRules)
                );
            });
            return null;
        }
    }

    private static List<FilterRule> parseRules(List<String> serializedRules) {
        List<FilterRule> parsed = new ArrayList<>();
        Set<FilterRule> seen = new LinkedHashSet<>();
        if (serializedRules != null) {
            for (String serialized : serializedRules) {
                FilterRule rule = FilterRule.deserialize(serialized);
                if (rule != null && seen.add(rule)) {
                    parsed.add(rule);
                }
            }
        }
        if (parsed.size() > MAX_RULES) {
            parsed = parsed.subList(0, MAX_RULES);
        }
        return parsed;
    }

    private static List<String> readRulesList(ByteBuf buf) {
        int size = readClampedInt(buf, 512);
        List<String> rules = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            rules.add(readString(buf, 256));
        }
        return rules;
    }

    private static void writeRulesList(ByteBuf buf, List<String> rules) {
        List<String> safeRules = rules == null ? new ArrayList<>() : rules;
        buf.writeInt(Math.min(safeRules.size(), 512));
        for (int i = 0; i < safeRules.size() && i < 512; i++) {
            writeString(buf, safeRules.get(i), 256);
        }
    }

    static void writeString(ByteBuf buf, String value) {
        writeString(buf, value, Integer.MAX_VALUE);
    }

    static void writeString(ByteBuf buf, String value, int maxBytes) {
        if (buf == null) {
            return;
        }
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (maxBytes >= 0 && bytes.length > maxBytes) {
            String truncated = new String(bytes, 0, maxBytes, StandardCharsets.UTF_8);
            bytes = truncated.getBytes(StandardCharsets.UTF_8);
        }
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    static String readString(ByteBuf buf, int maxBytes) {
        if (buf == null || buf.readableBytes() < 4) {
            return "";
        }
        int len = buf.readInt();
        if (len < 0 || len > maxBytes) {
            buf.skipBytes(Math.max(0, Math.min(len, buf.readableBytes())));
            return "";
        }
        int available = Math.min(len, buf.readableBytes());
        if (available <= 0) {
            return "";
        }
        byte[] bytes = new byte[available];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static int readClampedInt(ByteBuf buf, int maxValue) {
        if (buf == null || buf.readableBytes() < 4) {
            return 0;
        }
        int value = buf.readInt();
        if (value <= 0) {
            return 0;
        }
        return Math.min(value, maxValue);
    }
}
