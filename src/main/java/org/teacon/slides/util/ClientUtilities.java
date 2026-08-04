package org.teacon.slides.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.teacon.slides.projector.ImageSourceScreen;

public class ClientUtilities {
   public static void clientSetScreen(InteractionHand hand, ItemStack itemStack) {
      Minecraft.getInstance().setScreen(new ImageSourceScreen(hand, itemStack));
   }
}
