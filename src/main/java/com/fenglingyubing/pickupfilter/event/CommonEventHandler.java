package com.fenglingyubing.pickupfilter.event;

import com.fenglingyubing.pickupfilter.config.ConfigManager;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CommonEventHandler {
    private final ConfigManager configManager;

    public CommonEventHandler(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @SubscribeEvent
    public void onEntityItemPickup(EntityItemPickupEvent event) {
        // TODO: implement pickup filtering (docs/requirements.md 需求 3)
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        // TODO: implement auto-destroy scan (docs/requirements.md 需求 2)
    }
}

