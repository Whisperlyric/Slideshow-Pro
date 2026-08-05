package org.teacon.slides.util;

import java.util.function.Consumer;
//#if MC >= 12108
//$$ import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
//#else
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
//#endif
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.PlayPayloadHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
//#if MC >= 12108
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#endif
//#if MC >= 12110
//$$ import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
//#endif
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistryClient {
//#if MC >= 12108
   //$$ public static void registerBlockRenderType(Block block, ChunkSectionLayer type) {
   //$$    BlockRenderLayerMap.putBlock(block, type);
   //$$ }
//#else
   public static void registerBlockRenderType(Block block, RenderType type) {
      BlockRenderLayerMap.INSTANCE.putBlock(block, type);
   }
//#endif

//#if MC >= 12110
   //$$ public static <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererProvider<T, S> function) {
   //$$    BlockEntityRenderers.register(type, function);
   //$$ }
//#else
   public static <T extends BlockEntity> void registerBlockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererProvider<? super T> function) {
      BlockEntityRenderers.register(type, function);
   }
//#endif

   public static <V extends CustomPacketPayload> void registerCodec(
      Type<V> id, StreamMemberEncoder<RegistryFriendlyByteBuf, V> encoder, StreamDecoder<RegistryFriendlyByteBuf, V> decoder
   ) {
      PayloadTypeRegistry.playC2S().register(id, StreamCodec.ofMember(encoder, decoder));
      PayloadTypeRegistry.playS2C().register(id, StreamCodec.ofMember(encoder, decoder));
   }

   public static <V extends CustomPacketPayload> void registerNetworkReceiver(Type<V> id, PlayPayloadHandler<V> handler) {
      ClientPlayNetworking.registerGlobalReceiver(id, handler);
   }

   public static void registerClientStartedEvent(Consumer<Minecraft> consumer) {
      ClientLifecycleEvents.CLIENT_STARTED.register(consumer::accept);
   }

   public static void registerClientStoppingEvent(Consumer<Minecraft> consumer) {
      ClientLifecycleEvents.CLIENT_STOPPING.register(consumer::accept);
   }

   public static void registerTickEvent(Consumer<Minecraft> consumer) {
      ClientTickEvents.START_CLIENT_TICK.register(consumer::accept);
   }

   public static <V extends CustomPacketPayload> void sendToServer(V payload) {
      ClientPlayNetworking.send(payload);
   }
}
