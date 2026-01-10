package com.fenglingyubing.pickupfilter.proxy;

import com.fenglingyubing.pickupfilter.client.PickupFilterClient;
import com.fenglingyubing.pickupfilter.client.event.ClientEventHandler;
import com.fenglingyubing.pickupfilter.client.event.InventoryGuiButtonHandler;
import com.fenglingyubing.pickupfilter.client.input.KeyBindingManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        PickupFilterClient.init(event == null ? null : event.getModConfigurationDirectory());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        KeyBindingManager.registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new InventoryGuiButtonHandler());
    }
}
