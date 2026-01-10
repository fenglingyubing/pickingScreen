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
    private static final String KEY_RULES = "rules";
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

    public List<FilterRule> getRules(EntityPlayer player) {
        return getSnapshot(player).getRules();
    }

    public boolean matchesAnyRule(EntityPlayer player, String registryName, int metadata) {
        List<FilterRule> rules = getRules(player);
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
        Snapshot updated = new Snapshot(mode == null ? FilterMode.DISABLED : mode, snapshot.getRules());
        saveSnapshot(player, updated);
    }

    public void setRules(EntityPlayer player, List<FilterRule> rules) {
        Snapshot snapshot = getSnapshot(player);
        Snapshot updated = new Snapshot(snapshot.getMode(), normalizeRules(rules));
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

        List<FilterRule> rules = new ArrayList<>();
        if (root.hasKey(KEY_RULES, Constants.NBT.TAG_LIST)) {
            NBTTagList list = root.getTagList(KEY_RULES, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                FilterRule rule = FilterRule.deserialize(list.getStringTagAt(i));
                if (rule != null && !rules.contains(rule)) {
                    rules.add(rule);
                }
            }
        }
        if (rules.size() > MAX_RULES) {
            rules = rules.subList(0, MAX_RULES);
        }
        return new Snapshot(mode, Collections.unmodifiableList(rules));
    }

    private void saveSnapshot(EntityPlayer player, Snapshot snapshot) {
        if (player == null || snapshot == null) {
            return;
        }

        NBTTagCompound root = getRoot(player, true);
        FilterMode mode = snapshot.getMode() == null ? FilterMode.DISABLED : snapshot.getMode();
        root.setString(KEY_MODE, mode.getId());

        NBTTagList list = new NBTTagList();
        List<FilterRule> normalized = normalizeRules(snapshot.getRules());
        for (FilterRule rule : normalized) {
            if (rule != null) {
                list.appendTag(new NBTTagString(rule.serialize()));
            }
        }
        root.setTag(KEY_RULES, list);

        cache.put(player.getUniqueID(), new Snapshot(mode, normalized));
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
        private final List<FilterRule> rules;

        public Snapshot(FilterMode mode, List<FilterRule> rules) {
            this.mode = mode;
            this.rules = rules;
        }

        public static Snapshot defaults() {
            return new Snapshot(FilterMode.DISABLED, Collections.emptyList());
        }

        public FilterMode getMode() {
            return mode;
        }

        public List<FilterRule> getRules() {
            return rules;
        }
    }
}
