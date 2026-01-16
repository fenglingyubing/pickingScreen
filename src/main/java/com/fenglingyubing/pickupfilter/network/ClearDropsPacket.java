package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.PickupFilterCommon;
import com.fenglingyubing.pickupfilter.event.DropClearArea;
import com.fenglingyubing.pickupfilter.event.DropClearLogic;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class ClearDropsPacket implements IMessage {
    private int chunkRadius = -1;

    public ClearDropsPacket() {
    }

    public ClearDropsPacket(int chunkRadius) {
        this.chunkRadius = chunkRadius;
    }

    public int getChunkRadius() {
        return chunkRadius;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf != null && buf.readableBytes() >= 4) {
            chunkRadius = buf.readInt();
        } else {
            chunkRadius = -1;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (buf != null) {
            buf.writeInt(chunkRadius);
        }
    }

    public static class Handler implements IMessageHandler<ClearDropsPacket, IMessage> {
        @Override
        public IMessage onMessage(ClearDropsPacket message, MessageContext ctx) {
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

                int chunkRadius = message == null ? -1 : message.getChunkRadius();
                if (chunkRadius < 0) {
                    chunkRadius = PickupFilterCommon.getCommonSettings().getClearDropsChunkRadius();
                }
                chunkRadius = clampChunkRadius(chunkRadius);
                AxisAlignedBB scanBox = DropClearArea.chunkRadiusArea(player, chunkRadius);
                List<EntityItem> drops = player.world.getEntitiesWithinAABB(EntityItem.class, scanBox);
                DropClearLogic.clearAll(drops, drop -> drop.isDead, EntityItem::setDead);

                List<EntityArrow> arrows = player.world.getEntitiesWithinAABB(EntityArrow.class, scanBox);
                clearGroundArrows(arrows);

                TextComponentTranslation messageComponent = new TextComponentTranslation("pickupfilter.message.cleared_drops");
                messageComponent.getStyle().setColor(TextFormatting.GRAY);
                player.sendMessage(messageComponent);
            });

            return null;
        }
    }

    private static int clampChunkRadius(int value) {
        return Math.max(0, Math.min(16, value));
    }

    private static void clearGroundArrows(List<EntityArrow> arrows) {
        if (arrows == null || arrows.isEmpty()) {
            return;
        }

        for (EntityArrow arrow : arrows) {
            if (arrow == null || arrow.isDead) {
                continue;
            }
            if (!isArrowInGround(arrow)) {
                continue;
            }
            arrow.setDead();
        }
    }

    private static boolean isArrowInGround(EntityArrow arrow) {
        if (arrow == null) {
            return false;
        }

        try {
            Boolean inGround = ObfuscationReflectionHelper.getPrivateValue(
                    EntityArrow.class,
                    arrow,
                    "inGround",
                    "field_70254_i"
            );
            if (inGround != null) {
                return inGround;
            }
        } catch (RuntimeException ignored) {
        }

        if (arrow.onGround) {
            return true;
        }

        return arrow.motionX == 0D && arrow.motionY == 0D && arrow.motionZ == 0D;
    }
}
