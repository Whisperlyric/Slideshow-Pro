package org.teacon.slides.projector;

//#if MC < 12102
import com.mojang.blaze3d.systems.RenderSystem;
//#endif
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//#if MC >= 26_00_00
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//#if MC >= 12110
//$$ import net.minecraft.client.input.KeyEvent;
//#endif
//#if MC >= 12108
//$$ import net.minecraft.client.renderer.RenderPipelines;
//#elseif MC >= 12102
//$$ import net.minecraft.client.renderer.RenderType;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.teacon.slides.network.ProjectorAfterUpdateC2SPayload;
import org.teacon.slides.network.ProjectorExportC2SPayload;
import org.teacon.slides.renderer.SlideState;
import org.teacon.slides.util.RegistryClient;

public final class ProjectorScreen extends AbstractContainerScreen<ProjectorScreenHandler> {
   private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("slide_show", "textures/gui/projector.png");
   private static final Component IMAGE_TEXT = Component.translatable("gui.slide_show.section.image");
   private static final Component OFFSET_TEXT = Component.translatable("gui.slide_show.section.offset");
   private static final Component OTHERS_TEXT = Component.translatable("gui.slide_show.section.others");
   private static final Component URL_TEXT = Component.translatable("gui.slide_show.url");
   private static final Component ID_TEXT = Component.translatable("gui.slide_show.id");
   private static final Component EXPORT_TEXT = Component.translatable("gui.slide_show.export");
   private static final Component CONTAINER_TEXT = Component.translatable("gui.slide_show.container");
   private static final Component COLOR_TEXT = Component.translatable("gui.slide_show.color");
   private static final Component WIDTH_TEXT = Component.translatable("gui.slide_show.width");
   private static final Component HEIGHT_TEXT = Component.translatable("gui.slide_show.height");
   private static final Component ANGLE_X_TEXT = Component.translatable("gui.slide_show.angle_x");
   private static final Component ANGLE_Y_TEXT = Component.translatable("gui.slide_show.angle_y");
   private static final Component ANGLE_Z_TEXT = Component.translatable("gui.slide_show.angle_z");
   private static final Component OFFSET_X_TEXT = Component.translatable("gui.slide_show.offset_x");
   private static final Component OFFSET_Y_TEXT = Component.translatable("gui.slide_show.offset_y");
   private static final Component OFFSET_Z_TEXT = Component.translatable("gui.slide_show.offset_z");
   private static final Component FLIP_TEXT = Component.translatable("gui.slide_show.flip");
   private static final Component ROTATE_TEXT = Component.translatable("gui.slide_show.rotate");
   private static final Component SINGLE_DOUBLE_SIDED_TEXT = Component.translatable("gui.slide_show.single_double_sided");
   private static final Component LAST_SCREEN_TEXT = Component.translatable("gui.slide_show.last_screen");
   private static final Component NEXT_SCREEN_TEXT = Component.translatable("gui.slide_show.next_screen");
   private static final Component NEW_SCREEN_TEXT = Component.translatable("gui.slide_show.new_screen");
   private static final Component REMOVE_SCREEN_TEXT = Component.translatable("gui.slide_show.remove_screen");
   private static final Predicate<String> COLOR_PREDICATE = string -> string.matches("^[0-9A-Fa-f]+$");
   private static final Predicate<String> INTEGER_PREDICATE = string -> string.matches("^[-+]?\\d*$");
   private static int LAST_SCREEN = 0;
   private static BlockPos LAST_PROJECTOR_POS = null;
   private boolean editBool = true;
   private Direction blockDirection;
   private EditBox mURLInput;
   private EditBox mColorInput;
   private EditBox mWidthInput;
   private EditBox mHeightInput;
   private EditBox mOffsetXInput;
   private EditBox mOffsetYInput;
   private EditBox mOffsetZInput;
   private EditBox mAngleXInput;
   private EditBox mAngleYInput;
   private EditBox mAngleZInput;
   private ProjectorScreen.ScreenTexturedButtonWidget mLastScreen;
   private ProjectorScreen.ScreenTexturedButtonWidget mNextScreen;
   private ProjectorScreen.ScreenTexturedButtonWidget mNewScreen;
   private ProjectorScreen.ScreenTexturedButtonWidget mRemoveScreen;
   private ProjectorScreen.ScreenTexturedButtonWidget mSwitchURL;
   private ProjectorScreen.ScreenTexturedButtonWidget mSwitchID;
   private ProjectorScreen.ScreenTexturedButtonWidget mSwitchContainer;
   private ProjectorScreen.ScreenTexturedButtonWidget mButtonExport;
   private ProjectorScreen.ScreenTexturedButtonWidget mSwitchSingleSided;
   private ProjectorScreen.ScreenTexturedButtonWidget mSwitchDoubleSided;
   private SourceType mSourceType;
   private List<ImageProperties> props;
   private int propIndex;
   private boolean mDoubleSided;
   private int mImageColor = -1;
   private Vec2 mImageSize = Vec2.ONE;
   private Vector3f mImageOffset = new Vector3f();
   private Vec3i mImageAngles = new Vec3i(0, 0, 0);
   private ProjectorBlock.InternalRotation mRotation = ProjectorBlock.InternalRotation.NONE;
   private boolean mInvalidURL = true;
   private boolean mInvalidColor = false;
   private boolean mInvalidWidth = false;
   private boolean mInvalidHeight = false;
   private boolean mInvalidOffsetX = false;
   private boolean mInvalidOffsetY = false;
   private boolean mInvalidOffsetZ = false;
   private final ProjectorBlockEntity mEntity;
   private final int imageWidth;
   private final int imageHeight;

