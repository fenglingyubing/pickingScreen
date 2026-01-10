package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.event.DropClearArea;
import com.fenglingyubing.pickupfilter.event.DropClearLogic;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class ClearDropsPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<ClearDropsPacket, IMessage> {
        @Override
        public IMessage onMessage(ClearDropsPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }

            player.getServerWorld().addScheduledTask(() -> {
                if (player.world == null || player.world.isRemote) {
                    return;
                }

                AxisAlignedBB scanBox = DropClearArea.chunkRadiusArea(player, DropClearArea.DEFAULT_CHUNK_RADIUS);
                List<EntityItem> drops = player.world.getEntitiesWithinAABB(EntityItem.class, scanBox);
                DropClearLogic.clearAll(drops, drop -> drop.isDead, EntityItem::setDead);

                player.sendMessage(new TextComponentTranslation("pickupfilter.message.cleared_drops"));
            });

            return null;
        }
    }
}

