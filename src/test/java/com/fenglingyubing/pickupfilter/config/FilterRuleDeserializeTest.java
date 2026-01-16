package com.fenglingyubing.pickupfilter.config;

import org.junit.Assert;
import org.junit.Test;

public class FilterRuleDeserializeTest {

    @Test
    public void deserialize_returnsNullForInvalidMetadata() {
        Assert.assertNull(FilterRule.deserialize("minecraft:stone@abc"));
        Assert.assertNull(FilterRule.deserialize("minecraft:stone:abc"));
        Assert.assertNull(FilterRule.deserialize("minecraft:stone:1:maybe:extra"));
    }
}

