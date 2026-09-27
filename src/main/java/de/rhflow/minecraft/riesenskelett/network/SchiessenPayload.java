package de.rhflow.minecraft.riesenskelett.network;

import de.rhflow.minecraft.riesenskelett.RiesenskelettMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client → Server: "Reiter hat links geklickt, Riesenskelett soll schießen". Hat keinen Inhalt. */
public record SchiessenPayload() implements CustomPayload {
	public static final CustomPayload.Id<SchiessenPayload> ID =
			new CustomPayload.Id<>(Identifier.of(RiesenskelettMod.MOD_ID, "schiessen"));
	public static final PacketCodec<RegistryByteBuf, SchiessenPayload> CODEC = PacketCodec.unit(new SchiessenPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
