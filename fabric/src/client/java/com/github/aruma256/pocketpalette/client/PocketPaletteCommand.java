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
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PocketPaletteCommand implements ClientCommandRegistrationCallback {

	private static void displayMessage(Component textComponent) {
		Minecraft.getInstance().getChatListener().handleSystemMessage(textComponent, false);
	}

	private static void displayError(String message) {
		displayMessage(Component.literal(ChatFormatting.RED + "PocketPalette: " + message));
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

	private void executeAdd(int groupIndex) {
		Minecraft mc = Minecraft.getInstance();
		List<ItemState> cycle = new ArrayList<>();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack itemStack = mc.player.getInventory().getItem(i);
			if (itemStack.isEmpty()) {
				break;
			}

			if (ItemPalette.containsExactItem(itemStack, groupIndex)) {
				ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
				displayError(String.format("'%s' already exists (slot %d)", id, i + 1));
				return;
			}

			cycle.add(new ItemState(itemStack));
		}

		if (cycle.isEmpty()) {
			displayError("Hotbar is empty.");
			return;
		}

		if (cycle.size() < 2) {
			displayError("Need at least 2 items to create a cycle.");
			return;
		}

		PocketPalette.LOGGER.debug("adding a new item-group from /pocketpalette command");

		boolean succeeded = PaletteConfigManager.tryToAddItemGroup(cycle, groupIndex);
		if (succeeded) {
			displayMessage(Component.literal(String.format("PocketPalette: added %d items to group %d", cycle.size(), groupIndex + 1)));
		} else {
			displayError("failed to add items");
		}
	}

	private void executeReload() {
		if (PaletteConfigManager.loadAllFromFile()) {
			displayMessage(Component.literal("PocketPalette: reload succeeded!"));
		} else {
			displayError("failed to reload config file");
		}
		PocketPaletteClient.showErrorLogToChat();
	}
}
