package com.fenglingyubing.pickupfilter.event;

import com.fenglingyubing.pickupfilter.PickupFilterCommon;
import com.fenglingyubing.pickupfilter.config.FilterMode;
import com.fenglingyubing.pickupfilter.config.FilterRule;
import com.fenglingyubing.pickupfilter.config.PlayerFilterConfigStore;
import com.fenglingyubing.pickupfilter.settings.CommonSettings;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommonEventHandler {
    private final PlayerFilterConfigStore configStore;
    private static final double AUTO_DESTROY_RANGE = 1.5D;
    private final Map<UUID, AutoDestroyState> autoDestroyStates = new HashMap<>();

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
            autoDestroyStates.remove(player.getUniqueID());
            return;
        }

        List<FilterRule> rulesForMode = configStore.getRulesForMode(player, mode);
        if (rulesForMode == null || rulesForMode.isEmpty()) {
            autoDestroyStates.remove(player.getUniqueID());
            return;
        }

        CommonSettings settings = PickupFilterCommon.getCommonSettings();
        int minIntervalTicks = Math.max(1, settings.getAutoDestroyScanMinIntervalTicks());
        int maxIntervalTicks = Math.max(minIntervalTicks, settings.getAutoDestroyScanMaxIntervalTicks());
        int maxEntities = Math.max(0, settings.getAutoDestroyScanMaxEntities());
        int emptyBackoffMissThreshold = Math.max(1, settings.getAutoDestroyEmptyBackoffMissThreshold());

        UUID playerId = player.getUniqueID();
        AutoDestroyState state = autoDestroyStates.get(playerId);
        if (state == null) {
            state = new AutoDestroyState(minIntervalTicks, player.ticksExisted, rulesForMode.hashCode(), player.posX, player.posY, player.posZ);
            autoDestroyStates.put(playerId, state);
        }

        int rulesHash = rulesForMode.hashCode();
        boolean rulesChanged = rulesHash != state.lastRulesHash;
        boolean movedSinceLastScan = state.hasLastPos && squaredDistance(player.posX, player.posY, player.posZ, state.lastPosX, state.lastPosY, state.lastPosZ) > 1.0E-6D;

        int currentTick = player.ticksExisted;
        if (rulesChanged && currentTick > state.lastScanTick) {
            state.currentIntervalTicks = minIntervalTicks;
            state.emptyMissStreak = 0;
            state.nextScanTick = currentTick;
        } else if (movedSinceLastScan
                && state.currentIntervalTicks > minIntervalTicks
                && currentTick - state.lastScanTick >= minIntervalTicks) {
            state.currentIntervalTicks = minIntervalTicks;
            state.emptyMissStreak = 0;
            state.nextScanTick = currentTick;
        }

        if (currentTick < state.nextScanTick) {
            return;
        }

        AxisAlignedBB scanBox = player.getEntityBoundingBox().grow(AUTO_DESTROY_RANGE);
        List<EntityItem> nearbyDrops = player.world.getEntitiesWithinAABB(EntityItem.class, scanBox);
        int destroyed = 0;
        int processed = 0;
        for (EntityItem drop : nearbyDrops) {
            if (maxEntities > 0 && processed >= maxEntities) {
                break;
            }
            if (drop == null || drop.isDead) {
                continue;
            }

            ItemStack item = drop.getItem();
            if (ItemActionPolicy.shouldDestroyDrop(mode, matchesFilter(player, item))) {
                drop.setDead();
                destroyed++;
            }
            processed++;
        }

        state.lastScanTick = currentTick;
        state.lastRulesHash = rulesHash;
        state.lastPosX = player.posX;
        state.lastPosY = player.posY;
        state.lastPosZ = player.posZ;
        state.hasLastPos = true;

        boolean sawDrops = nearbyDrops != null && !nearbyDrops.isEmpty();
        boolean hitEntityCap = maxEntities > 0 && nearbyDrops != null && nearbyDrops.size() > maxEntities;

        if (destroyed > 0 || sawDrops || hitEntityCap) {
            state.currentIntervalTicks = minIntervalTicks;
            state.emptyMissStreak = 0;
        } else {
            state.emptyMissStreak++;
            if (maxIntervalTicks > minIntervalTicks && state.emptyMissStreak >= emptyBackoffMissThreshold) {
                state.currentIntervalTicks = Math.min(maxIntervalTicks, Math.max(minIntervalTicks, state.currentIntervalTicks * 2));
                state.emptyMissStreak = 0;
            } else {
                state.currentIntervalTicks = Math.max(minIntervalTicks, Math.min(maxIntervalTicks, state.currentIntervalTicks));
            }
        }

        state.nextScanTick = currentTick + state.currentIntervalTicks;
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
        autoDestroyStates.remove(player.getUniqueID());
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
        autoDestroyStates.remove(event.player.getUniqueID());
    }

    private boolean matchesFilter(EntityPlayer player, ItemStack item) {
        if (item == null || item.isEmpty() || item.getItem() == null) {
            return false;
        }
        return configStore.matchesAnyRule(player, item);
    }

    private static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class AutoDestroyState {
        private int currentIntervalTicks;
        private int nextScanTick;
        private int emptyMissStreak;
        private int lastRulesHash;
        private int lastScanTick = Integer.MIN_VALUE;
        private boolean hasLastPos;
        private double lastPosX;
        private double lastPosY;
        private double lastPosZ;

        private AutoDestroyState(int minIntervalTicks, int currentTick, int rulesHash, double posX, double posY, double posZ) {
            this.currentIntervalTicks = minIntervalTicks;
            this.nextScanTick = currentTick;
            this.lastRulesHash = rulesHash;
            this.lastPosX = posX;
            this.lastPosY = posY;
            this.lastPosZ = posZ;
            this.hasLastPos = true;
        }
    }
}