   public ProjectorScreen(ProjectorScreenHandler handler, Inventory inventory, Component title) {
      super(handler, inventory, title);
      BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(handler.getPos());
      this.mEntity = blockEntity instanceof ProjectorBlockEntity ? (ProjectorBlockEntity)blockEntity : null;
      this.imageWidth = 176;
      this.imageHeight = 217;
   }

   protected void init() {
      super.init();
      if (this.mEntity != null) {
         this.mSourceType = this.mEntity.mSourceType;
         this.props = new ArrayList<>();
         this.blockDirection = (Direction)this.mEntity.getBlockState().getValue(BlockStateProperties.FACING);
         this.mEntity.imageProps.forEach(prop -> {
            ImageProperties nP = prop.copy();
            if (nP.anglesUseDefault) {
               nP.updateAngles(this.blockDirection);
            }

            this.props.add(nP);
         });
         if (this.props.isEmpty()) {
            this.props.add(new ImageProperties());
         }

         this.propIndex = 0;
         if (this.mEntity.getBlockPos().equals(LAST_PROJECTOR_POS) && LAST_SCREEN < this.props.size()) {
            this.propIndex = LAST_SCREEN;
         }

         int leftPos = (this.width - this.imageWidth) / 2;
         int topPos = (this.height - this.imageHeight) / 2;
         this.mURLInput = new EditBox(this.font, leftPos + 30, topPos + 29, 137, 16, Component.translatable("gui.slide_show.url"));
         this.mURLInput.setMaxLength(512);
         this.mURLInput.setResponder(text -> {
            if (StringUtils.isNotBlank(text)) {
               this.mInvalidURL = switch (this.mSourceType) {
                  case ResourceID -> ResourceLocation.tryParse(text) == null;
                  case URL -> SlideState.createURI(text) == null;
                  default -> false;
               };
            } else {
               this.mInvalidURL = false;
            }

            this.mURLInput.setTextColor(this.mInvalidURL ? 0xFFE0494B : 0xFFE0E0E0);
         });
         this.mURLInput.setValue(this.mEntity.mLocation);
         this.addRenderableWidget(this.mURLInput);
         this.setInitialFocus(this.mURLInput);
         this.mColorInput = new EditBox(this.font, leftPos + 55, topPos + 155, 56, 16, Component.translatable("gui.slide_show.color"));
         this.mColorInput.setMaxLength(8);
         //#if MC >= 26_00_00
      //$$ // EditBox.setFilter removed in 26.x
//#else
      this.mColorInput.setFilter(COLOR_PREDICATE);
//#endif
      this.mColorInput.setResponder(text -> {
            try {
               this.mImageColor = Integer.parseUnsignedInt(text, 16);
            } catch (Exception var3) {
            }
         });
         this.mColorInput.setValue(String.format("%08X", this.mEntity.mColor));
         this.mColorInput.setTextColor(0xFFE0E0E0);
         this.addRenderableWidget(this.mColorInput);
         this.mLastScreen = new ProjectorScreen.ScreenTexturedButtonWidget(
            leftPos + 9, topPos + 55, 9, 9, 201, 33, GUI_TEXTURE, button -> this.switchToScreen(this.propIndex - 1)
         );
         this.mLastScreen.setTooltip(Tooltip.create(LAST_SCREEN_TEXT));
         this.mNextScreen = new ProjectorScreen.ScreenTexturedButtonWidget(
            leftPos + 157, topPos + 55, 9, 9, 211, 33, GUI_TEXTURE, button -> this.switchToScreen(this.propIndex + 1)
         );
         this.mNextScreen.setTooltip(Tooltip.create(NEXT_SCREEN_TEXT));
         this.mNewScreen = new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 157, topPos + 55, 9, 9, 201, 43, GUI_TEXTURE, button -> {
            ImageProperties prop = new ImageProperties();
            prop.updateAngles(this.blockDirection);
            this.props.add(prop);
            this.switchToScreen(this.props.size() - 1);
         });
         this.mNewScreen.setTooltip(Tooltip.create(NEW_SCREEN_TEXT));
         this.mRemoveScreen = new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 142, topPos + 55, 9, 9, 211, 43, GUI_TEXTURE, button -> {
            if (this.props.size() != 1) {
               this.props.remove(this.propIndex);
               if (this.propIndex == this.props.size()) {
                  this.propIndex--;
               }

               this.switchToScreen(this.propIndex);
            }
         });
         this.mRemoveScreen.setTooltip(Tooltip.create(REMOVE_SCREEN_TEXT));
         this.addRenderableWidget(this.mLastScreen);
         this.addRenderableWidget(this.mNextScreen);
         this.addRenderableWidget(this.mNewScreen);
         this.addRenderableWidget(this.mRemoveScreen);
         this.mWidthInput = new EditBox(this.font, leftPos + 30, topPos + 68, 56, 16, Component.translatable("gui.slide_show.width"));
         this.mWidthInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  float nValue = parseFloat(input);
                  Vec2 newSize = new Vec2(nValue, this.mImageSize.y);
                  this.updateSize(newSize);
                  this.props.get(this.propIndex).width = nValue;
                  this.mInvalidWidth = false;
               } catch (Exception var4) {
                  this.mInvalidWidth = true;
               }

               this.mWidthInput.setTextColor(this.mInvalidWidth ? 0xFFE0494B : 0xFFE0E0E0);
            } else {
               this.mInvalidWidth = false;
               this.mWidthInput.setTextColor(0xFFE0E0E0);
            }
         });
         this.addRenderableWidget(this.mWidthInput);
         this.mHeightInput = new EditBox(this.font, leftPos + 111, topPos + 68, 56, 16, Component.translatable("gui.slide_show.height"));
         this.mHeightInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  float nValue = parseFloat(input);
                  Vec2 newSize = new Vec2(this.mImageSize.x, nValue);
                  this.updateSize(newSize);
                  this.props.get(this.propIndex).height = nValue;
                  this.mInvalidHeight = false;
               } catch (Exception var4) {
                  this.mInvalidHeight = true;
               }

               this.mHeightInput.setTextColor(this.mInvalidHeight ? 0xFFE0494B : 0xFFE0E0E0);
            } else {
               this.mInvalidWidth = false;
               this.mWidthInput.setTextColor(0xFFE0E0E0);
            }
         });
         this.addRenderableWidget(this.mHeightInput);
         this.mAngleXInput = new EditBox(this.font, leftPos + 30, topPos + 90, 29, 16, Component.translatable("gui.slide_show.angle_x"));
