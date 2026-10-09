package com.github.aruma256.pocketpalette.client;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

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
			.then(literal("reload")
				.executes(context -> {executeReload(); return Command.SINGLE_SUCCESS;})
			)
		;
		dispatcher.register(builder);
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String string) {
		return ClientCommands.literal(string);
	}

	private void executeReload() {
		if (PaletteConfigManager.loadAllFromFile()) {
			displayMessage(Component.literal("PocketPalette: reload succeeded!"));
		} else {
			displayError("failed to reload config file");
		}
		PocketPaletteClient.showConfigLoadStatus();
	}
}
