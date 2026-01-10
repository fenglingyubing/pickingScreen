package com.fenglingyubing.pickupfilter.event;

import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.PlayerFilterConfigStore;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class CommonEventHandler {
    private final PlayerFilterConfigStore configStore;
    private static final double AUTO_DESTROY_RANGE = 1.5D;

    public CommonEventHandler(PlayerFilterConfigStore configStore) {
        this.configStore = configStore;
    }

    @SubscribeEvent
    public void onEntityItemPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }

        FilterMode mode = configStore.getMode(player);
        EntityItem entityItem = event.getItem();
        ItemStack item = entityItem == null ? ItemStack.EMPTY : entityItem.getItem();

        boolean matchesFilter = matchesFilter(player, item);
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

        FilterMode mode = configStore.getMode(player);
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
            if (ItemActionPolicy.shouldDestroyDrop(mode, matchesFilter(player, item))) {
                drop.setDead();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event == null) {
            return;
        }
        EntityPlayer original = event.getOriginal();
        EntityPlayer player = event.getEntityPlayer();
        if (original == null || player == null) {
            return;
        }
        if (player.world == null || player.world.isRemote) {
            return;
        }
        configStore.copyPersistedData(original, player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event == null || event.player == null) {
            return;
        }
        if (event.player.world == null || event.player.world.isRemote) {
            return;
        }
        configStore.invalidate(event.player);
    }

    private boolean matchesFilter(EntityPlayer player, ItemStack item) {
        if (item == null || item.isEmpty() || item.getItem() == null) {
            return false;
        }
        ResourceLocation registryName = item.getItem().getRegistryName();
        if (registryName == null) {
            return false;
        }
        return configStore.matchesAnyRule(player, registryName.toString(), item.getMetadata());
    }
}
