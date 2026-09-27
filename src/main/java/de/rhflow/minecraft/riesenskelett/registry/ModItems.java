package de.rhflow.minecraft.riesenskelett.registry;

import de.rhflow.minecraft.riesenskelett.RiesenskelettMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Items der Mod – aktuell nur das Riesenskelett-Spawn-Ei (Farben wie beim Skelett-Ei). */
public final class ModItems {
	private ModItems() {}

	public static final Item RIESENSKELETT_SPAWN_EGG = register("riesenskelett_spawn_egg",
			new SpawnEggItem(ModEntities.RIESENSKELETT, 0xC1C1C1, 0x494949, new Item.Settings()));

	private static Item register(String name, Item item) {
		return Registry.register(Registries.ITEM, Identifier.of(RiesenskelettMod.MOD_ID, name), item);
	}

	public static void register() {
		// Spawn-Ei in den Kreativ-Reiter "Spawn-Eier" einsortieren.
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
				.register(entries -> entries.add(RIESENSKELETT_SPAWN_EGG));
	}
}
