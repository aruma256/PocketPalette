package com.github.aruma256.pocketpalette.client.keybinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

@ExtendWith(MinecraftBootstrapExtension.class)
public class KeyBaseTest {

    private static int keyCounter = 0;

    /**
     * KeyBase subclass that records callback invocations. Each instance uses
     * a unique mapping name to avoid clashes in KeyMapping's static registry.
     */
    private static class TestKey extends KeyBase {
        int pressStartCount = 0;
        int whilePressedCount = 0;
        int releasedCount = 0;
        int doubleTapTickAtPressStart = -1;

        TestKey() {
            super("key.pocketpalette.test" + (keyCounter++), 82, Category.MISC);
        }

        @Override
        protected void onKeyPressStart() {
            pressStartCount++;
            doubleTapTickAtPressStart = this.doubleTapTick;
        }

        @Override
        protected void whilePressed() {
            whilePressedCount++;
        }

        @Override
        protected void onKeyReleased() {
            releasedCount++;
        }

        void tick(boolean down) {
            setDown(down);
            onEndTick(null);
        }

        void tick(boolean down, int times) {
            for (int i = 0; i < times; i++) {
                tick(down);
            }
        }
    }

    @Test
    public void testNoCallbacksWhileKeyIsUp() {
        TestKey key = new TestKey();

        key.tick(false, 3);

        assertEquals(0, key.pressStartCount);
        assertEquals(0, key.whilePressedCount);
        assertEquals(0, key.releasedCount);
    }

    @Test
    public void testPressStartFiresOnceAndWhilePressedEveryTick() {
        TestKey key = new TestKey();

        key.tick(true, 3);

        assertEquals(1, key.pressStartCount);
        assertEquals(3, key.whilePressedCount);
        assertEquals(0, key.releasedCount);
        assertEquals(3, key.pressTime);
    }

    @Test
    public void testReleaseFiresOnceAndResetsPressTime() {
        TestKey key = new TestKey();

        key.tick(true, 2);
        key.tick(false, 3);

        assertEquals(1, key.releasedCount);
        assertEquals(0, key.pressTime);
    }

    @Test
    public void testSecondPressFiresPressStartAgain() {
        TestKey key = new TestKey();

        key.tick(true, 2);
        key.tick(false);
        key.tick(true, 2);

        assertEquals(2, key.pressStartCount);
        assertEquals(1, key.releasedCount);
    }

    @Test
    public void testFirstPressSeesNoDoubleTap() {
        TestKey key = new TestKey();

        key.tick(true);

        assertEquals(0, key.doubleTapTickAtPressStart);
    }

    @Test
    public void testQuickRepressCountsAsDoubleTap() {
        TestKey key = new TestKey();

        key.tick(true);      // first press (arms doubleTapTick)
        key.tick(false, 2);  // short pause
        key.tick(true);      // re-press within the double-tap window

        assertTrue(key.doubleTapTickAtPressStart > 0,
                "re-press within window must see doubleTapTick > 0");
    }

    @Test
    public void testSlowRepressIsNotADoubleTap() {
        TestKey key = new TestKey();

        key.tick(true);      // first press
        key.tick(false, 6);  // pause longer than the double-tap window (5 ticks)
        key.tick(true);

        assertEquals(0, key.doubleTapTickAtPressStart);
    }
}
