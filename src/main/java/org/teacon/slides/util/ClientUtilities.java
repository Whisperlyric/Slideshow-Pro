package org.teacon.slides.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.teacon.slides.projector.ImageSourceScreen;

public class ClientUtilities {
   public static void clientSetScreen(InteractionHand hand, ItemStack itemStack) {
//#if MC >= 26_02_00
      //$$ Minecraft.getInstance().setScreenAndShow(new ImageSourceScreen(hand, itemStack));
//#else
      Minecraft.getInstance().setScreen(new ImageSourceScreen(hand, itemStack));
//#endif
   }
}
