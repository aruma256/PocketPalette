package com.github.aruma256.pocketpalette.client.keybinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;
import com.mojang.blaze3d.platform.InputConstants;

@ExtendWith(MinecraftBootstrapExtension.class)
class PaletteKeyTest {

    private static class DisabledPaletteKey extends PaletteKey {
        int releasedCount;

        DisabledPaletteKey() {
            super(InputConstants.KEY_R, Category.MISC);
            setDown(true);
            pressTime = 3;
            doubleTapTick = 4;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        protected void onKeyReleased() {
            super.onKeyReleased();
            releasedCount++;
        }
    }

    @Test
    void disabledScrollPassesThroughAndClearsHeldStateBeforeNextTick() {
        DisabledPaletteKey key = new DisabledPaletteKey();

        assertFalse(key.onScroll(1));
        assertFalse(key.onScroll(-1));

        assertEquals(1, key.releasedCount);
        assertEquals(0, key.pressTime);
        assertEquals(0, key.doubleTapTick);
        assertTrue(key.isDown());
    }

    @Test
    void disabledRenderClearsHeldStateBeforeNextTick() {
        DisabledPaletteKey key = new DisabledPaletteKey();

        key.onRenderHotbar(null, 0);

        assertEquals(1, key.releasedCount);
        assertEquals(0, key.pressTime);
        assertEquals(0, key.doubleTapTick);
    }
}
