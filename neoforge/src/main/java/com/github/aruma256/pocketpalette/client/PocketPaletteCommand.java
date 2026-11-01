package com.github.aruma256.pocketpalette.client;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class PocketPaletteCommand {

	private static void displayMessage(Component textComponent) {
		Minecraft.getInstance().getChatListener().handleSystemMessage(textComponent, false);
	}

	private static void displayError(String message) {
		displayMessage(Component.literal(ChatFormatting.RED + "PocketPalette: " + message));
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(PocketPalette.MODID)
			.then(Commands.literal("reload")
				.executes(context -> {executeReload(); return Command.SINGLE_SUCCESS;})
			)
		;
		dispatcher.register(builder);
	}

	private static void executeReload() {
		if (PaletteConfigManager.loadAllFromFile()) {
			displayMessage(Component.literal("PocketPalette: reload succeeded!"));
		} else {
			displayError("failed to reload config file");
		}
		PocketPaletteClient.showConfigLoadStatus();
	}
}
