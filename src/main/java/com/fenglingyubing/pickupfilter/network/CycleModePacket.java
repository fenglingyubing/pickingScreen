package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.PickupFilterMod;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.ModeSwitching;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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

                if (PickupFilterMod.instance == null || PickupFilterMod.instance.getConfigManager() == null) {
                    return;
                }

                ModeSwitching modeSwitching = PickupFilterMod.instance.getModeSwitching();
                if (modeSwitching == null) {
                    return;
                }
                FilterMode newMode = modeSwitching.cycleToNextMode();

                player.sendMessage(new TextComponentTranslation(
                        "pickupfilter.message.mode_changed",
                        new TextComponentTranslation(newMode.getTranslationKey())
                ));
            });

            return null;
        }
    }
}
