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

    public static final KeyBinding TOGGLE_MODE_KEY = new KeyBinding(
            "key.pickupfilter.toggle_mode",
            Keyboard.KEY_O,
            "key.categories.pickupfilter"
    );

    public static final KeyBinding OPEN_CONFIG_KEY = new KeyBinding(
            "key.pickupfilter.open_config",
            Keyboard.KEY_P,
            "key.categories.pickupfilter"
    );

    public static void registerKeyBindings() {
        registerKeyBindings(ClientRegistry::registerKeyBinding);
    }

    static void registerKeyBindings(KeyBindingRegistrar registrar) {
        registrar.register(CLEAR_DROPS_KEY);
        registrar.register(TOGGLE_MODE_KEY);
        registrar.register(OPEN_CONFIG_KEY);
    }

    public static boolean consumeClearDropsKeyPress() {
        return consumeClearDropsKeyPress(KeyBinding::isPressed);
    }

    static boolean consumeClearDropsKeyPress(KeyPressReader keyPressReader) {
        return keyPressReader.isPressed(CLEAR_DROPS_KEY);
    }

    public static boolean consumeToggleModeKeyPress() {
        return consumeToggleModeKeyPress(KeyBinding::isPressed);
    }

    static boolean consumeToggleModeKeyPress(KeyPressReader keyPressReader) {
        return keyPressReader.isPressed(TOGGLE_MODE_KEY);
    }

    public static boolean consumeOpenConfigKeyPress() {
        return consumeOpenConfigKeyPress(KeyBinding::isPressed);
    }

    static boolean consumeOpenConfigKeyPress(KeyPressReader keyPressReader) {
        return keyPressReader.isPressed(OPEN_CONFIG_KEY);
    }

    public static boolean matchesKeyBindingPress(KeyBinding binding, int eventKey, char eventChar) {
        if (binding == null) {
            return false;
        }

        int keyCode = binding.getKeyCode();
        if (keyCode == Keyboard.KEY_NONE) {
            return false;
        }

        if (eventKey == keyCode) {
            return true;
        }

        if (eventKey != Keyboard.KEY_NONE || eventChar == 0) {
            return false;
        }

        String keyName = Keyboard.getKeyName(keyCode);
        if (keyName == null || keyName.length() != 1) {
            return false;
        }

        char expected = Character.toLowerCase(keyName.charAt(0));
        char actual = Character.toLowerCase(eventChar);
        return expected == actual;
    }
}
