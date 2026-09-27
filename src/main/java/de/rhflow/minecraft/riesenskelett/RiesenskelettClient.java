package de.rhflow.minecraft.riesenskelett;

import de.rhflow.minecraft.riesenskelett.client.RiesenskelettRenderer;
import de.rhflow.minecraft.riesenskelett.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client-Einstiegspunkt: registriert den Renderer (nutzt das eingebaute Skelett-Modell). */
public class RiesenskelettClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.RIESENSKELETT, RiesenskelettRenderer::new);
	}
}
