package com.fenglingyubing.pickupfilter.proxy;

import com.fenglingyubing.pickupfilter.client.event.ClientEventHandler;
import com.fenglingyubing.pickupfilter.client.input.KeyBindingManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void init(FMLInitializationEvent event) {
        KeyBindingManager.registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }
}

