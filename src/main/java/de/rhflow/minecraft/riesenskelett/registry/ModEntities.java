package de.rhflow.minecraft.riesenskelett.registry;

import de.rhflow.minecraft.riesenskelett.RiesenskelettMod;
import de.rhflow.minecraft.riesenskelett.entity.RiesenskelettEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Alle EntityTypes der Mod. */
public final class ModEntities {
	private ModEntities() {}

	/**
	 * 20 Blöcke hoch. Die Hitbox ist nur 3 breit (ein echtes ×10-Skelett wäre 6 breit),
	 * damit es nicht an jedem Baum hängen bleibt. Der Reiter sitzt ganz oben auf dem Kopf.
	 */
	public static final EntityType<RiesenskelettEntity> RIESENSKELETT = register("riesenskelett",
			EntityType.Builder.<RiesenskelettEntity>create(RiesenskelettEntity::new, SpawnGroup.MISC)
					.dimensions(3.0f, RiesenskelettEntity.HEIGHT)
					.eyeHeight(18.5f)
					.passengerAttachments(RiesenskelettEntity.HEIGHT)
					.maxTrackingRange(16));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		return Registry.register(Registries.ENTITY_TYPE, Identifier.of(RiesenskelettMod.MOD_ID, name), builder.build(name));
	}

	/** Sorgt dafür, dass diese Klasse (und damit die Felder oben) geladen wird. */
	public static void register() {
		RiesenskelettMod.LOGGER.debug("Registriere Entities");
	}
}