//#if MC >= 26_00_00
         //$$ // EditBox.setFilter removed in 26.x
//#else
         this.mAngleXInput.setFilter(INTEGER_PREDICATE);
//#endif
         this.mAngleXInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  ImageProperties prop = this.props.get(this.propIndex);
                  int nValue = Integer.parseInt(input);
                  if (prop.angleX != nValue) {
                     this.mImageAngles = new Vec3i(nValue, this.mImageAngles.getY(), this.mImageAngles.getZ());
                     prop.anglesUseDefault = false;
                     prop.angleX = nValue;
                  }
               } catch (Exception var4) {
               }
            }
         });
         this.mAngleXInput.setTextColor(0xFFE0E0E0);
         this.addRenderableWidget(this.mAngleXInput);
         this.mAngleYInput = new EditBox(this.font, leftPos + 84, topPos + 90, 29, 16, Component.translatable("gui.slide_show.angle_y"));
//#if MC >= 26_00_00
         //$$ // EditBox.setFilter removed in 26.x
//#else
         this.mAngleYInput.setFilter(INTEGER_PREDICATE);
//#endif
         this.mAngleYInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  ImageProperties prop = this.props.get(this.propIndex);
                  int nValue = Integer.parseInt(input);
                  if (prop.angleY != nValue) {
                     this.mImageAngles = new Vec3i(this.mImageAngles.getX(), nValue, this.mImageAngles.getZ());
                     prop.anglesUseDefault = false;
                     prop.angleY = nValue;
                  }
               } catch (Exception var4) {
               }
            }
         });
         this.mAngleYInput.setTextColor(0xFFE0E0E0);
         this.addRenderableWidget(this.mAngleYInput);
         this.mAngleZInput = new EditBox(this.font, leftPos + 138, topPos + 90, 29, 16, Component.translatable("gui.slide_show.angle_z"));
//#if MC >= 26_00_00
         //$$ // EditBox.setFilter removed in 26.x
//#else
         this.mAngleZInput.setFilter(INTEGER_PREDICATE);
