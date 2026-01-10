package com.fenglingyubing.pickupfilter.client.input;

import net.minecraft.client.settings.KeyBinding;
import org.junit.Test;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class KeyBindingManagerTest {
    @Test
    public void clearDropsKey_isK_andHasTranslationKeys() {
        assertEquals("key.pickupfilter.clear_drops", KeyBindingManager.CLEAR_DROPS_KEY.getKeyDescription());
        assertEquals("key.categories.pickupfilter", KeyBindingManager.CLEAR_DROPS_KEY.getKeyCategory());
        assertEquals(Keyboard.KEY_K, KeyBindingManager.CLEAR_DROPS_KEY.getKeyCode());
    }

    @Test
    public void toggleModeKey_isO_andHasTranslationKeys() {
        assertEquals("key.pickupfilter.toggle_mode", KeyBindingManager.TOGGLE_MODE_KEY.getKeyDescription());
        assertEquals("key.categories.pickupfilter", KeyBindingManager.TOGGLE_MODE_KEY.getKeyCategory());
        assertEquals(Keyboard.KEY_O, KeyBindingManager.TOGGLE_MODE_KEY.getKeyCode());
    }

    @Test
    public void registerKeyBindings_delegatesToRegistrar() {
        List<KeyBinding> registered = new ArrayList<>();
        KeyBindingManager.registerKeyBindings(registered::add);
        assertEquals(3, registered.size());
        assertSame(KeyBindingManager.CLEAR_DROPS_KEY, registered.get(0));
        assertSame(KeyBindingManager.TOGGLE_MODE_KEY, registered.get(1));
        assertSame(KeyBindingManager.OPEN_CONFIG_KEY, registered.get(2));
    }

    @Test
    public void consumeClearDropsKeyPress_consumesTicks() {
        AtomicInteger calls = new AtomicInteger();
        assertTrue(KeyBindingManager.consumeClearDropsKeyPress(keyBinding -> {
            assertSame(KeyBindingManager.CLEAR_DROPS_KEY, keyBinding);
            return calls.getAndIncrement() == 0;
        }));

        assertFalse(KeyBindingManager.consumeClearDropsKeyPress(keyBinding -> {
            assertSame(KeyBindingManager.CLEAR_DROPS_KEY, keyBinding);
            return calls.getAndIncrement() == 0;
        }));
    }

    @Test
    public void consumeToggleModeKeyPress_consumesTicks() {
        AtomicInteger calls = new AtomicInteger();
        assertTrue(KeyBindingManager.consumeToggleModeKeyPress(keyBinding -> {
            assertSame(KeyBindingManager.TOGGLE_MODE_KEY, keyBinding);
            return calls.getAndIncrement() == 0;
        }));

        assertFalse(KeyBindingManager.consumeToggleModeKeyPress(keyBinding -> {
            assertSame(KeyBindingManager.TOGGLE_MODE_KEY, keyBinding);
            return calls.getAndIncrement() == 0;
        }));
    }

    @Test
    public void openConfigKey_isP_andHasTranslationKeys() {
        assertEquals("key.pickupfilter.open_config", KeyBindingManager.OPEN_CONFIG_KEY.getKeyDescription());
        assertEquals("key.categories.pickupfilter", KeyBindingManager.OPEN_CONFIG_KEY.getKeyCategory());
        assertEquals(Keyboard.KEY_P, KeyBindingManager.OPEN_CONFIG_KEY.getKeyCode());
    }

    @Test
    public void matchesKeyBindingPress_matchesKeyCode() {
        assertTrue(KeyBindingManager.matchesKeyBindingPress(KeyBindingManager.TOGGLE_MODE_KEY, Keyboard.KEY_O, (char) 0));
        assertTrue(KeyBindingManager.matchesKeyBindingPress(KeyBindingManager.CLEAR_DROPS_KEY, Keyboard.KEY_K, (char) 0));
        assertTrue(KeyBindingManager.matchesKeyBindingPress(KeyBindingManager.OPEN_CONFIG_KEY, Keyboard.KEY_P, (char) 0));
        assertFalse(KeyBindingManager.matchesKeyBindingPress(KeyBindingManager.TOGGLE_MODE_KEY, Keyboard.KEY_P, (char) 0));
    }

    @Test
    public void matchesKeyBindingPress_matchesLetterByCharWhenEventKeyIsNone() {
        assertTrue(KeyBindingManager.matchesKeyBindingPress(KeyBindingManager.TOGGLE_MODE_KEY, Keyboard.KEY_NONE, 'o'));
        assertTrue(KeyBindingManager.matchesKeyBindingPress(KeyBindingManager.TOGGLE_MODE_KEY, Keyboard.KEY_NONE, 'O'));
        assertFalse(KeyBindingManager.matchesKeyBindingPress(KeyBindingManager.TOGGLE_MODE_KEY, Keyboard.KEY_NONE, 'p'));
    }

    @Test
    public void consumeOpenConfigKeyPress_consumesTicks() {
        AtomicInteger calls = new AtomicInteger();
        assertTrue(KeyBindingManager.consumeOpenConfigKeyPress(keyBinding -> {
            assertSame(KeyBindingManager.OPEN_CONFIG_KEY, keyBinding);
            return calls.getAndIncrement() == 0;
        }));

        assertFalse(KeyBindingManager.consumeOpenConfigKeyPress(keyBinding -> {
            assertSame(KeyBindingManager.OPEN_CONFIG_KEY, keyBinding);
            return calls.getAndIncrement() == 0;
        }));
    }
}
