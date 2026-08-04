package org.teacon.slides.item;

import java.util.Arrays;
import java.util.List;
//#if MC >= 12105
//$$ import java.util.function.Consumer;
//#endif
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//#if MC < 12102
import net.minecraft.world.InteractionResultHolder;
//#endif
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//#if MC >= 12105
//$$ import net.minecraft.server.level.ServerLevel;
//$$ import net.minecraft.world.entity.EquipmentSlot;
//$$ import net.minecraft.world.item.component.TooltipDisplay;
//#endif
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.Slideshow;
import org.teacon.slides.network.FlipperFlipBackC2SPayload;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.util.RegistryClient;
import org.teacon.slides.util.Utilities;

public class FlipperItem extends Item {
   private static boolean attackKeyDown = false;

   public FlipperItem(Properties properties) {
      super(properties);
   }

   public static List<Integer> getProjectorPos(ItemStack stack) {
      if (!stack.is(Slideshow.FLIPPER_ITEM)) {
         return null;
      } else {
         List<Integer> intArray = (List<Integer>)stack.get(Slideshow.PROJECTOR_COMPONENT);
         return intArray != null && intArray.size() >= 3 ? intArray : null;
      }
   }

   public static void setProjectorPos(ItemStack stack, @Nullable BlockPos pos) {
      if (stack.is(Slideshow.FLIPPER_ITEM)) {
         if (pos == null) {
            stack.remove(Slideshow.PROJECTOR_COMPONENT);
         } else {
            stack.set(Slideshow.PROJECTOR_COMPONENT, Arrays.asList(pos.getX(), pos.getY(), pos.getZ()));
         }
      }
   }

   public static boolean trySendFlip(Level world, ServerPlayer player, ItemStack itemStack, boolean back, boolean init) {
      List<Integer> pos = getProjectorPos(itemStack);
      if (pos == null) {
         Utilities.sendOverLayMessage(player, Component.translatable("info.slide_show.need_bound").withStyle(ChatFormatting.DARK_RED));
         return false;
      } else if (world.getBlockEntity(new BlockPos(pos.get(0), pos.get(1), pos.get(2))) instanceof ProjectorBlockEntity entity1) {
         if (!hasFlipperPermission(player)) {
            return false;
         } else if (!entity1.canFlip()) {
            return false;
         } else if (init) {
            entity1.needInitContainer = true;
            Utilities.sendOverLayMessage(player, Component.translatable("info.slide_show.initialized").withStyle(ChatFormatting.AQUA));
            return true;
         } else {
            entity1.needHandleReadImage = true;
            if (back) {
               entity1.flipBack = true;
            }

            Utilities.sendOverLayMessage(player, Component.translatable("info.slide_show.slide_flipped").withStyle(ChatFormatting.AQUA));
            return true;
         }
      } else {
         Utilities.sendOverLayMessage(player, Component.translatable("info.slide_show.binding_lost").withStyle(ChatFormatting.DARK_RED));
         setProjectorPos(itemStack, null);
         return false;
      }
   }

   private static boolean hasFlipperPermission(ServerPlayer player) {
      return player.isCreative() || player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
   }

//#if MC >= 12102
   //$$ public InteractionResult use(Level world, Player user, InteractionHand hand) {
      //$$ ItemStack itemStack = user.getItemInHand(hand);
      //$$ if (world.isClientSide()) {
         //$$ return InteractionResult.SUCCESS;
      //$$ } else {
         //$$ return (InteractionResult)(trySendFlip(world, (ServerPlayer)user, itemStack, false, user.isShiftKeyDown()) ? InteractionResult.SUCCESS : InteractionResult.FAIL);
      //$$ }
   //$$ }
//#else
   public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
      ItemStack itemStack = user.getItemInHand(hand);
      if (world.isClientSide()) {
         return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
      } else {
         return trySendFlip(world, (ServerPlayer)user, itemStack, false, user.isShiftKeyDown())
            ? InteractionResultHolder.sidedSuccess(itemStack, true)
            : InteractionResultHolder.fail(itemStack);
      }
   }
