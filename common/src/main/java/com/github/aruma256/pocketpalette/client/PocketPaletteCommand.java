package com.github.aruma256.pocketpalette.client;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class PocketPaletteCommand {

	private PocketPaletteCommand() {}

	private static void displayMessage(Component textComponent) {
		Minecraft.getInstance().getChatListener().handleSystemMessage(textComponent, false);
	}

	private static void displayError(String message) {
		displayMessage(Component.literal(ChatFormatting.RED + "PocketPalette: " + message));
	}

	/** Generic because each loader has its own client command source type. */
	public static <S> void register(CommandDispatcher<S> dispatcher) {
		LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.<S>literal(PocketPalette.MODID)
			.then(LiteralArgumentBuilder.<S>literal("reload")
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
