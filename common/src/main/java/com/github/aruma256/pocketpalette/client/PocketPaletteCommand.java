package com.github.aruma256.pocketpalette.client;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.network.chat.Component;

public final class PocketPaletteCommand {

	private PocketPaletteCommand() {}

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
			PocketPaletteClient.showMessage(Component.translatable("pocketpalette.command.reload_succeeded"));
		} else {
			PocketPaletteClient.showConfigLoadStatus();
		}
	}
}
