package org.teacon.slides.util;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class RegistryServer {

	public static <V extends CustomPacketPayload> void registerCodec(
			CustomPacketPayload.Type<V> id,
			final StreamMemberEncoder<RegistryFriendlyByteBuf, V> encoder,
			final StreamDecoder<RegistryFriendlyByteBuf, V> decoder
	) {
//#if MC >= 26_00_00
		//$$ PayloadTypeRegistry.serverboundPlay().register(id, StreamCodec.ofMember(encoder, decoder));
		//$$ PayloadTypeRegistry.clientboundPlay().register(id, StreamCodec.ofMember(encoder, decoder));
//#else
		PayloadTypeRegistry.playC2S().register(id, StreamCodec.ofMember(encoder, decoder));
		PayloadTypeRegistry.playS2C().register(id, StreamCodec.ofMember(encoder, decoder));
//#endif
	}

	public static <V extends CustomPacketPayload> void registerNetworkReceiver(
			CustomPacketPayload.Type<V> id,
			ServerPlayNetworking.PlayPayloadHandler<V> handler
	) {
		ServerPlayNetworking.registerGlobalReceiver(id, handler);
	}

	public static <V extends CustomPacketPayload> void sendToPlayer(ServerPlayer player, V payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
