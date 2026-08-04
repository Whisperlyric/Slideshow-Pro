package org.teacon.slides.projector;

import com.mojang.blaze3d.platform.Lighting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.teacon.slides.Slideshow;
import org.teacon.slides.network.ImageAfterUpdateC2SPayload;
import org.teacon.slides.renderer.SlideState;
import org.teacon.slides.util.RegistryClient;

@Environment(EnvType.CLIENT)
public class ImageSourceScreen extends Screen {
   private final InteractionHand hand;
   private boolean fromId = false;
   private String location = "";
   private EditBox locationInput;
   private Button sourceUrlButton;
   private Button sourceIdButton;
   private boolean invalidLocation = false;
   private boolean resultDone = true;

   public ImageSourceScreen(InteractionHand hand, ItemStack itemStack) {
      super(Component.translatable("gui.slide_show.edit_image_source"));
      this.hand = hand;
      if (itemStack != null && !itemStack.isEmpty()) {
         this.fromId = Boolean.TRUE.equals(itemStack.get(Slideshow.FROM_ID_COMPONENT));
         this.location = (String)itemStack.get(Slideshow.LOCATION_COMPONENT);
         if (this.location == null) {
            this.location = "";
         }
      }
   }

   protected void init() {
      super.init();
      this.sourceUrlButton = Button.builder(Component.translatable("gui.slide_show.url"), button -> {
         this.fromId = true;
         button.visible = false;
         this.sourceIdButton.visible = true;
      }).bounds(this.width / 2 - 100, 80, 200, 20).build();
      this.addRenderableWidget(this.sourceUrlButton);
      this.sourceIdButton = Button.builder(Component.translatable("gui.slide_show.id"), button -> {
         this.fromId = false;
         button.visible = false;
         this.sourceUrlButton.visible = true;
      }).bounds(this.width / 2 - 100, 80, 200, 20).build();
      this.addRenderableWidget(this.sourceIdButton);
      this.sourceUrlButton.visible = !this.fromId;
      this.sourceIdButton.visible = this.fromId;
      this.locationInput = new EditBox(this.font, this.width / 2 - 100, 110, 200, 16, Component.translatable("gui.slide_show.url"));
      this.locationInput.setMaxLength(512);
      this.locationInput.setResponder(text -> {
         if (StringUtils.isNotBlank(text)) {
            this.invalidLocation = this.fromId ? ResourceLocation.tryParse(text) == null : SlideState.createURI(text) == null;
         } else {
            this.invalidLocation = false;
         }

         this.locationInput.setTextColor(this.invalidLocation ? 14699339 : 14737632);
         this.location = text;
      });
      this.locationInput.setValue(this.location);
      this.addRenderableWidget(this.locationInput);
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
         this.resultDone = true;
         this.finishEditing();
      }).bounds(this.width / 2 - 90, this.height / 4 + 114, 80, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
         this.resultDone = false;
         this.finishEditing();
      }).bounds(this.width / 2 + 10, this.height / 4 + 114, 80, 20).build());
   }

   public void removed() {
      if (this.resultDone) {
         RegistryClient.sendToServer(new ImageAfterUpdateC2SPayload(this.hand, this.fromId, this.location));
      }
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
//#if MC >= 12108
      //$$ try (Lighting lighting = new Lighting()) {
         //$$ lighting.setupFor(Lighting.Entry.ITEMS_FLAT);
         //$$ this.renderBackground(context, mouseX, mouseY, delta);
         //$$ context.drawCenteredString(this.font, this.title, this.width / 2, 40, 16777215);
         //$$ lighting.setupFor(Lighting.Entry.ITEMS_3D);
         //$$ super.render(context, mouseX, mouseY, delta);
      //$$ }
//#else
      Lighting.setupForFlatItems();
      this.renderBackground(context, mouseX, mouseY, delta);
      context.drawCenteredString(this.font, this.title, this.width / 2, 40, 16777215);
      Lighting.setupFor3DItems();
      super.render(context, mouseX, mouseY, delta);
//#endif
   }

   private void finishEditing() {
      this.minecraft.setScreen(null);
   }
}
