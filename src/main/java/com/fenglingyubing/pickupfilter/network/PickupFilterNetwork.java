package com.fenglingyubing.pickupfilter.network;

import com.fenglingyubing.pickupfilter.PickupFilterMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class PickupFilterNetwork {
    private PickupFilterNetwork() {
    }

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(PickupFilterMod.MODID);

    public static void init() {
        int discriminator = 0;
        CHANNEL.registerMessage(ClearDropsPacket.Handler.class, ClearDropsPacket.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(CycleModePacket.Handler.class, CycleModePacket.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(RequestConfigSnapshotPacket.Handler.class, RequestConfigSnapshotPacket.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(UpdateConfigPacket.Handler.class, UpdateConfigPacket.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(ConfigSnapshotPacket.Handler.class, ConfigSnapshotPacket.class, discriminator++, Side.CLIENT);
    }
}