//#endif
         this.mAngleZInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  ImageProperties prop = this.props.get(this.propIndex);
                  int nValue = Integer.parseInt(input);
                  if (prop.angleZ != nValue) {
                     this.mImageAngles = new Vec3i(this.mImageAngles.getX(), this.mImageAngles.getY(), nValue);
                     prop.anglesUseDefault = false;
                     prop.angleZ = nValue;
                  }
               } catch (Exception var4) {
               }
            }
         });
         this.mAngleZInput.setTextColor(0xFFE0E0E0);
         this.addRenderableWidget(this.mAngleZInput);
         this.mOffsetXInput = new EditBox(this.font, leftPos + 30, topPos + 112, 29, 16, Component.translatable("gui.slide_show.offset_x"));
         this.mOffsetXInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  float nValue = parseFloat(input);
                  this.mImageOffset = new Vector3f(parseFloat(input), this.mImageOffset.y(), this.mImageOffset.z());
                  this.props.get(this.propIndex).offsetX = nValue;
                  this.mInvalidOffsetX = false;
               } catch (Exception var3) {
                  this.mInvalidOffsetX = true;
               }

               this.mOffsetXInput.setTextColor(this.mInvalidOffsetX ? 0xFFE0494B : 0xFFE0E0E0);
            } else {
               this.mInvalidWidth = false;
               this.mWidthInput.setTextColor(0xFFE0E0E0);
            }
         });
         this.addRenderableWidget(this.mOffsetXInput);
         this.mOffsetYInput = new EditBox(this.font, leftPos + 84, topPos + 112, 29, 16, Component.translatable("gui.slide_show.offset_y"));
         this.mOffsetYInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  float nValue = parseFloat(input);
                  this.mImageOffset = new Vector3f(this.mImageOffset.x(), parseFloat(input), this.mImageOffset.z());
                  this.props.get(this.propIndex).offsetY = nValue;
                  this.mInvalidOffsetY = false;
               } catch (Exception var3) {
                  this.mInvalidOffsetY = true;
               }

               this.mOffsetYInput.setTextColor(this.mInvalidOffsetY ? 0xFFE0494B : 0xFFE0E0E0);
            } else {
               this.mInvalidWidth = false;
               this.mWidthInput.setTextColor(0xFFE0E0E0);
            }
         });
         this.addRenderableWidget(this.mOffsetYInput);
         this.mOffsetZInput = new EditBox(this.font, leftPos + 138, topPos + 112, 29, 16, Component.translatable("gui.slide_show.offset_z"));
         this.mOffsetZInput.setResponder(input -> {
            if (this.editBool) {
               try {
                  float nValue = parseFloat(input);
                  this.mImageOffset = new Vector3f(this.mImageOffset.x(), this.mImageOffset.y(), parseFloat(input));
                  this.props.get(this.propIndex).offsetZ = nValue;
                  this.mInvalidOffsetZ = false;
               } catch (Exception var3) {
                  this.mInvalidOffsetZ = true;
               }

               this.mOffsetZInput.setTextColor(this.mInvalidOffsetZ ? 0xFFE0494B : 0xFFE0E0E0);
            } else {
               this.mInvalidWidth = false;
               this.mWidthInput.setTextColor(0xFFE0E0E0);
            }
         });
         this.addRenderableWidget(this.mOffsetZInput);
         this.switchToScreen(this.propIndex);
         this.addRenderableWidget(new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 117, topPos + 153, 18, 19, 179, 153, GUI_TEXTURE, button -> {
            ProjectorBlock.InternalRotation newRotation = this.mRotation.flip();
            this.updateRotation(newRotation);
         }));
         this.addRenderableWidget(new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 142, topPos + 153, 18, 19, 179, 173, GUI_TEXTURE, button -> {
            ProjectorBlock.InternalRotation newRotation = this.mRotation.compose(Rotation.CLOCKWISE_90);
            this.updateRotation(newRotation);
         }));
         this.mRotation = (ProjectorBlock.InternalRotation)this.mEntity.getBlockState().getValue(ProjectorBlock.ROTATION);
         this.mSwitchURL = new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 53, GUI_TEXTURE, button -> {
            this.mSourceType = SourceType.ResourceID;
            this.mSwitchID.visible = true;
            this.mSwitchURL.visible = false;
         });
         this.mSwitchID = new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 73, GUI_TEXTURE, button -> {
            this.mSourceType = SourceType.ContainerBlock;
            this.mSwitchContainer.visible = true;
            this.mSwitchID.visible = false;
            this.mButtonExport.visible = false;
         });
         this.mSwitchContainer = new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 9, topPos + 27, 18, 19, 179, 93, GUI_TEXTURE, button -> {
            this.mSourceType = SourceType.URL;
            this.mSwitchURL.visible = true;
            this.mSwitchContainer.visible = false;
            this.mButtonExport.visible = true;
         });
         this.mButtonExport = new ProjectorScreen.ScreenTexturedButtonWidget(
            leftPos + 149, topPos + 7, 18, 19, 179, 33, GUI_TEXTURE, button -> this.sendExport()
         );
         this.mSwitchSingleSided = new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 9, topPos + 153, 18, 19, 179, 113, GUI_TEXTURE, button -> {
            this.mDoubleSided = true;
            this.mSwitchDoubleSided.visible = true;
            this.mSwitchSingleSided.visible = false;
         });
         this.mSwitchDoubleSided = new ProjectorScreen.ScreenTexturedButtonWidget(leftPos + 9, topPos + 153, 18, 19, 179, 133, GUI_TEXTURE, button -> {
            this.mDoubleSided = false;
            this.mSwitchSingleSided.visible = true;
            this.mSwitchDoubleSided.visible = false;
         });
         this.mSwitchURL.visible = this.mSourceType == SourceType.URL;
         this.mSwitchID.visible = this.mSourceType == SourceType.ResourceID;
         this.mSwitchContainer.visible = this.mSourceType == SourceType.ContainerBlock;
         this.mButtonExport.visible = this.mSourceType != SourceType.ContainerBlock;
         this.mDoubleSided = this.mEntity.mDoubleSided;
         this.mSwitchDoubleSided.visible = this.mDoubleSided;
         this.mSwitchSingleSided.visible = !this.mDoubleSided;
         this.addRenderableWidget(this.mSwitchURL);
         this.addRenderableWidget(this.mSwitchID);
         this.addRenderableWidget(this.mSwitchContainer);
         this.addRenderableWidget(this.mButtonExport);
         this.addRenderableWidget(this.mSwitchSingleSided);
         this.addRenderableWidget(this.mSwitchDoubleSided);
      }
   }

   private void syncNewSize(ImageProperties prop) {
      this.mImageSize = new Vec2(prop.width, prop.height);
      this.editBool = false;
      this.mWidthInput.setValue(floatToString(prop.width));
      this.mHeightInput.setValue(floatToString(prop.height));
      this.editBool = true;
   }

   private void syncNewOffset(ImageProperties prop) {
      this.mImageOffset = new Vector3f(prop.offsetX, prop.offsetY, prop.offsetZ);
      this.editBool = false;
      this.mOffsetXInput.setValue(floatToString(prop.offsetX));
      this.mOffsetYInput.setValue(floatToString(prop.offsetY));
      this.mOffsetZInput.setValue(floatToString(prop.offsetZ));
      this.editBool = true;
   }

   private void syncNewAngles(ImageProperties prop) {
      this.mImageAngles = new Vec3i(prop.angleX, prop.angleY, prop.angleZ);
      this.editBool = false;
      this.mAngleXInput.setValue(String.valueOf(prop.angleX));
      this.mAngleYInput.setValue(String.valueOf(prop.angleY));
      this.mAngleZInput.setValue(String.valueOf(prop.angleZ));
      this.editBool = true;
   }

   private void switchToScreen(int index) {
      this.propIndex = index;
      ImageProperties prop = this.props.get(this.propIndex);
      this.syncNewSize(prop);
      this.syncNewOffset(prop);
      this.syncNewAngles(prop);
      this.mLastScreen.visible = this.propIndex > 0;
      this.mNextScreen.visible = this.propIndex < this.props.size() - 1;
      this.mNewScreen.visible = !this.mNextScreen.visible;
      this.mRemoveScreen.visible = this.props.size() > 1;
   }

   private void updateRotation(ProjectorBlock.InternalRotation newRotation) {
      if (!this.mInvalidOffsetX && !this.mInvalidOffsetY && !this.mInvalidOffsetZ) {
         Vector3f absolute = relativeToAbsolute(this.mImageOffset, this.mImageSize, this.mRotation);
         Vector3f newRelative = absoluteToRelative(absolute, this.mImageSize, newRotation);
         ImageProperties prop = this.props.get(this.propIndex);
         prop.offsetX = newRelative.x;
         prop.offsetY = newRelative.y;
         prop.offsetZ = newRelative.z;
         this.syncNewOffset(prop);
      }

      this.mRotation = newRotation;
   }

   private void updateSize(Vec2 newSize) {
      if (!this.mInvalidOffsetX && !this.mInvalidOffsetY && !this.mInvalidOffsetZ) {
         Vector3f absolute = relativeToAbsolute(this.mImageOffset, this.mImageSize, this.mRotation);
         Vector3f newRelative = absoluteToRelative(absolute, newSize, this.mRotation);
         ImageProperties prop = this.props.get(this.propIndex);
         prop.offsetX = newRelative.x;
         prop.offsetY = newRelative.y;
         prop.offsetZ = newRelative.z;
         this.syncNewOffset(prop);
      }

      this.mImageSize = newSize;
   }

   protected void containerTick() {
      if (this.mEntity == null) {
         this.minecraft.player.closeContainer();
      }
   }

   private void sendExport() {
      if (this.mSourceType != SourceType.ContainerBlock) {
         ClientPlayNetworking.send(new ProjectorExportC2SPayload(this.mSourceType == SourceType.ResourceID, this.mURLInput.getValue()));
      }
   }

   public void removed() {
      super.removed();
      if (this.mEntity != null) {
         LAST_SCREEN = this.propIndex;
         LAST_PROJECTOR_POS = this.mEntity.getBlockPos();
         if (!this.mInvalidURL) {
            this.mEntity.mLocation = this.mURLInput.getValue();
         }

         if (!this.mInvalidColor) {
            this.mEntity.mColor = this.mImageColor;
         }

         this.mEntity.updateProps(this.props);
         this.mEntity.needInitContainer = this.mEntity.mSourceType != this.mSourceType;
         this.mEntity.mSourceType = this.mSourceType;
         this.mEntity.mDoubleSided = this.mDoubleSided;
         RegistryClient.sendToServer(new ProjectorAfterUpdateC2SPayload(this.mEntity, this.mRotation));
      }
   }

