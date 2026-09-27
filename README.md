# Riesenskelett-Mod

Eine kleine Minecraft-Java-Mod (Fabric, 1.21.1), gebaut von Papa und Sohn.
Sie fügt ein **20 Blöcke hohes Skelett** hinzu, auf dessen Kopf man reiten kann.
Es sieht aus wie das Original-Skelett – es nutzt das eingebaute Modell und die
Vanilla-Textur, nur zehnmal so groß.

## Was das Riesenskelett kann

| Aktion | Effekt |
| --- | --- |
| **Einfach stehen lassen** | Es läuft nicht weg, schaut sich nur um und dreht den Kopf zu Spielern. |
| **Rechtsklick** aufs Skelett | Du sitzt oben auf dem Kopf – 20 Blöcke über dem Boden. |
| **WASD + Maus** | Steuern. Es läuft mit der normalen Skelett-Animation, nur in Riesenschritten. |
| **Gegen eine Wand laufen** | Stufen bis **8 Blöcke** Höhe steigt es einfach hoch. |
| **Leertaste halten, loslassen** | Sprung (Sprungleiste wie beim Pferd) – voll aufgeladen bis **20 Blöcke** hoch. |
| **Sturz aus mehr als 50 Blöcken** | Beim Aufschlag gibt der Boden nach: die Blöcke darunter fliegen zur Seite weg und es bleibt ein Krater. Grundgestein und Truhen bleiben heil; mit `/gamerule mobGriefing false` gibt es nur den Knall. |
| **Schleichen** | Absteigen – du landest sicher neben dem Fuß. |

Weder Reiter noch Skelett nehmen Fallschaden. Das Skelett gibt es nur als
**Riesenskelett-Spawn-Ei** im Kreativ-Inventar (Reiter „Spawn-Eier"), es spawnt
nicht von selbst und verschwindet nie.

## Entwicklungsumgebung

Gleiches Setup wie bei der [Känguru-Mod](https://github.com/ronnyhartenstein/minecraft-mod-kaenguruh):
JDK 21, VS Code mit „Extension Pack for Java" und „Gradle for Java".

```sh
./gradlew runClient      # Minecraft mit der Mod starten
./gradlew build          # fertige .jar nach build/libs/
```

Alle Versionsnummern stehen in `gradle.properties`.

## Projektstruktur

```
src/main/java/de/rhflow/minecraft/riesenskelett/
  RiesenskelettMod.java                Einstiegspunkt (Server + Client)
  RiesenskelettClient.java             Einstiegspunkt Client (Renderer)
  registry/ModEntities.java            EntityType "riesenskelett" (20 hoch, 3 breit)
  registry/ModItems.java               Spawn-Ei
  entity/RiesenskelettEntity.java      Reiten, Steuern, Sprung, Krater, Absteigen
  client/RiesenskelettRenderer.java    Vanilla-Skelettmodell ×10

src/main/resources/
  assets/riesenskelett/…               Icon, Item-Modell, Sprachdateien (de/en)
  data/riesenskelett/loot_table/…      Drops: Knochenblöcke + Skelettschädel
```

## Zahlen zum Rumspielen

In `RiesenskelettEntity.java`:

- `FULL_JUMP_VELOCITY` – Sprungkraft (2.0 ≈ 20 Blöcke)
- `CRATER_FALL_DISTANCE` – ab welcher Fallhöhe der Krater entsteht
- `MAX_CRATER_BLOCKS` – wie viele Blöcke höchstens wegfliegen
- `LIMB_SWING_PER_BLOCK` – wie schnell die Beine schwingen
- `GENERIC_MOVEMENT_SPEED` / `GENERIC_STEP_HEIGHT` in `createRiesenskelettAttributes()`
