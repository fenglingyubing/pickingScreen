package com.fenglingyubing.pickupfilter.config;

import org.junit.Test;
import org.quicktheories.WithQuickTheories;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.SourceDSL;

public class FilterRulePropertyTest implements WithQuickTheories {

    @Test
    public void property7_matchingConsistentAfterSerialize() {
        qt().withExamples(250)
                .forAll(rules(), registryNames(), SourceDSL.integers().between(0, 15))
                .check((rule, registryName, metadata) -> {
                    FilterRule reloaded = FilterRule.deserialize(rule.serialize());
                    if (reloaded == null) {
                        return false;
                    }
                    return reloaded.equals(rule)
                            && (rule.matches(registryName, metadata) == reloaded.matches(registryName, metadata));
                });
    }

    private static Gen<FilterRule> rules() {
        Gen<String> randomModId = SourceDSL.strings()
                .betweenCodePoints('a', 'z')
                .ofLengthBetween(1, 8);
        Gen<String> modId = SourceDSL.integers()
                .between(0, 9)
                .zip(randomModId, (pick, generated) -> pick == 0 ? FilterRule.ANY : generated);

        Gen<Boolean> wildcardEnabled = SourceDSL.booleans().all();
        Gen<String> baseItemName = SourceDSL.strings()
                .betweenCodePoints('a', 'z')
                .ofLengthBetween(1, 12);
        Gen<String> itemName = wildcardEnabled.zip(baseItemName, (wildcard, base) -> {
            if (!wildcard) {
                return base;
            }
            int starAt = Math.max(1, base.length() / 2);
            return base.substring(0, starAt) + FilterRule.ANY + base.substring(starAt);
        });

        Gen<Integer> metadata = SourceDSL.integers().between(FilterRule.ANY_METADATA, 15);

        return modId.zip(itemName, (pickedModId, pickedItemName) -> new Object[]{pickedModId, pickedItemName})
                .zip(metadata, (pair, pickedMetadata) -> new Object[]{pair[0], pair[1], pickedMetadata})
                .zip(wildcardEnabled, (triple, pickedWildcardEnabled) ->
                        new FilterRule(
                                (String) triple[0],
                                (String) triple[1],
                                (Integer) triple[2],
                                pickedWildcardEnabled
                        ));
    }

    private static Gen<String> registryNames() {
        Gen<String> modId = SourceDSL.strings()
                .betweenCodePoints('a', 'z')
                .ofLengthBetween(1, 8);
        Gen<String> itemName = SourceDSL.strings()
                .betweenCodePoints('a', 'z')
                .ofLengthBetween(1, 12);
        return modId.zip(itemName, (pickedModId, pickedItemName) -> pickedModId + ":" + pickedItemName);
    }
}
