package org.teacon.slides.util;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.codec.ValueFirstEncoder;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public class RegistryServer {

	public static <V extends CustomPayload> void registerCodec(
			CustomPayload.Id<V> id,
			final ValueFirstEncoder<RegistryByteBuf, V> encoder,
			final PacketDecoder<RegistryByteBuf, V> decoder
	) {
		PayloadTypeRegistry.playC2S().register(id, PacketCodec.of(encoder, decoder));
		PayloadTypeRegistry.playS2C().register(id, PacketCodec.of(encoder, decoder));
	}

	public static <V extends CustomPayload> void registerNetworkReceiver(
			CustomPayload.Id<V> id,
			ServerPlayNetworking.PlayPayloadHandler<V> handler
	) {
		ServerPlayNetworking.registerGlobalReceiver(id, handler);
	}

	public static <V extends CustomPayload> void sendToPlayer(ServerPlayerEntity player, V payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