//#if MC >= 12110
   //$$ public boolean keyPressed(KeyEvent event) {
      //$$ if (this.mEntity == null) {
         //$$ return super.keyPressed(event);
      //$$ } else if (event.key() == 256) {
         //$$ this.minecraft.player.closeContainer();
         //$$ return true;
      //$$ } else {
         //$$ return this.mURLInput.keyPressed(event)
            //$$ || this.mURLInput.canConsumeInput()
            //$$ || this.mColorInput.keyPressed(event)
            //$$ || this.mColorInput.canConsumeInput()
            //$$ || this.mAngleXInput.keyPressed(event)
            //$$ || this.mAngleXInput.canConsumeInput()
            //$$ || this.mAngleYInput.keyPressed(event)
            //$$ || this.mAngleYInput.canConsumeInput()
            //$$ || this.mAngleZInput.keyPressed(event)
            //$$ || this.mAngleZInput.canConsumeInput()
            //$$ || this.mWidthInput.keyPressed(event)
            //$$ || this.mWidthInput.canConsumeInput()
            //$$ || this.mHeightInput.keyPressed(event)
            //$$ || this.mHeightInput.canConsumeInput()
            //$$ || this.mOffsetXInput.keyPressed(event)
            //$$ || this.mOffsetXInput.canConsumeInput()
            //$$ || this.mOffsetYInput.keyPressed(event)
            //$$ || this.mOffsetYInput.canConsumeInput()
            //$$ || this.mOffsetZInput.keyPressed(event)
            //$$ || this.mOffsetZInput.canConsumeInput()
            //$$ || super.keyPressed(event);
      //$$ }
   //$$ }
