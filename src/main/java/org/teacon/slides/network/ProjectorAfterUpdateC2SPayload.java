package org.teacon.slides.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.util.Utilities;

public final class ProjectorAfterUpdateC2SPayload implements CustomPacketPayload {
   public static final Type<ProjectorAfterUpdateC2SPayload> ID = new Type(Slideshow.PACKET_UPDATE);
   private final BlockPos mPos;
   private final ProjectorBlock.InternalRotation mRotation;
   private final CompoundTag mTag;
   private final boolean mIC;

   public ProjectorAfterUpdateC2SPayload(ProjectorBlockEntity entity, ProjectorBlock.InternalRotation rotation) {
      this.mPos = entity.getBlockPos();
      this.mRotation = rotation;
      this.mTag = new CompoundTag();
      entity.saveCompound(this.mTag);
      this.mIC = entity.needInitContainer;
   }

   public ProjectorAfterUpdateC2SPayload(RegistryFriendlyByteBuf buf) {
      this.mPos = buf.readBlockPos();
      this.mRotation = ProjectorBlock.InternalRotation.VALUES[buf.readVarInt()];
      this.mTag = buf.readNbt();
      this.mIC = buf.readBoolean();
   }

   public static void writeBuffer(ProjectorAfterUpdateC2SPayload payload, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(payload.mPos);
      buffer.writeVarInt(payload.mRotation.ordinal());
      buffer.writeNbt(payload.mTag);
      buffer.writeBoolean(payload.mIC);
   }

   public static void handle(ProjectorAfterUpdateC2SPayload payload, Context context) {
      ServerPlayer player = context.player();
      ServerLevel level = player.serverLevel();
      BlockPos pos = payload.mPos;
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (ProjectorBlock.hasProjectorPermission(player) && level.isLoaded(pos) && blockEntity instanceof ProjectorBlockEntity blockEntity1) {
         BlockState state = (BlockState)blockEntity.getBlockState().setValue(ProjectorBlock.ROTATION, payload.mRotation);
         blockEntity1.loadCompound(payload.mTag);
         blockEntity1.needInitContainer = payload.mIC;
         blockEntity.setChanged();
         if (!level.setBlock(pos, state, 3)) {
            level.sendBlockUpdated(pos, state, state, 2);
         }
      } else {
         GameProfile profile = player.getGameProfile();
         Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for projector update: player = {}, pos = {}", profile, payload.mPos);
      }
   }

   public Type<ProjectorAfterUpdateC2SPayload> type() {
      return ID;
   }
}
