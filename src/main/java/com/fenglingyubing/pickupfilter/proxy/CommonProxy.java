package com.fenglingyubing.pickupfilter.proxy;

import com.fenglingyubing.pickupfilter.PickupFilterCommon;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        PickupFilterCommon.init(event == null ? null : event.getModConfigurationDirectory());
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
