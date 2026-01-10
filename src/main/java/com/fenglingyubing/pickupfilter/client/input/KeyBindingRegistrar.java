package com.fenglingyubing.pickupfilter.client.input;

import net.minecraft.client.settings.KeyBinding;

@FunctionalInterface
public interface KeyBindingRegistrar {
    void register(KeyBinding keyBinding);
}

