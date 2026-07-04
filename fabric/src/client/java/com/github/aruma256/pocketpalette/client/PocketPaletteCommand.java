package com.github.aruma256.pocketpalette.client;

import java.util.ArrayList;
import java.util.List;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public class PocketPaletteCommand implements ClientCommandRegistrationCallback {

	private static void displayMessage(Component textComponent) {
		Minecraft.getInstance().getChatListener().handleSystemMessage(textComponent, false);
	}

	@Override
	public void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
		LiteralArgumentBuilder<FabricClientCommandSource> builder = literal(PocketPalette.MODID)
			.then(literal("add")
				.then(literal("1")
					.executes(context -> {executeAdd(0); return Command.SINGLE_SUCCESS;}))
				.then(literal("2")
					.executes(context -> {executeAdd(1); return Command.SINGLE_SUCCESS;}))
				)
			.then(literal("reload")
				.executes(context -> {executeReload(); return Command.SINGLE_SUCCESS;})
			)
		;
		dispatcher.register(builder);
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String string) {
		return ClientCommandManager.literal(string);
	}

	private void executeAdd(int groupIndex) throws PocketPaletteCommandRuntimeException {
		Minecraft mc = Minecraft.getInstance();
		List<ItemState> cycle = new ArrayList<>();
		int count = 0;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack itemStack = mc.player.getInventory().getItem(i);
			if (itemStack.isEmpty()) {
				break;
			}
			Item item = itemStack.getItem();
			if (item == Items.AIR) {
				throw new PocketPaletteCommandRuntimeException(String.format("Failed to get item instance. (%d)", i + 1));
			}

			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			String name = id.toString();

			if (ItemPalette.containsExactItem(itemStack, groupIndex)) {
				throw new PocketPaletteCommandRuntimeException(String.format("'%s' already exists (slot %d)", name, i + 1));
			}

			cycle.add(new ItemState(itemStack));
			count++;
		}

		if (cycle.isEmpty()) {
			throw new PocketPaletteCommandRuntimeException("Hotbar is empty.");
		}

		if (cycle.size() < 2) {
			throw new PocketPaletteCommandRuntimeException("Need at least 2 items to create a cycle.");
		}

		PocketPalette.LOGGER.debug("adding a new item-group from /pocketpalette command");

		boolean succeeded = PaletteConfigManager.tryToAddItemGroup(cycle, groupIndex);
		if (succeeded) {
			displayMessage(Component.literal(String.format("PocketPalette: added %d items to group %d", count, groupIndex + 1)));
		} else {
			displayMessage(Component.literal(ChatFormatting.RED + "PocketPalette: failed to add items"));
		}
	}

	private void executeReload() throws PocketPaletteCommandRuntimeException {
		try {
			boolean f = PaletteConfigManager.loadAllFromFile();
			if (!f) throw new PocketPaletteCommandRuntimeException("PocketPalette: failed to reload config file");
			displayMessage(Component.literal("PocketPalette: reload succeeded!"));
		} catch (PocketPaletteCommandRuntimeException e) {
			displayMessage(Component.literal(ChatFormatting.RED + e.getMessage()));
		}
		PocketPaletteClient.showErrorLogToChat();
	}

	// TODO: 例外を使わずにエラーハンドリングを行う設計に変更する
	@SuppressWarnings("serial")
	private static final class PocketPaletteCommandRuntimeException extends RuntimeException {
		public PocketPaletteCommandRuntimeException(String message) {
			super(message);
		}
	}
}
