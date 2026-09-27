package de.rhflow.minecraft.riesenskelett;

import de.rhflow.minecraft.riesenskelett.client.RiesenpfeilRenderer;
import de.rhflow.minecraft.riesenskelett.client.RiesenskelettRenderer;
import de.rhflow.minecraft.riesenskelett.entity.RiesenskelettEntity;
import de.rhflow.minecraft.riesenskelett.network.SchiessenPayload;
import de.rhflow.minecraft.riesenskelett.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

/** Client-Einstiegspunkt: Renderer registrieren und Linksklick beim Reiten in einen Schuss umwandeln. */
public class RiesenskelettClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.RIESENSKELETT, RiesenskelettRenderer::new);
		EntityRendererRegistry.register(ModEntities.RIESENPFEIL, RiesenpfeilRenderer::new);

		// Beim Reiten ersetzt Linksklick das Schlagen/Abbauen: das Skelett schießt stattdessen.
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (!(player.getVehicle() instanceof RiesenskelettEntity)) {
				return false;
			}
			if (clickCount > 0) {
				ClientPlayNetworking.send(new SchiessenPayload());
			}
			return true;
		});
	}
}
