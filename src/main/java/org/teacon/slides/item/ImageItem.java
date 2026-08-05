package org.teacon.slides.item;

import java.util.List;
//#if MC >= 12105
//$$ import java.util.function.Consumer;
//#endif
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
//#if MC >= 12102
//$$ import net.minecraft.world.InteractionResult;
//#else
import net.minecraft.world.InteractionResultHolder;
//#endif
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//#if MC >= 12105
//$$ import net.minecraft.world.item.component.TooltipDisplay;
//#endif
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.ClientUtilities;

public class ImageItem extends Item {
   public ImageItem(Properties properties) {
      super(properties);
   }

//#if MC >= 12105
   //$$ @SuppressWarnings("deprecation")
   //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
      //$$ String location = (String)stack.get(Slideshow.LOCATION_COMPONENT);
      //$$ if (location == null) {
         //$$ tooltip.accept(Component.translatable("item.slide_show.image.tooltip.no_properties").withStyle(ChatFormatting.DARK_RED));
         //$$ super.appendHoverText(stack, context, display, tooltip, type);
      //$$ } else {
         //$$ boolean bl = Boolean.TRUE.equals(stack.get(Slideshow.FROM_ID_COMPONENT));
         //$$ tooltip.accept(Component.translatable(bl ? "item.slide_show.image.tooltip.id" : "item.slide_show.image.tooltip.url").withStyle(ChatFormatting.AQUA));
         //$$ tooltip.accept(Component.literal(location).withStyle(ChatFormatting.AQUA));
         //$$ super.appendHoverText(stack, context, display, tooltip, type);
      //$$ }
   //$$ }
//#else
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
      String location = (String)stack.get(Slideshow.LOCATION_COMPONENT);
      if (location == null) {
         tooltip.add(Component.translatable("item.slide_show.image.tooltip.no_properties").withStyle(ChatFormatting.DARK_RED));
         super.appendHoverText(stack, context, tooltip, type);
      } else {
         boolean bl = Boolean.TRUE.equals(stack.get(Slideshow.FROM_ID_COMPONENT));
         tooltip.add(Component.translatable(bl ? "item.slide_show.image.tooltip.id" : "item.slide_show.image.tooltip.url").withStyle(ChatFormatting.AQUA));
         tooltip.add(Component.literal(location).withStyle(ChatFormatting.AQUA));
         super.appendHoverText(stack, context, tooltip, type);
      }
   }
//#endif

   public int getUseDuration(ItemStack stack, LivingEntity user) {
      return 0;
   }

//#if MC >= 12102
   //$$ public InteractionResult use(Level world, Player user, InteractionHand hand) {
//#if MC >= 12110
      //$$ if (!world.isClientSide()) {
//#elseif MC >= 12102
      //$$ if (!world.isClientSide) {
//#endif
         //$$ return InteractionResult.SUCCESS;
      //$$ } else {
         //$$ ItemStack itemStack = user.getItemInHand(hand);
         //$$ if (itemStack != null && !itemStack.isEmpty()) {
            //$$ ClientUtilities.clientSetScreen(hand, itemStack);
            //$$ return InteractionResult.SUCCESS;
         //$$ } else {
            //$$ return InteractionResult.PASS;
         //$$ }
      //$$ }
   //$$ }
//#else
   public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
      if (!world.isClientSide) {
         return InteractionResultHolder.success(user.getItemInHand(hand));
      } else {
         ItemStack itemStack = user.getItemInHand(hand);
         if (itemStack != null && !itemStack.isEmpty()) {
            ClientUtilities.clientSetScreen(hand, itemStack);
            return InteractionResultHolder.success(itemStack);
         } else {
            return InteractionResultHolder.pass(itemStack);
         }
      }
   }
//#endif
}
