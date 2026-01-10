package com.fenglingyubing.pickupfilter;

import com.fenglingyubing.pickupfilter.config.ConfigManager;
import com.fenglingyubing.pickupfilter.event.CommonEventHandler;
import com.fenglingyubing.pickupfilter.proxy.CommonProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

@Mod(
        modid = PickupFilterMod.MODID,
        name = PickupFilterMod.NAME,
        version = PickupFilterMod.VERSION,
        acceptedMinecraftVersions = "[1.12.2]"
)
public class PickupFilterMod {
    public static final String MODID = "pickupfilter";
    public static final String NAME = "Pickup Filter (拾取筛)";
    public static final String VERSION = "0.1.0";

    @Mod.Instance(MODID)
    public static PickupFilterMod instance;

    @SidedProxy(
            clientSide = "com.fenglingyubing.pickupfilter.proxy.ClientProxy",
            serverSide = "com.fenglingyubing.pickupfilter.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    private ConfigManager configManager;
    private CommonEventHandler eventHandler;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = event.getSuggestedConfigurationFile();
        this.configManager = new ConfigManager(configFile);
        this.configManager.load();
        this.eventHandler = new CommonEventHandler(this.configManager);
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this.eventHandler);
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
