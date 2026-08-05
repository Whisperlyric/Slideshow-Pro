package org.teacon.slides.projector;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.Slideshow;
import org.teacon.slides.network.ProjectorOpenScreenPayload;

public class ProjectorScreenHandler extends AbstractContainerMenu {
   private final BlockPos pos;

   public ProjectorScreenHandler(int syncId, ProjectorOpenScreenPayload payload) {
      super(Slideshow.PROJECTOR_SCREEN_HANDLER, syncId);
      this.pos = payload.pos;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public @NotNull ItemStack quickMoveStack(Player player, int slot) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      return player instanceof ServerPlayer serverPlayer && ProjectorBlock.hasProjectorPermission(serverPlayer);
   }
}
