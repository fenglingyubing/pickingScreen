package com.fenglingyubing.pickupfilter.config;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class FilterRule {
    public static final int ANY_METADATA = -1;
    public static final String ANY = "*";

    private final String itemName;
    private final int metadata;
    private final String modId;
    private final boolean useWildcard;
    private final Pattern itemNamePattern;

    public FilterRule(String modId, String itemName, int metadata, boolean useWildcard) {
        this.modId = normalizePart(modId);
        this.itemName = normalizePart(itemName);
        this.metadata = metadata;
        this.useWildcard = useWildcard;
        this.itemNamePattern = buildItemNamePattern(this.itemName, useWildcard);
    }

    public String getItemName() {
        return itemName;
    }

    public int getMetadata() {
        return metadata;
    }

    public String getModId() {
        return modId;
    }

    public boolean isUseWildcard() {
        return useWildcard;
    }

    public boolean matches(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.isEmpty() || item.getItem() == null) {
            return false;
        }
        ResourceLocation registryName = item.getItem().getRegistryName();
        if (registryName == null) {
            return false;
        }

        String actualModId = normalizePart(registryName.getNamespace());
        String actualItemName = normalizePart(registryName.getPath());

        if (!matchesPart(modId, actualModId, false)) {
            return false;
        }
        if (!matchesItemName(actualItemName)) {
            return false;
        }

        if (this.metadata == ANY_METADATA) {
            return true;
        }
        if (!item.getItem().getHasSubtypes() && item.isItemStackDamageable()) {
            return true;
        }
        return this.metadata == item.getMetadata();
    }

    public boolean matches(String registryName, int metadata) {
        if (registryName == null) {
            return false;
        }

        ResourceLocation resourceLocation;
        try {
            resourceLocation = new ResourceLocation(registryName);
        } catch (Exception ignored) {
            return false;
        }
        String actualModId = normalizePart(resourceLocation.getNamespace());
        String actualItemName = normalizePart(resourceLocation.getPath());

        if (!matchesPart(modId, actualModId, false)) {
            return false;
        }
        if (!matchesItemName(actualItemName)) {
            return false;
        }
        return this.metadata == ANY_METADATA || this.metadata == metadata;
    }

    public static FilterRule fromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return null;
        }
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) {
            return null;
        }
        int metadata = normalizeMetadataForRule(stack);
        return new FilterRule(registryName.getNamespace(), registryName.getPath(), metadata, false);
    }

    private static int normalizeMetadataForRule(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return ANY_METADATA;
        }
        if (!stack.getItem().getHasSubtypes() && stack.isItemStackDamageable()) {
            return ANY_METADATA;
        }
        return stack.getMetadata();
    }

    public String serialize() {
        String serializedModId = modId == null ? ANY : modId;
        String serializedItemName = itemName == null ? ANY : itemName;
        String serializedMetadata = metadata == ANY_METADATA ? ANY : Integer.toString(metadata);
        String serializedWildcard = useWildcard ? "1" : "0";
        return serializedModId + ":" + serializedItemName + ":" + serializedMetadata + ":" + serializedWildcard;
    }

    public static FilterRule deserialize(String data) {
        if (data == null) {
            return null;
        }
        String trimmed = data.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Accept vanilla "modid:item@meta" input from users/configs.
        String[] atSplit = trimmed.split("@", -1);
        if (atSplit.length == 2) {
            FilterRule base = deserialize(atSplit[0]);
            if (base == null) {
                return null;
            }
            int metadata = ANY_METADATA;
            String metaRaw = atSplit[1].trim();
            if (!metaRaw.isEmpty() && !ANY.equals(metaRaw)) {
                metadata = Integer.parseInt(metaRaw);
            }
            return new FilterRule(base.modId, base.itemName, metadata, base.useWildcard);
        }

        String[] parts = trimmed.split(":", -1);
        if (parts.length == 1) {
            String itemName = normalizePart(parts[0]);
            return new FilterRule(ANY, itemName, ANY_METADATA, itemName != null && itemName.contains(ANY));
        }
        if (parts.length == 2) {
            String modId = normalizePart(parts[0]);
            String itemName = normalizePart(parts[1]);
            return new FilterRule(modId, itemName, ANY_METADATA, itemName != null && itemName.contains(ANY));
        }
        if (parts.length == 3 || parts.length == 4) {
            String modId = normalizePart(parts[0]);
            String itemName = normalizePart(parts[1]);

            int metadata = ANY_METADATA;
            String metadataRaw = parts[2].trim();
            if (!metadataRaw.isEmpty() && !ANY.equals(metadataRaw)) {
                metadata = Integer.parseInt(metadataRaw);
            }

            boolean useWildcard = itemName != null && itemName.contains(ANY);
            if (parts.length == 4) {
                String wildcardRaw = parts[3].trim();
                useWildcard = "1".equals(wildcardRaw) || "true".equalsIgnoreCase(wildcardRaw);
            }

            return new FilterRule(modId, itemName, metadata, useWildcard);
        }

        return null;
    }

    private static String normalizePart(String part) {
        if (part == null) {
            return null;
        }
        String trimmed = part.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (ANY.equals(trimmed)) {
            return ANY;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static Pattern buildItemNamePattern(String itemName, boolean useWildcard) {
        if (!useWildcard || itemName == null || !itemName.contains(ANY) || ANY.equals(itemName)) {
            return null;
        }
        String regex = Pattern.quote(itemName).replace("\\*", ".*");
        return Pattern.compile("^" + regex + "$");
    }

    private boolean matchesItemName(String actualItemName) {
        if (itemName == null || ANY.equals(itemName)) {
            return true;
        }
        if (actualItemName == null) {
            return false;
        }
        if (!useWildcard || !itemName.contains(ANY)) {
            return itemName.equals(actualItemName);
        }
        return itemNamePattern != null && itemNamePattern.matcher(actualItemName).matches();
    }

    private static boolean matchesPart(String rulePart, String actualPart, boolean wildcardEnabled) {
        if (rulePart == null || ANY.equals(rulePart)) {
            return true;
        }
        if (actualPart == null) {
            return false;
        }
        if (!wildcardEnabled || !rulePart.contains(ANY)) {
            return rulePart.equals(actualPart);
        }

        String regex = Pattern.quote(rulePart).replace("\\*", ".*");
        return Pattern.compile("^" + regex + "$").matcher(actualPart).matches();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FilterRule)) {
            return false;
        }
        FilterRule that = (FilterRule) other;
        return metadata == that.metadata
                && useWildcard == that.useWildcard
                && Objects.equals(modId, that.modId)
                && Objects.equals(itemName, that.itemName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemName, metadata, modId, useWildcard);
    }

    @Override
    public String toString() {
        return "FilterRule{" +
                "modId='" + modId + '\'' +
                ", itemName='" + itemName + '\'' +
                ", metadata=" + metadata +
                ", useWildcard=" + useWildcard +
                '}';
    }
}
