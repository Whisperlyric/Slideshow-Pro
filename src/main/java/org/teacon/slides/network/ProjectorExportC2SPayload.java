package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.util.Utilities;

public class ProjectorExportC2SPayload implements CustomPacketPayload {
   public static final Type<ProjectorExportC2SPayload> ID = new Type<>(Slideshow.PACKET_EXPORT);
   private final boolean mFromID;
   private final String mLocation;

   public ProjectorExportC2SPayload(boolean fromID, String location) {
      this.mFromID = fromID;
      this.mLocation = location;
   }

   public ProjectorExportC2SPayload(RegistryFriendlyByteBuf buf) {
      this.mFromID = buf.readBoolean();
      this.mLocation = buf.readUtf();
   }

   public static void writeBuffer(ProjectorExportC2SPayload payload, RegistryFriendlyByteBuf buffer) {
      buffer.writeBoolean(payload.mFromID);
      buffer.writeUtf(payload.mLocation);
   }

   public static void handle(ProjectorExportC2SPayload payload, Context context) {
      ServerPlayer serverPlayer = context.player();
      if (!giveImageItem(serverPlayer, payload.mFromID, payload.mLocation)) {
         GameProfile profile = serverPlayer.getGameProfile();
         Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for projector export: player = {}", profile);
      }
   }

   public static boolean giveImageItem(ServerPlayer serverPlayer, boolean fromID, String location) {
      if (!ProjectorBlock.hasProjectorPermission(serverPlayer)) {
         return false;
      } else {
         ItemStack itemStack = new ItemStack(Slideshow.IMAGE_ITEM, 1);
         itemStack.set(Slideshow.FROM_ID_COMPONENT, fromID);
         itemStack.set(Slideshow.LOCATION_COMPONENT, location);
         boolean bl = serverPlayer.getInventory().add(itemStack);
         if (bl && itemStack.isEmpty()) {
            itemStack.setCount(1);
            ItemEntity itemEntity = serverPlayer.drop(itemStack, false);
            if (itemEntity != null) {
               itemEntity.makeFakeItem();
            }

            serverPlayer.level()
               .playSound(
                  null,
                  serverPlayer.getX(),
                  serverPlayer.getY(),
                  serverPlayer.getZ(),
                  SoundEvents.ITEM_PICKUP,
                  SoundSource.PLAYERS,
                  0.2F,
                  ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
               );
            serverPlayer.inventoryMenu.broadcastChanges();
         } else {
            ItemEntity itemEntity = serverPlayer.drop(itemStack, false);
            if (itemEntity != null) {
               itemEntity.setNoPickUpDelay();
               itemEntity.setTarget(serverPlayer.getUUID());
            }
         }

         return true;
      }
   }

   public @NotNull Type<ProjectorExportC2SPayload> type() {
      return ID;
   }
}
