package com.github.aruma256.pocketpalette.client.compat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

@ExtendWith(MinecraftBootstrapExtension.class)
class LotTweaksCompatTest {

    private static int keyCounter;

    private static KeyMapping pocketKey() {
        return new KeyMapping("key.pocketpalette.compat.test" + keyCounter++, InputConstants.KEY_R, KeyMapping.Category.MISC);
    }

    private static KeyMapping lotTweaksKey(int keyCode) {
        return new KeyMapping("key.lottweaks.palette", keyCode, KeyMapping.Category.MISC);
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, false, false",
            "false, false, true, false",
            "false, true, false, false",
            "false, true, true, false",
            "true, false, false, false",
            "true, false, true, false",
            "true, true, false, false",
            "true, true, true, true"
    })
    void disablesOnlyWhenAllThreeConditionsHold(boolean loaded, boolean creative, boolean sameKey, boolean disabled) {
        KeyMapping pocket = pocketKey();
        KeyMapping lotTweaks = lotTweaksKey(sameKey ? InputConstants.KEY_R : InputConstants.KEY_V);

        assertEquals(disabled, new LotTweaksCompat(loaded)
                .shouldDisablePaletteKey(creative, pocket, new KeyMapping[]{lotTweaks}));
    }

    @Test
    void missingPaletteMappingAndOtherLotTweaksKeysDoNotDisableInput() {
        LotTweaksCompat compat = new LotTweaksCompat(true);
        KeyMapping pocket = pocketKey();
        KeyMapping other = new KeyMapping("key.lottweaks.smartpick", InputConstants.KEY_R, KeyMapping.Category.MISC);

        assertFalse(compat.shouldDisablePaletteKey(true, pocket, new KeyMapping[0]));
        assertFalse(compat.shouldDisablePaletteKey(true, pocket, new KeyMapping[]{other}));
    }

    @Test
    void rebindingEitherPaletteTakesEffectWithoutRestarting() {
        LotTweaksCompat compat = new LotTweaksCompat(true);
        KeyMapping pocket = pocketKey();
        KeyMapping lotTweaks = lotTweaksKey(InputConstants.KEY_R);
        KeyMapping[] registered = {lotTweaks};

        assertTrue(compat.shouldDisablePaletteKey(true, pocket, registered));
        lotTweaks.setKey(InputConstants.getKey("key.keyboard.v"));
        assertFalse(compat.shouldDisablePaletteKey(true, pocket, registered));
        pocket.setKey(InputConstants.getKey("key.keyboard.v"));
        assertTrue(compat.shouldDisablePaletteKey(true, pocket, registered));
        pocket.setKey(InputConstants.UNKNOWN);
        lotTweaks.setKey(InputConstants.UNKNOWN);
        assertFalse(compat.shouldDisablePaletteKey(true, pocket, registered));
    }

    @Test
    void bindingsMatchByInputTypeAndValue() {
        LotTweaksCompat compat = new LotTweaksCompat(true);
        KeyMapping pocket = pocketKey();
        KeyMapping lotTweaks = lotTweaksKey(InputConstants.KEY_R);
        KeyMapping[] registered = {lotTweaks};

        for (InputConstants.Type type : InputConstants.Type.values()) {
            pocket.setKey(type.getOrCreate(4));
            lotTweaks.setKey(type.getOrCreate(4));
            assertTrue(compat.shouldDisablePaletteKey(true, pocket, registered));
            lotTweaks.setKey(type.getOrCreate(5));
            assertFalse(compat.shouldDisablePaletteKey(true, pocket, registered));
            for (InputConstants.Type otherType : InputConstants.Type.values()) {
                if (otherType == type) continue;
                lotTweaks.setKey(otherType.getOrCreate(4));
                assertFalse(compat.shouldDisablePaletteKey(true, pocket, registered));
            }
        }
    }
}
