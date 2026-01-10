package com.fenglingyubing.pickupfilter.client.input;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public final class KeyBindingManager {
    private KeyBindingManager() {
    }

    public static final KeyBinding CLEAR_DROPS_KEY = new KeyBinding(
            "key.pickupfilter.clear_drops",
            Keyboard.KEY_K,
            "key.categories.pickupfilter"
    );

    public static void registerKeyBindings() {
        ClientRegistry.registerKeyBinding(CLEAR_DROPS_KEY);
    }
}