//#else
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.mEntity == null) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else if (keyCode == 256) {
         this.minecraft.player.closeContainer();
         return true;
      } else {
         return this.mURLInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mURLInput.canConsumeInput()
            || this.mColorInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mColorInput.canConsumeInput()
            || this.mAngleXInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mAngleXInput.canConsumeInput()
            || this.mAngleYInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mAngleYInput.canConsumeInput()
            || this.mAngleZInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mAngleZInput.canConsumeInput()
            || this.mWidthInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mWidthInput.canConsumeInput()
            || this.mHeightInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mHeightInput.canConsumeInput()
            || this.mOffsetXInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mOffsetXInput.canConsumeInput()
            || this.mOffsetYInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mOffsetYInput.canConsumeInput()
            || this.mOffsetZInput.keyPressed(keyCode, scanCode, modifiers)
            || this.mOffsetZInput.canConsumeInput()
            || super.keyPressed(keyCode, scanCode, modifiers);
      }
   }
//#endif

//#if MC >= 26_00_00
   //$$ public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      //$$ super.extractBackground(context, mouseX, mouseY, delta);
//#else
   protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
//#endif
//#if MC >= 12108
      //$$ context.blit(
         //$$ RenderPipelines.GUI_TEXTURED,
         //$$ GUI_TEXTURE,
         //$$ (this.width - this.imageWidth) / 2,
         //$$ (this.height - this.imageHeight) / 2,
         //$$ 0.0F,
         //$$ 0.0F,
         //$$ this.imageWidth,
         //$$ this.imageHeight,
         //$$ 256,
         //$$ 256
      //$$ );
//#elseif MC >= 12102
      //$$ context.blit(
         //$$ RenderType::guiTexturedOverlay,
         //$$ GUI_TEXTURE,
         //$$ (this.width - this.imageWidth) / 2,
         //$$ (this.height - this.imageHeight) / 2,
         //$$ 0.0F,
         //$$ 0.0F,
         //$$ this.imageWidth,
         //$$ this.imageHeight,
         //$$ 256,
         //$$ 256
      //$$ );
//#else
      context.blit(
         GUI_TEXTURE, (this.width - this.imageWidth) / 2, (this.height - this.imageHeight) / 2, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256
      );
//#endif
   }

//#if MC >= 26_00_00
   //$$ protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
//#else
   protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
