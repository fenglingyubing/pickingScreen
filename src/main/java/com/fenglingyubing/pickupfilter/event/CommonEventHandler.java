package com.fenglingyubing.pickupfilter.event;

import com.fenglingyubing.pickupfilter.config.ConfigManager;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class CommonEventHandler {
    private final ConfigManager configManager;
    private static final double AUTO_DESTROY_RANGE = 1.5D;

    public CommonEventHandler(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @SubscribeEvent
    public void onEntityItemPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }

        FilterMode mode = configManager.getCurrentMode();
        EntityItem entityItem = event.getItem();
        ItemStack item = entityItem == null ? ItemStack.EMPTY : entityItem.getItem();

        boolean matchesFilter = matchesFilter(item);
        if (ItemActionPolicy.shouldCancelPickup(mode, matchesFilter)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world == null || player.world.isRemote) {
            return;
        }

        FilterMode mode = configManager.getCurrentMode();
        if (mode != FilterMode.DESTROY_MATCHING) {
            return;
        }

        AxisAlignedBB scanBox = player.getEntityBoundingBox().grow(AUTO_DESTROY_RANGE);
        List<EntityItem> nearbyDrops = player.world.getEntitiesWithinAABB(EntityItem.class, scanBox);
        for (EntityItem drop : nearbyDrops) {
            if (drop == null || drop.isDead) {
                continue;
            }

            ItemStack item = drop.getItem();
            if (ItemActionPolicy.shouldDestroyDrop(mode, matchesFilter(item))) {
                drop.setDead();
            }
        }
    }

    private boolean matchesFilter(ItemStack item) {
        if (item == null || item.isEmpty() || item.getItem() == null) {
            return false;
        }
        ResourceLocation registryName = item.getItem().getRegistryName();
        if (registryName == null) {
            return false;
        }
        return configManager.matchesAnyRule(registryName.toString(), item.getMetadata());
    }
}