//#endif

   public InteractionResult useOn(UseOnContext context) {
      Level world = context.getLevel();
      if (world.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack stack = context.getItemInHand();
         if (stack.is(Slideshow.FLIPPER_ITEM)) {
            BlockPos pos = context.getClickedPos();
//#if MC >= 12102
            //$$ Player player = context.getPlayer();
//#else
            ServerPlayer player = (ServerPlayer)context.getPlayer();
//#endif
            if (player == null) {
               return InteractionResult.FAIL;
            } else if (world.getBlockEntity(pos) instanceof ProjectorBlockEntity) {
               setProjectorPos(stack, pos);
               Utilities.sendOverLayMessage(player, Component.translatable("info.slide_show.bound_projector").withStyle(ChatFormatting.AQUA));
               return InteractionResult.CONSUME;
            } else {
//#if MC >= 12102
               //$$ return (InteractionResult)(trySendFlip(world, (ServerPlayer)player, stack, false, player.isShiftKeyDown())
                  //$$ ? InteractionResult.SUCCESS
                  //$$ : InteractionResult.FAIL);
//#else
               return trySendFlip(world, player, stack, false, player.isShiftKeyDown()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
//#endif
            }
         } else {
            return InteractionResult.PASS;
         }
      }
   }

//#if MC >= 12105
   //$$ public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
      //$$ super.inventoryTick(stack, world, entity, slot);
   //$$ }
//#else
   public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, world, entity, slot, selected);
      if (world.isClientSide() && selected && attackKeyDown != Minecraft.getInstance().options.keyAttack.isDown()) {
         attackKeyDown = !attackKeyDown;
         if (attackKeyDown) {
            sendServerFlipBack(slot);
         }
      }
   }
//#endif

//#if MC >= 12105
   //$$ public static void clientTick() {
      //$$ Minecraft mc = Minecraft.getInstance();
      //$$ if (mc.player != null) {
         //$$ Player player = mc.player;
         //$$ int slot = player.getInventory().getSelectedSlot();
         //$$ ItemStack stack = player.getInventory().getItem(slot);
         //$$ if (stack.is(Slideshow.FLIPPER_ITEM) && attackKeyDown != mc.options.keyAttack.isDown()) {
            //$$ attackKeyDown = !attackKeyDown;
            //$$ if (attackKeyDown) {
               //$$ sendServerFlipBack(slot);
            //$$ }
         //$$ }
      //$$ }
   //$$ }
//#endif

//#if MC >= 12105
   //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
      //$$ List<Integer> pos = getProjectorPos(stack);
      //$$ if (pos == null) {
         //$$ tooltip.accept(Component.translatable("item.slide_show.flipper.tooltip.not_bound").withStyle(ChatFormatting.RED));
         //$$ tooltip.accept(Component.translatable("item.slide_show.flipper.tooltip.not_bound1"));
         //$$ super.appendHoverText(stack, context, display, tooltip, type);
      //$$ } else {
         //$$ tooltip.accept(Component.translatable("item.slide_show.flipper.tooltip.bound", new Object[]{pos.get(0), pos.get(1), pos.get(2)}).withStyle(ChatFormatting.AQUA));
         //$$ super.appendHoverText(stack, context, display, tooltip, type);
      //$$ }
   //$$ }
//#else
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
      List<Integer> pos = getProjectorPos(stack);
      if (pos == null) {
         tooltip.add(Component.translatable("item.slide_show.flipper.tooltip.not_bound").withStyle(ChatFormatting.RED));
         tooltip.add(Component.translatable("item.slide_show.flipper.tooltip.not_bound1"));
         super.appendHoverText(stack, context, tooltip, type);
      } else {
         tooltip.add(Component.translatable("item.slide_show.flipper.tooltip.bound", new Object[]{pos.get(0), pos.get(1), pos.get(2)}).withStyle(ChatFormatting.AQUA));
         super.appendHoverText(stack, context, tooltip, type);
      }
   }
//#endif

   private static void sendServerFlipBack(int i) {
      RegistryClient.sendToServer(new FlipperFlipBackC2SPayload(i));
   }

   public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
      return false;
   }
}
