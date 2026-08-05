package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 12104
//$$ import net.minecraft.server.MinecraftServer;
//#endif
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.Slideshow;
import org.teacon.slides.item.FlipperItem;
import org.teacon.slides.util.Utilities;

public final class FlipperFlipBackC2SPayload implements CustomPacketPayload {
   public static final Type<FlipperFlipBackC2SPayload> ID = new Type(Slideshow.PACKET_FLIP_BACK);
   private final int slot;

   public FlipperFlipBackC2SPayload(int slot) {
      this.slot = slot;
   }

   public FlipperFlipBackC2SPayload(RegistryFriendlyByteBuf buf) {
      this.slot = buf.readInt();
   }

   public static void writeBuffer(FlipperFlipBackC2SPayload payload, RegistryFriendlyByteBuf buffer) {
      buffer.writeInt(payload.slot);
   }

   public static void handle(FlipperFlipBackC2SPayload payload, Context context) {
      int i = payload.slot;
      ServerPlayer serverPlayer = context.player();
//#if MC >= 12104
      //$$ MinecraftServer minecraftServer = context.server();
//#endif
      ItemStack itemStack = serverPlayer.getInventory().getItem(i);
      if (!itemStack.is(Slideshow.FLIPPER_ITEM) || !FlipperItem.trySendFlip(serverPlayer.serverLevel(), serverPlayer, itemStack, true, false)) {
         GameProfile profile = serverPlayer.getGameProfile();
         Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for flip back: player = {}", profile);
      }
   }

   public @NotNull Type<FlipperFlipBackC2SPayload> type() {
      return ID;
   }
}
