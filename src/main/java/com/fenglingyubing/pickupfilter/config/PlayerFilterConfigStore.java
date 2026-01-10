package com.fenglingyubing.pickupfilter.config;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerFilterConfigStore {
    private static final String ROOT_KEY = "pickupfilter";
    private static final String KEY_MODE = "mode";
    private static final String KEY_RULES_PICKUP = "rules_pickup";
    private static final String KEY_RULES_DESTROY = "rules_destroy";
    private static final String KEY_RULES_LEGACY = "rules";
    private static final int MAX_RULES = 200;

    private final Map<UUID, Snapshot> cache = new HashMap<>();

    public Snapshot getSnapshot(EntityPlayer player) {
        if (player == null) {
            return Snapshot.defaults();
        }
        UUID uuid = player.getUniqueID();
        Snapshot cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        Snapshot loaded = loadSnapshot(player);
        cache.put(uuid, loaded);
        return loaded;
    }

    public FilterMode getMode(EntityPlayer player) {
        return getSnapshot(player).getMode();
    }

    public List<FilterRule> getPickupRules(EntityPlayer player) {
        return getSnapshot(player).getPickupRules();
    }

    public List<FilterRule> getDestroyRules(EntityPlayer player) {
        return getSnapshot(player).getDestroyRules();
    }

    public List<FilterRule> getRulesForMode(EntityPlayer player, FilterMode mode) {
        FilterMode safeMode = mode == null ? FilterMode.DISABLED : mode;
        if (safeMode == FilterMode.DESTROY_MATCHING) {
            return getDestroyRules(player);
        }
        return getPickupRules(player);
    }

    public boolean matchesAnyRule(EntityPlayer player, String registryName, int metadata) {
        FilterMode mode = getMode(player);
        if (mode == null || mode == FilterMode.DISABLED) {
            return false;
        }
        List<FilterRule> rules = getRulesForMode(player, mode);
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        for (FilterRule rule : rules) {
            if (rule != null && rule.matches(registryName, metadata)) {
                return true;
            }
        }
        return false;
    }

    public FilterMode cycleToNextMode(EntityPlayer player) {
        FilterMode current = getMode(player);
        FilterMode next = FilterModeCycle.next(current);
        setMode(player, next);
        return next;
    }

    public void setMode(EntityPlayer player, FilterMode mode) {
        Snapshot snapshot = getSnapshot(player);
        Snapshot updated = new Snapshot(
                mode == null ? FilterMode.DISABLED : mode,
                snapshot.getPickupRules(),
                snapshot.getDestroyRules()
        );
        saveSnapshot(player, updated);
    }

    public void setRulesForMode(EntityPlayer player, FilterMode mode, List<FilterRule> rules) {
        FilterMode safeMode = mode == null ? FilterMode.DISABLED : mode;
        Snapshot snapshot = getSnapshot(player);
        List<FilterRule> normalized = normalizeRules(rules);
        Snapshot updated = safeMode == FilterMode.DESTROY_MATCHING
                ? new Snapshot(snapshot.getMode(), snapshot.getPickupRules(), normalized)
                : new Snapshot(snapshot.getMode(), normalized, snapshot.getDestroyRules());
        saveSnapshot(player, updated);
    }

    public void invalidate(EntityPlayer player) {
        if (player == null) {
            return;
        }
        cache.remove(player.getUniqueID());
    }

    public void copyPersistedData(EntityPlayer from, EntityPlayer to) {
        if (from == null || to == null) {
            return;
        }
        NBTTagCompound fromEntityData = from.getEntityData();
        if (fromEntityData == null || !fromEntityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        NBTTagCompound fromPersisted = fromEntityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!fromPersisted.hasKey(ROOT_KEY, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        NBTTagCompound fromRoot = fromPersisted.getCompoundTag(ROOT_KEY);

        NBTTagCompound toEntityData = to.getEntityData();
        if (toEntityData == null) {
            return;
        }
        NBTTagCompound toPersisted = toEntityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)
                ? toEntityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
                : new NBTTagCompound();
        toPersisted.setTag(ROOT_KEY, fromRoot.copy());
        toEntityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, toPersisted);
        cache.put(to.getUniqueID(), fromSnapshotCompound(fromRoot));
    }

    private Snapshot loadSnapshot(EntityPlayer player) {
        NBTTagCompound root = getRoot(player, false);
        if (root == null) {
            return Snapshot.defaults();
        }
        return fromSnapshotCompound(root);
    }

    private Snapshot fromSnapshotCompound(NBTTagCompound root) {
        FilterMode mode = FilterMode.fromId(root.getString(KEY_MODE));
        if (mode == null) {
            mode = FilterMode.DISABLED;
        }

        List<FilterRule> pickupRules = new ArrayList<>();
        if (root.hasKey(KEY_RULES_PICKUP, Constants.NBT.TAG_LIST)) {
            NBTTagList list = root.getTagList(KEY_RULES_PICKUP, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                FilterRule rule = FilterRule.deserialize(list.getStringTagAt(i));
                if (rule != null && !pickupRules.contains(rule)) {
                    pickupRules.add(rule);
                }
            }
        }

        List<FilterRule> destroyRules = new ArrayList<>();
        if (root.hasKey(KEY_RULES_DESTROY, Constants.NBT.TAG_LIST)) {
            NBTTagList list = root.getTagList(KEY_RULES_DESTROY, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                FilterRule rule = FilterRule.deserialize(list.getStringTagAt(i));
                if (rule != null && !destroyRules.contains(rule)) {
                    destroyRules.add(rule);
                }
            }
        }

        if (pickupRules.isEmpty() && destroyRules.isEmpty() && root.hasKey(KEY_RULES_LEGACY, Constants.NBT.TAG_LIST)) {
            NBTTagList legacy = root.getTagList(KEY_RULES_LEGACY, Constants.NBT.TAG_STRING);
            for (int i = 0; i < legacy.tagCount(); i++) {
                FilterRule rule = FilterRule.deserialize(legacy.getStringTagAt(i));
                if (rule != null && !pickupRules.contains(rule)) {
                    pickupRules.add(rule);
                }
            }
        }

        pickupRules = normalizeRules(pickupRules);
        destroyRules = normalizeRules(destroyRules);
        return new Snapshot(mode, pickupRules, destroyRules);
    }

    private void saveSnapshot(EntityPlayer player, Snapshot snapshot) {
        if (player == null || snapshot == null) {
            return;
        }

        NBTTagCompound root = getRoot(player, true);
        FilterMode mode = snapshot.getMode() == null ? FilterMode.DISABLED : snapshot.getMode();
        root.setString(KEY_MODE, mode.getId());

        root.setTag(KEY_RULES_PICKUP, toRulesTag(snapshot.getPickupRules()));
        root.setTag(KEY_RULES_DESTROY, toRulesTag(snapshot.getDestroyRules()));

        cache.put(player.getUniqueID(), new Snapshot(
                mode,
                normalizeRules(snapshot.getPickupRules()),
                normalizeRules(snapshot.getDestroyRules())
        ));
    }

    private static NBTTagList toRulesTag(List<FilterRule> rules) {
        NBTTagList list = new NBTTagList();
        List<FilterRule> normalized = normalizeRules(rules);
        for (FilterRule rule : normalized) {
            if (rule != null) {
                list.appendTag(new NBTTagString(rule.serialize()));
            }
        }
        return list;
    }

    private static List<FilterRule> normalizeRules(List<FilterRule> rules) {
        List<FilterRule> normalized = new ArrayList<>();
        if (rules != null) {
            for (FilterRule rule : rules) {
                if (rule != null && !normalized.contains(rule)) {
                    normalized.add(rule);
                }
            }
        }
        if (normalized.size() > MAX_RULES) {
            normalized = normalized.subList(0, MAX_RULES);
        }
        return Collections.unmodifiableList(normalized);
    }

    private static NBTTagCompound getRoot(EntityPlayer player, boolean create) {
        NBTTagCompound entityData = player.getEntityData();
        if (entityData == null) {
            return null;
        }

        NBTTagCompound persisted;
        if (entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) {
            persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        } else if (create) {
            persisted = new NBTTagCompound();
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        } else {
            return null;
        }

        if (persisted.hasKey(ROOT_KEY, Constants.NBT.TAG_COMPOUND)) {
            return persisted.getCompoundTag(ROOT_KEY);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound root = new NBTTagCompound();
        persisted.setTag(ROOT_KEY, root);
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        return root;
    }

    public static final class Snapshot {
        private final FilterMode mode;
        private final List<FilterRule> pickupRules;
        private final List<FilterRule> destroyRules;

        public Snapshot(FilterMode mode, List<FilterRule> pickupRules, List<FilterRule> destroyRules) {
            this.mode = mode;
            this.pickupRules = pickupRules == null ? Collections.emptyList() : pickupRules;
            this.destroyRules = destroyRules == null ? Collections.emptyList() : destroyRules;
        }

        public static Snapshot defaults() {
            return new Snapshot(FilterMode.DISABLED, Collections.emptyList(), Collections.emptyList());
        }

        public FilterMode getMode() {
            return mode;
        }

        public List<FilterRule> getPickupRules() {
            return pickupRules;
        }

        public List<FilterRule> getDestroyRules() {
            return destroyRules;
        }
    }
}