//#endif
      if (this.mEntity != null) {
         int alpha = this.mImageColor >>> 24;
         if (alpha > 0) {
//#if MC >= 12108
            //$$ context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, 38, 131, 180.0F, 194.0F, 10, 10, 256, 256, this.mImageColor);
            //$$ context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, 82, 159, 180.0F, 194.0F, 17, 17, 256, 256, this.mImageColor);
//#elseif MC >= 12102
            //$$ context.blit(RenderType::guiTexturedOverlay, GUI_TEXTURE, 38, 131, 180.0F, 194.0F, 10, 10, 256, 256, this.mImageColor);
            //$$ context.blit(RenderType::guiTexturedOverlay, GUI_TEXTURE, 82, 159, 180.0F, 194.0F, 17, 17, 256, 256, this.mImageColor);
//#else
            int red = this.mImageColor >> 16 & 0xFF;
            int green = this.mImageColor >> 8 & 0xFF;
            int blue = this.mImageColor & 0xFF;
            RenderSystem.setShaderColor((float)red / 255.0F, (float)green / 255.0F, (float)blue / 255.0F, (float)alpha / 255.0F);
            context.blit(GUI_TEXTURE, 38, 131, 180, 194, 10, 10);
            context.blit(GUI_TEXTURE, 82, 159, 180, 194, 17, 17);
//#endif
         }

//#if MC >= 12108
         //$$ context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, 82, 159, 202.0F, (float)(194 - this.mRotation.ordinal() * 20), 17, 17, 256, 256);
//#elseif MC >= 12102
         //$$ context.blit(RenderType::guiTexturedOverlay, GUI_TEXTURE, 82, 159, 202.0F, (float)(194 - this.mRotation.ordinal() * 20), 17, 17, 256, 256);
//#else
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         context.blit(GUI_TEXTURE, 82, 159, 202, 194 - this.mRotation.ordinal() * 20, 17, 17);
//#endif
         drawCenteredStringWithoutShadow(context, this.font, IMAGE_TEXT, this.imageWidth / 2, -14);
//#if MC >= 26_00_00
         //$$ context.text(this.font, this.propIndex + 1 + "/" + this.props.size(), this.imageWidth / 2 - 50, 31, -12566464, false);
//#else
         context.drawString(this.font, this.propIndex + 1 + "/" + this.props.size(), this.imageWidth / 2 - 50, 31, -12566464, false);
//#endif
         drawCenteredStringWithoutShadow(context, this.font, OTHERS_TEXT, this.imageWidth / 2, 112);
         int offsetX = mouseX - (this.width - this.imageWidth) / 2;
         int offsetY = mouseY - (this.height - this.imageHeight) / 2;
         if (offsetX >= 9 && offsetY >= 27 && offsetX < 27 && offsetY < 46) {
            renderTooltip(context, this.font, switch (this.mSourceType) {
               case ResourceID -> ID_TEXT;
               case ContainerBlock -> CONTAINER_TEXT;
               default -> URL_TEXT;
            }, offsetX, offsetY);
         } else if (offsetX >= 149 && offsetY >= 7 && offsetX < 167 && offsetY < 26) {
            if (this.mSourceType != SourceType.ContainerBlock) {
               renderTooltip(context, this.font, EXPORT_TEXT, offsetX, offsetY);
            }
         } else if (offsetX >= 34 && offsetY >= 153 && offsetX < 52 && offsetY < 172) {
            renderTooltip(context, this.font, COLOR_TEXT, offsetX, offsetY);
         } else if (offsetX >= 9 && offsetY >= 66 && offsetX < 27 && offsetY < 85) {
            renderTooltip(context, this.font, WIDTH_TEXT, offsetX, offsetY);
         } else if (offsetX >= 90 && offsetY >= 66 && offsetX < 108 && offsetY < 85) {
            renderTooltip(context, this.font, HEIGHT_TEXT, offsetX, offsetY);
         } else if (offsetX >= 9 && offsetY >= 88 && offsetX < 27 && offsetY < 107) {
            renderTooltip(context, this.font, ANGLE_X_TEXT, offsetX, offsetY);
         } else if (offsetX >= 63 && offsetY >= 88 && offsetX < 81 && offsetY < 107) {
            renderTooltip(context, this.font, ANGLE_Y_TEXT, offsetX, offsetY);
         } else if (offsetX >= 117 && offsetY >= 88 && offsetX < 135 && offsetY < 107) {
            renderTooltip(context, this.font, ANGLE_Z_TEXT, offsetX, offsetY);
         } else if (offsetX >= 9 && offsetY >= 110 && offsetX < 27 && offsetY < 129) {
            renderTooltip(context, this.font, OFFSET_X_TEXT, offsetX, offsetY);
         } else if (offsetX >= 63 && offsetY >= 110 && offsetX < 81 && offsetY < 129) {
            renderTooltip(context, this.font, OFFSET_Y_TEXT, offsetX, offsetY);
         } else if (offsetX >= 117 && offsetY >= 110 && offsetX < 135 && offsetY < 129) {
            renderTooltip(context, this.font, OFFSET_Z_TEXT, offsetX, offsetY);
         } else if (offsetX >= 117 && offsetY >= 153 && offsetX < 135 && offsetY < 172) {
            renderTooltip(context, this.font, FLIP_TEXT, offsetX, offsetY);
         } else if (offsetX >= 142 && offsetY >= 153 && offsetX < 160 && offsetY < 172) {
            renderTooltip(context, this.font, ROTATE_TEXT, offsetX, offsetY);
         } else if (offsetX >= 9 && offsetY >= 153 && offsetX < 27 && offsetY < 172) {
            renderTooltip(context, this.font, SINGLE_DOUBLE_SIDED_TEXT, offsetX, offsetY);
         }
      }
   }

