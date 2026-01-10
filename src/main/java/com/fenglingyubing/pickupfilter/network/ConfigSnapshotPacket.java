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
import java.util.List;

public class ConfigSnapshotPacket implements IMessage {
    private String modeId;
    private List<String> rules;

    public ConfigSnapshotPacket() {
    }

    public ConfigSnapshotPacket(String modeId, List<String> rules) {
        this.modeId = modeId;
        this.rules = rules;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        modeId = readString(buf, 32);
        int size = Math.max(0, Math.min(buf.readInt(), 512));
        rules = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            rules.add(readString(buf, 256));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeString(buf, modeId == null ? FilterMode.DISABLED.getId() : modeId);
        List<String> safeRules = rules == null ? new ArrayList<>() : rules;
        buf.writeInt(Math.min(safeRules.size(), 512));
        for (int i = 0; i < safeRules.size() && i < 512; i++) {
            writeString(buf, safeRules.get(i));
        }
    }

    public static class Handler implements IMessageHandler<ConfigSnapshotPacket, IMessage> {
        @Override
        public IMessage onMessage(ConfigSnapshotPacket message, MessageContext ctx) {
            if (ctx == null || ctx.side == null || !ctx.side.isClient()) {
                return null;
            }
            IThreadListener threadListener = FMLCommonHandler.instance().getWorldThread(ctx.netHandler);
            threadListener.addScheduledTask(() -> {
                FilterMode mode = FilterMode.fromId(message.modeId);
                List<FilterRule> parsed = new ArrayList<>();
                if (message.rules != null) {
                    for (String serialized : message.rules) {
                        FilterRule rule = FilterRule.deserialize(serialized);
                        if (rule != null) {
                            parsed.add(rule);
                        }
                    }
                }
                ClientConfigSnapshotStore.update(mode, parsed);
            });
            return null;
        }
    }

    static void writeString(ByteBuf buf, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    static String readString(ByteBuf buf, int maxBytes) {
        int len = buf.readInt();
        if (len < 0 || len > maxBytes) {
            buf.skipBytes(Math.max(0, Math.min(len, buf.readableBytes())));
            return "";
        }
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
