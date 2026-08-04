package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.Utilities;

public class ImageAfterUpdateC2SPayload implements CustomPacketPayload {
   public static final Type<ImageAfterUpdateC2SPayload> ID = new Type(Slideshow.PACKET_IMAGE_UPDATE);
   private final InteractionHand hand;
   private final boolean fromId;
   private final String location;

   public ImageAfterUpdateC2SPayload(InteractionHand hand, boolean fromId, String location) {
      this.hand = hand;
      this.fromId = fromId;
      this.location = location;
   }

   public ImageAfterUpdateC2SPayload(RegistryFriendlyByteBuf buf) {
      this.hand = buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
      this.fromId = buf.readBoolean();
      this.location = buf.readUtf(512);
   }

   public static void writeBuffer(ImageAfterUpdateC2SPayload payload, RegistryFriendlyByteBuf buffer) {
      buffer.writeBoolean(payload.hand == InteractionHand.MAIN_HAND);
      buffer.writeBoolean(payload.fromId);
      buffer.writeUtf(payload.location);
   }

   public static void handle(ImageAfterUpdateC2SPayload payload, Context context) {
      ItemStack itemStack = context.player().getItemInHand(payload.hand);
      if (itemStack != null && !itemStack.isEmpty() && itemStack.is(Slideshow.IMAGE_ITEM)) {
         itemStack.set(Slideshow.FROM_ID_COMPONENT, payload.fromId);
         itemStack.set(Slideshow.LOCATION_COMPONENT, payload.location);
      } else {
         GameProfile profile = context.player().getGameProfile();
         Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for projector export: player = {}", profile);
      }
   }

   public Type<ImageAfterUpdateC2SPayload> type() {
      return ID;
   }
}