//#if MC >= 26_00_00
   //$$ private void renderTooltip(GuiGraphicsExtractor ctx, Font textRenderer, Component text, int offsetX, int offsetY) {
      //$$ ctx.setTooltipForNextFrame(textRenderer, text, offsetX + (this.width - this.imageWidth) / 2, offsetY + (this.height - this.imageHeight) / 2);
   //$$ }
//#elseif MC >= 12106
   //$$ private void renderTooltip(GuiGraphics ctx, Font textRenderer, Component text, int offsetX, int offsetY) {
      //$$ ctx.setTooltipForNextFrame(textRenderer, text, offsetX + (this.width - this.imageWidth) / 2, offsetY + (this.height - this.imageHeight) / 2);
   //$$ }
//#else
   private void renderTooltip(GuiGraphics ctx, Font textRenderer, Component text, int offsetX, int offsetY) {
      ctx.renderTooltip(textRenderer, text, offsetX, offsetY);
   }
//#endif

//#if MC >= 26_00_00
   //$$ private static void drawCenteredStringWithoutShadow(GuiGraphicsExtractor ctx, Font textRenderer, Component text, int centerX, int y) {
      //$$ FormattedCharSequence orderedText = text.getVisualOrderText();
      //$$ ctx.text(textRenderer, text, centerX - textRenderer.width(orderedText) / 2, y, -12566464, false);
   //$$ }
//#else
   private static void drawCenteredStringWithoutShadow(GuiGraphics ctx, Font textRenderer, Component text, int centerX, int y) {
      FormattedCharSequence orderedText = text.getVisualOrderText();
      ctx.drawString(textRenderer, text, centerX - textRenderer.width(orderedText) / 2, y, -12566464, false);
   }
//#endif

   private static float parseFloat(String text) {
      return (float)Math.round(Float.parseFloat(text) * 10000.0F) / 10000.0F;
   }

   private static String floatToString(float value) {
      return String.valueOf((float)Math.round(value * 10000.0F) / 10000.0F);
   }

   private static Vector3f relativeToAbsolute(Vector3f relatedOffset, Vec2 size, ProjectorBlock.InternalRotation rotation) {
      Vector4f center = new Vector4f(0.5F * size.x, 0.0F, 0.5F * size.y, 1.0F);
      center.mul(new Matrix4f().translate(relatedOffset.x(), -relatedOffset.z(), relatedOffset.y()));
      center.mul(new Matrix4f().translate(-0.5F, 0.0F, 0.5F - size.y));
      rotation.transform(center);
      return new Vector3f(center.x(), center.y(), center.z());
   }

   private static Vector3f absoluteToRelative(Vector3f absoluteOffset, Vec2 size, ProjectorBlock.InternalRotation rotation) {
      Vector4f center = new Vector4f(absoluteOffset, 1.0F);
      rotation.invert().transform(center);
      center.mul(new Matrix4f().translate(0.5F, 0.0F, -0.5F + size.y));
      center.mul(new Matrix4f().translate(-0.5F * size.x, 0.0F, -0.5F * size.y));
      return new Vector3f(center.x(), center.z(), -center.y());
   }

   public static class ScreenTexturedButtonWidget extends Button {
      public int u;
      public int v;
      public ResourceLocation texture;

      public ScreenTexturedButtonWidget(int x, int y, int width, int height, int u, int v, ResourceLocation texture, OnPress pressAction) {
         super(x, y, width, height, CommonComponents.EMPTY, pressAction, DEFAULT_NARRATION);
         this.u = u;
         this.v = v;
         this.texture = texture;
      }

//#if MC >= 26_00_00
      //$$ protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
//#elseif MC >= 12111
      //$$ protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float delta) {
//#else
      public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
//#endif
//#if MC >= 12108
         //$$ context.blit(
            //$$ RenderPipelines.GUI_TEXTURED, this.texture, this.getX(), this.getY(), (float)this.u, (float)this.v, this.width, this.height, 256, 256
         //$$ );
//#elseif MC >= 12102
         //$$ context.blit(
            //$$ RenderType::guiTexturedOverlay, this.texture, this.getX(), this.getY(), (float)this.u, (float)this.v, this.width, this.height, 256, 256
         //$$ );
//#else
         context.blit(this.texture, this.getX(), this.getY(), (float)this.u, (float)this.v, this.width, this.height, 256, 256);
//#endif
      }
   }
}
