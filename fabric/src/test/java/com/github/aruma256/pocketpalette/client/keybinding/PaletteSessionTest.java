package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;
import com.github.aruma256.pocketpalette.client.palette.ItemState;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
class PaletteSessionTest {

	private PaletteSession session;
	private ItemStack[] slots;
	private List<Integer> swappedSlots;
	private int selectedSlot;

	@BeforeEach
	void setUp() {
		session = new PaletteSession();
		slots = new ItemStack[Inventory.INVENTORY_SIZE];
		Arrays.fill(slots, ItemStack.EMPTY);
		swappedSlots = new ArrayList<>();
		selectedSlot = 0;
	}

	private static ItemState entry(Item item) {
		return new ItemState(new ItemStack(item));
	}

	private void rebuild(Item... items) {
		session.rebuild(slot -> slots[slot], selectedSlot,
				Arrays.stream(items).map(PaletteSessionTest::entry).toList());
	}

	private boolean hasInventoryChanged() {
		return session.hasInventoryChanged(slot -> slots[slot], selectedSlot);
	}

	/** Simulate the local inventory prediction performed by a successful SWAP. */
	private boolean swap() {
		return session.swapToFirstCandidate(slot -> slots[slot], target -> {
			swappedSlots.add(target);
			ItemStack held = slots[selectedSlot];
			slots[selectedSlot] = slots[target];
			slots[target] = held;
			return true;
		});
	}

	private List<Integer> candidateCounts() {
		return session.getStacks().stream().map(ItemStack::getCount).toList();
	}

