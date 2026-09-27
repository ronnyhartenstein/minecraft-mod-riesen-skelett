package de.rhflow.minecraft.riesenskelett;

import de.rhflow.minecraft.riesenskelett.entity.RiesenskelettEntity;
import de.rhflow.minecraft.riesenskelett.network.SchiessenPayload;
import de.rhflow.minecraft.riesenskelett.registry.ModEntities;
import de.rhflow.minecraft.riesenskelett.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt der Mod (läuft auf Server und Client).
 * Registriert das Riesenskelett, sein Spawn-Ei und die Standard-Attribute.
 * Es spawnt nicht natürlich – nur per Spawn-Ei.
 */
public class RiesenskelettMod implements ModInitializer {
	public static final String MOD_ID = "riesenskelett";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		ModItems.register();

		FabricDefaultAttributeRegistry.register(ModEntities.RIESENSKELETT, RiesenskelettEntity.createRiesenskelettAttributes());

		// Linksklick des Reiters kommt als Paket vom Client → Skelett schießt einen Riesenpfeil.
		PayloadTypeRegistry.playC2S().register(SchiessenPayload.ID, SchiessenPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SchiessenPayload.ID, (payload, context) -> {
			if (context.player().getVehicle() instanceof RiesenskelettEntity riesenskelett) {
				riesenskelett.shootRiesenpfeil(context.player());
			}
		});

		LOGGER.info("Riesenskelett-Mod geladen. Kopf einziehen!");
	}
}
