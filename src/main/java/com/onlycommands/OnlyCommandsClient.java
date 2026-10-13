package com.onlycommands;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Solo deja ejecutar los comandos de Carpet Mod y de HeadIndex.
 * Cualquier otro comando escrito en el chat se bloquea.
 *
 * La lista de comandos permitidos se guarda en config/onlycommands.txt
 * (un comando por linea, sin la barra). Puedes editarla cuando quieras.
 */
public class OnlyCommandsClient implements ClientModInitializer {

	private static final List<String> DEFAULT_ALLOWED = Arrays.asList(
		// HeadIndex (el comando real del mod es /head)
		"head",
		// Carpet Mod
		"carpet", "player", "tick", "log", "spawn", "distance", "info",
		"perimeterinfo", "profile", "script", "track", "draw", "counter",
		"ping", "c", "s"
	);

	// Espacios de nombres permitidos, por ejemplo /carpet:player
	private static final Set<String> ALLOWED_NAMESPACES = Set.of("carpet", "headindex");

	private final Set<String> allowed = new HashSet<>();

	@Override
	public void onInitializeClient() {
		loadConfig();

		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
			if (isAllowed(command)) {
				return true;
			}
			notifyBlocked(command);
			return false;
		});
	}

	private boolean isAllowed(String command) {
		String trimmed = command.trim();
		if (trimmed.isEmpty()) {
			return false;
		}

		String root = trimmed.split("\\s+")[0].toLowerCase(Locale.ROOT);

		int colon = root.indexOf(':');
		if (colon >= 0) {
			String namespace = root.substring(0, colon);
			root = root.substring(colon + 1);
			if (!ALLOWED_NAMESPACES.contains(namespace)) {
				return false;
			}
		}

		return allowed.contains(root);
	}

	private void notifyBlocked(String command) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			String root = command.trim().split("\\s+")[0];
			client.player.sendMessage(
				Text.literal("[OnlyCommands] Comando bloqueado: /" + root
					+ ". Solo se permiten los comandos de Carpet y HeadIndex (/head).")
					.formatted(Formatting.RED),
				false
			);
		}
	}

	private void loadConfig() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("onlycommands.txt");

		try {
			if (!Files.exists(file)) {
				Files.write(file, DEFAULT_ALLOWED);
			}
			for (String line : Files.readAllLines(file)) {
				String name = line.trim().toLowerCase(Locale.ROOT);
				if (name.startsWith("/")) {
					name = name.substring(1);
				}
				if (!name.isEmpty() && !name.startsWith("#")) {
					allowed.add(name);
				}
			}
		} catch (IOException e) {
			allowed.addAll(DEFAULT_ALLOWED);
		}

		if (allowed.isEmpty()) {
			allowed.addAll(DEFAULT_ALLOWED);
		}
	}
}