	@Test
	void candidatesStartWithHeldStackAndFollowCycleAndStackOrder() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.STONE, 8);
		slots[10] = new ItemStack(Items.STONE, 3);
		slots[11] = new ItemStack(Items.GRANITE, 2);
		slots[12] = new ItemStack(Items.DIRT, 1);
		rebuild(Items.STONE, Items.GRANITE, Items.ANDESITE);

		assertEquals(4, session.size());
		assertEquals(List.of(17, 3, 8, 2), candidateCounts());
		assertEquals(List.of(Items.STONE, Items.STONE, Items.STONE, Items.GRANITE),
				session.getStacks().stream().map(ItemStack::getItem).toList());
		assertFalse(swap(), "The initial first candidate is already in hand");
		assertTrue(swappedSlots.isEmpty());
	}

	@Test
	void overlappingBaseAndExactEntriesClaimEachPhysicalStackOnce() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.STONE, 3);
		slots[9].set(DataComponents.CUSTOM_NAME, Component.literal("Named stone"));
		slots[10] = new ItemStack(Items.STONE, 8);
		session.rebuild(slot -> slots[slot], selectedSlot,
				List.of(entry(Items.STONE), new ItemState(slots[9])));

		assertEquals(List.of(17, 3, 8), candidateCounts());
	}

	@Test
	void repeatedSwapsPreserveRingOrderAndTrackMovedStacks() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.STONE, 3);
		slots[10] = new ItemStack(Items.STONE, 8);
		rebuild(Items.STONE, Items.GRANITE);

		for (int count : List.of(3, 8, 17, 3, 8, 17)) {
			session.rotateBackward();
			assertTrue(swap());
			assertEquals(count, slots[0].getCount());
			assertEquals(count, session.getStacks().getFirst().getCount());
			assertFalse(hasInventoryChanged(), "Own swaps must not trigger re-sorting");
		}
		assertEquals(List.of(9, 10, 9, 10, 9, 10), swappedSlots);
		assertEquals(List.of(17, 3, 8), candidateCounts());
	}

	@Test
	void reversingDirectionSwapsBackToTheOriginalPhysicalStack() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.STONE, 3);
		slots[10] = new ItemStack(Items.STONE, 8);
		rebuild(Items.STONE, Items.GRANITE);

		session.rotateForward();
		assertTrue(swap());
		assertEquals(8, slots[0].getCount());
		session.rotateBackward();
		assertTrue(swap());
		assertEquals(17, slots[0].getCount());
		assertEquals(List.of(10, 10), swappedSlots);
		assertEquals(List.of(17, 3, 8), candidateCounts());
	}

	@Test
	void failedSwapKeepsHandAndSlotTrackingWhileRetainingRotation() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.GRANITE, 3);
		rebuild(Items.STONE, Items.GRANITE);

		session.rotateBackward();
		assertFalse(session.swapToFirstCandidate(slot -> slots[slot], target -> {
			assertEquals(9, target);
			return false;
		}));
		assertEquals(List.of(3, 17), candidateCounts());
		assertEquals(17, slots[0].getCount());
		assertFalse(hasInventoryChanged());
		session.rotateBackward();
		assertFalse(swap(), "Returning to the actual hand must not send a SWAP");
		assertTrue(swappedSlots.isEmpty());
		session.rotateBackward();
		assertTrue(swap());
		session.rotateBackward();
		assertTrue(swap());
		assertEquals(List.of(9, 9), swappedSlots);
		assertEquals(17, slots[0].getCount());
	}

	@Test
	void staleTargetIsRejectedBeforeCallingSwapper() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.GRANITE, 3);
		rebuild(Items.STONE, Items.GRANITE);
		session.rotateBackward();

		ItemStack original = slots[9].copy();
		slots[9].set(DataComponents.CUSTOM_NAME, Component.literal("Changed"));
		assertFalse(swap());
		assertTrue(hasInventoryChanged());
		slots[9] = ItemStack.EMPTY;
		assertFalse(swap());
		assertTrue(swappedSlots.isEmpty());

		slots[9] = original;
		assertTrue(swap(), "Rejected attempts must not change hand or slot tracking");
		assertEquals(List.of(9), swappedSlots);
	}

	@Test
	void changesInNonCandidateSlotsCanAddOrRemoveCandidates() {
		slots[0] = new ItemStack(Items.STONE, 17);
		rebuild(Items.STONE, Items.GRANITE);
		assertTrue(session.isEmpty());
		assertFalse(hasInventoryChanged());

		slots[35] = new ItemStack(Items.GRANITE, 3);
		assertTrue(hasInventoryChanged());
		rebuild(Items.STONE, Items.GRANITE);
		assertEquals(2, session.size());
		assertFalse(hasInventoryChanged());

		slots[35] = ItemStack.EMPTY;
		assertTrue(hasInventoryChanged());
		rebuild(Items.STONE, Items.GRANITE);
		assertTrue(session.isEmpty());
	}

	@Test
	void countAndComponentMutationsAreComparedAgainstCopies() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.GRANITE, 3);
		rebuild(Items.STONE, Items.GRANITE);

		slots[9].shrink(1);
		assertTrue(hasInventoryChanged());
		assertEquals(List.of(17, 3), candidateCounts());
		rebuild(Items.STONE, Items.GRANITE);
		assertEquals(List.of(17, 2), candidateCounts());

		slots[9].set(DataComponents.CUSTOM_NAME, Component.literal("Changed"));
		assertTrue(hasInventoryChanged());
	}

	@Test
	void serverCorrectionAfterOwnSwapRequiresRebuild() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.GRANITE, 3);
		rebuild(Items.STONE, Items.GRANITE);
		session.rotateBackward();
		assertTrue(swap());
		assertFalse(hasInventoryChanged());

		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.GRANITE, 3);
		assertTrue(hasInventoryChanged());
		rebuild(Items.STONE, Items.GRANITE);
		assertFalse(swap());
		session.rotateBackward();
		assertTrue(swap());
		assertEquals(List.of(9, 9), swappedSlots);
	}

	@Test
	void selectedSlotChangeRebuildsFromNewHandAndClearsRingForEmptyHand() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[1] = new ItemStack(Items.GRANITE, 3);
		rebuild(Items.STONE, Items.GRANITE);

		selectedSlot = 1;
		assertTrue(hasInventoryChanged());
		rebuild(Items.GRANITE, Items.STONE);
		assertEquals(List.of(3, 17), candidateCounts());
		session.rotateBackward();
		assertTrue(swap());
		assertEquals(List.of(0), swappedSlots);
		assertEquals(17, slots[1].getCount());
		assertFalse(hasInventoryChanged());

		selectedSlot = 2;
		assertTrue(hasInventoryChanged());
		rebuild(Items.STONE, Items.GRANITE);
		assertTrue(session.isEmpty());
		assertFalse(swap());
	}

	@Test
	void missingCycleStillTracksInventoryAndClearResetsSession() {
		slots[0] = new ItemStack(Items.STONE, 17);
		session.rebuild(slot -> slots[slot], selectedSlot, null);
		assertTrue(session.isEmpty());
		assertFalse(hasInventoryChanged());
		slots[9] = new ItemStack(Items.GRANITE, 3);
		assertTrue(hasInventoryChanged());
		rebuild(Items.STONE, Items.GRANITE);

		session.clear();
		session.rotateForward();
		session.rotateBackward();
		assertTrue(session.isEmpty());
		assertTrue(hasInventoryChanged());
		assertFalse(swap());
		assertTrue(swappedSlots.isEmpty());
	}

	@Test
	void renderingCopiesCannotChangeCandidateMatching() {
		slots[0] = new ItemStack(Items.STONE, 17);
		slots[9] = new ItemStack(Items.GRANITE, 3);
		rebuild(Items.STONE, Items.GRANITE);

		List<ItemStack> display = session.getStacks();
		display.getFirst().setCount(1);
		display.get(1).set(DataComponents.CUSTOM_NAME, Component.literal("Display only"));
		assertEquals(List.of(17, 3), candidateCounts());
		session.rotateBackward();
		assertTrue(swap());
		assertEquals(3, slots[0].getCount());
	}
}
