package org.teacon.slides.projector;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
//#if MC >= 12108
//$$ import net.minecraft.world.level.storage.ValueInput;
//$$ import net.minecraft.world.level.storage.ValueOutput;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.teacon.slides.Slideshow;
import org.teacon.slides.network.ProjectorImageInfoS2CPayload;
import org.teacon.slides.network.ProjectorOpenScreenPayload;
import org.teacon.slides.util.RegistryServer;
import org.teacon.slides.util.Utilities;

public final class ProjectorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<ProjectorOpenScreenPayload> {
   public SourceType mSourceType = SourceType.URL;
   public String mLocation = "";
   public int mColor = -1;
   public List<ImageProperties> imageProps;
   public boolean mDoubleSided = true;
   public Container mContainer = null;
   public int scanIndex = -1;
   public boolean needInitContainer = false;
   public boolean needHandleReadImage = false;
   public boolean flipBack = false;
   public boolean mCFromID = false;
   public String mCLocation = "";
   public boolean mCNextFromID = false;
   public String mCNextLocation = "";

   public ProjectorBlockEntity(BlockPos pos, BlockState state) {
      super(Slideshow.PROJECTOR_BLOCK_ENTITY, pos, state);
      this.imageProps = new ArrayList<>();
      this.imageProps.add(new ImageProperties());
   }

   public void transformToSlideSpace(Matrix4f pose, Matrix3f normal, ImageProperties prop) {
      BlockState state = this.getBlockState();
      pose.translate(0.5F, 0.5F, 0.5F);
      Quaternionf q = prop.anglesUseDefault ? ((Direction)state.getValue(BlockStateProperties.FACING)).getRotation() : prop.getRotation();
      pose.rotate(q);
      normal.rotate(q);
      pose.translate(0.0F, 0.5F, 0.0F);
      ProjectorBlock.InternalRotation rotation = (ProjectorBlock.InternalRotation)state.getValue(ProjectorBlock.ROTATION);
      rotation.transform(pose);
      rotation.transform(normal);
      pose.translate(-0.5F, 0.0F, 0.5F - prop.height);
      pose.translate(prop.offsetX, -prop.offsetZ, prop.offsetY);
      pose.scale(prop.width, 1.0F, prop.height);
   }

   private boolean tryReadImageItem(ItemStack item, boolean next) {
      if (item.is(Slideshow.IMAGE_ITEM)) {
         String loc = (String)item.get(Slideshow.LOCATION_COMPONENT);
         if (loc != null) {
            if (next) {
               this.mCNextFromID = Boolean.TRUE.equals(item.get(Slideshow.FROM_ID_COMPONENT));
               this.mCNextLocation = loc;
            } else {
               this.mCFromID = Boolean.TRUE.equals(item.get(Slideshow.FROM_ID_COMPONENT));
               this.mCLocation = loc;
            }

            return true;
         }
      }

      return false;
   }

   private void handleReadImage(boolean back) {
      int size = this.mContainer.getContainerSize();
      if (size > 0) {
         int start = back ? size + this.scanIndex - 1 : this.scanIndex + 1;
         int end = back ? this.scanIndex - 1 : size + this.scanIndex + 1;
         boolean found = false;
         if (back) {
            for (int j = start; j > end; j--) {
               int i = j % size;
               ItemStack item = this.mContainer.getItem(i);
               if (this.tryReadImageItem(item, false)) {
                  this.scanIndex = i;
                  found = true;
                  start = i + 1;
                  end = start + size;
                  break;
               }
            }
         } else {
            for (int jx = start; jx < end; jx++) {
               int i = jx % size;
               ItemStack item = this.mContainer.getItem(i);
               if (this.tryReadImageItem(item, false)) {
                  this.scanIndex = i;
                  found = true;
                  start = i + 1;
                  end = start + size;
                  break;
               }
            }
         }

         if (!found) {
            this.mCNextLocation = "";
         } else {
            for (int jxx = start; jxx < end; jxx++) {
               int i = jxx % size;
               ItemStack item = this.mContainer.getItem(i);
               if (this.tryReadImageItem(item, true)) {
                  return;
               }
            }
         }
      }
   }

   public boolean canFlip() {
      return this.mSourceType == SourceType.ContainerBlock && this.mContainer != null;
   }

   public boolean getFromID() {
      return this.mSourceType != SourceType.ContainerBlock ? this.mSourceType == SourceType.ResourceID : this.mCFromID;
   }

   public String getLocation() {
      return this.mSourceType != SourceType.ContainerBlock ? this.mLocation : this.mCLocation;
   }

   public void updateProps(List<ImageProperties> props) {
      this.imageProps = new ArrayList<>(props);
   }

//#if MC >= 12108
//$$    public void saveAdditional(ValueOutput output) {
//$$       this.saveCompound(output);
//$$       super.saveAdditional(output);
//$$    }
//$$
//$$    public void loadAdditional(ValueInput input) {
//$$       this.loadCompound(input);
//$$       super.loadAdditional(input);
//$$    }
//#else
   public void saveAdditional(CompoundTag nbt, Provider registries) {
      this.saveCompound(nbt);
      super.saveAdditional(nbt, registries);
   }

   public void loadAdditional(CompoundTag nbt, Provider registries) {
      this.loadCompound(nbt);
      super.loadAdditional(nbt, registries);
   }
//#endif

   public void saveCompound(CompoundTag compoundTag) {
      compoundTag.putString("SourceType", switch (this.mSourceType) {
         case ResourceID -> "resource_id";
         case ContainerBlock -> "container";
         default -> "url";
      });
      compoundTag.putString("ImageLocation", this.mLocation);
      compoundTag.putInt("Color", this.mColor);
      ListTag list = new ListTag();
      this.imageProps.forEach(imageProp -> list.add(imageProp.getNbt()));
      compoundTag.put("ImageProps", list);
      compoundTag.putBoolean("DoubleSided", this.mDoubleSided);
      compoundTag.putInt("ScanIndex", this.scanIndex);
      compoundTag.putBoolean("CFromID", this.mCFromID);
      compoundTag.putString("CLocation", this.mCLocation);
      compoundTag.putBoolean("CNextFromID", this.mCNextFromID);
      compoundTag.putString("CNextLocation", this.mCNextLocation);
   }

   public void loadCompound(CompoundTag compoundTag) {
//#if MC >= 12105
      //$$ String nbtList = compoundTag.getString("SourceType").orElse("");
//#else
      String nbtList = compoundTag.getString("SourceType");
//#endif

      this.mSourceType = switch (nbtList) {
         case "resource_id" -> SourceType.ResourceID;
         case "container" -> SourceType.ContainerBlock;
         default -> SourceType.URL;
      };
//#if MC >= 12105
      //$$ this.mLocation = compoundTag.getString("ImageLocation").orElse("");
      //$$ this.mColor = compoundTag.getInt("Color").orElse(0);
//#else
      this.mLocation = compoundTag.getString("ImageLocation");
      this.mColor = compoundTag.getInt("Color");
//#endif
      this.imageProps = new ArrayList<>();
      if (compoundTag.contains("ImageProps")) {
//#if MC >= 12105
         //$$ ListTag imagePropsList = compoundTag.getList("ImageProps").orElseGet(ListTag::new);
//#else
         ListTag imagePropsList = compoundTag.getList("ImageProps", 10);
//#endif
         imagePropsList.forEach(nbt -> {
            if (nbt instanceof CompoundTag compound) {
               this.imageProps.add(new ImageProperties(compound));
            }
         });
         if (this.imageProps.isEmpty()) {
            this.imageProps.add(new ImageProperties());
         }
      } else {
         this.imageProps.add(new ImageProperties());
      }

//#if MC >= 12105
      //$$ this.mDoubleSided = compoundTag.getBoolean("DoubleSided").orElse(false);
      //$$ this.scanIndex = compoundTag.getInt("ScanIndex").orElse(0);
      //$$ this.mCFromID = compoundTag.getBoolean("CFromID").orElse(false);
      //$$ this.mCLocation = compoundTag.getString("CLocation").orElse("");
      //$$ this.mCNextFromID = compoundTag.getBoolean("CNextFromID").orElse(false);
      //$$ this.mCNextLocation = compoundTag.getString("CNextLocation").orElse("");
//#else
      this.mDoubleSided = compoundTag.getBoolean("DoubleSided");
      this.scanIndex = compoundTag.getInt("ScanIndex");
      this.mCFromID = compoundTag.getBoolean("CFromID");
      this.mCLocation = compoundTag.getString("CLocation");
      this.mCNextFromID = compoundTag.getBoolean("CNextFromID");
      this.mCNextLocation = compoundTag.getString("CNextLocation");
//#endif
   }

//#if MC >= 12108
//$$    public void saveCompound(ValueOutput output) {
//$$       output.putString("SourceType", switch (this.mSourceType) {
//$$          case ResourceID -> "resource_id";
//$$          case ContainerBlock -> "container";
//$$          default -> "url";
//$$       });
//$$       output.putString("ImageLocation", this.mLocation);
//$$       output.putInt("Color", this.mColor);
//$$       ValueOutput.TypedOutputList<CompoundTag> list = output.list("ImageProps", CompoundTag.CODEC);
//$$       this.imageProps.forEach(imageProp -> list.add(imageProp.getNbt()));
//$$       output.putBoolean("DoubleSided", this.mDoubleSided);
//$$       output.putInt("ScanIndex", this.scanIndex);
//$$       output.putBoolean("CFromID", this.mCFromID);
//$$       output.putString("CLocation", this.mCLocation);
//$$       output.putBoolean("CNextFromID", this.mCNextFromID);
//$$       output.putString("CNextLocation", this.mCNextLocation);
//$$    }
//$$
//$$    public void loadCompound(ValueInput input) {
//$$       String nbtList = input.getStringOr("SourceType", "");
//$$       this.mSourceType = switch (nbtList) {
//$$          case "resource_id" -> SourceType.ResourceID;
//$$          case "container" -> SourceType.ContainerBlock;
//$$          default -> SourceType.URL;
//$$       };
//$$       this.mLocation = input.getStringOr("ImageLocation", "");
//$$       this.mColor = input.getIntOr("Color", 0);
//$$       this.imageProps = new ArrayList<>();
//$$       ValueInput.TypedInputList<CompoundTag> imagePropsList = input.listOrEmpty("ImageProps", CompoundTag.CODEC);
//$$       if (!imagePropsList.isEmpty()) {
//$$          imagePropsList.forEach(nbt -> this.imageProps.add(new ImageProperties(nbt)));
//$$          if (this.imageProps.isEmpty()) {
//$$             this.imageProps.add(new ImageProperties());
//$$          }
//$$       } else {
//$$          this.imageProps.add(new ImageProperties());
//$$       }
//$$       this.mDoubleSided = input.getBooleanOr("DoubleSided", false);
//$$       this.scanIndex = input.getIntOr("ScanIndex", 0);
//$$       this.mCFromID = input.getBooleanOr("CFromID", false);
//$$       this.mCLocation = input.getStringOr("CLocation", "");
//$$       this.mCNextFromID = input.getBooleanOr("CNextFromID", false);
//$$       this.mCNextLocation = input.getStringOr("CNextLocation", "");
//$$    }
//#endif

   public void sync() {
      Utilities.forPlayersTacking(this, player -> RegistryServer.sendToPlayer(player, new ProjectorImageInfoS2CPayload(this)));
   }

   public static void tick(Level world, BlockPos pos, ProjectorBlockEntity entity) {
      if (!world.isClientSide()) {
         if (entity.mSourceType == SourceType.ContainerBlock) {
            entity.mContainer = HopperBlockEntity.getContainerAt(world, tryParseCoor(entity.mLocation, pos));
            if (entity.mContainer == null) {
               entity.mCLocation = "";
               entity.mCNextLocation = "";
               entity.scanIndex = -1;
            } else if (entity.needInitContainer) {
               entity.scanIndex = -1;
               entity.handleReadImage(false);
               entity.needInitContainer = false;
               entity.setChanged();
               entity.sync();
            } else {
               if (entity.needHandleReadImage) {
                  if (entity.scanIndex < 0) {
                     entity.scanIndex = -1;
                     entity.handleReadImage(false);
                     return;
                  }

                  entity.handleReadImage(entity.flipBack);
                  entity.setChanged();
                  entity.sync();
                  entity.needHandleReadImage = false;
                  entity.flipBack = false;
               }
            }
         }
      }
   }

   private static BlockPos tryParseCoor(String coor, BlockPos pos) {
      try {
         String[] xyz = coor.split("\\s+");
         int[] nPos = new int[3];
         String x = xyz[0];
         if (x.startsWith("~")) {
            nPos[0] = pos.getX() + parseInt(x.substring(1));
         } else {
            nPos[0] = parseInt(x);
         }

         String y = xyz[1];
         if (y.startsWith("~")) {
            nPos[1] = pos.getY() + parseInt(y.substring(1));
         } else {
            nPos[1] = parseInt(y);
         }

         String z = xyz[2];
         if (z.startsWith("~")) {
            nPos[2] = pos.getZ() + parseInt(z.substring(1));
         } else {
            nPos[2] = parseInt(z);
         }

         return new BlockPos(nPos[0], nPos[1], nPos[2]);
      } catch (Exception var7) {
         return pos.below();
      }
   }

   private static int parseInt(String s) {
      return s.isEmpty() ? 0 : Integer.parseInt(s);
   }

   public CompoundTag getUpdateTag(Provider registries) {
//#if MC >= 12108
      //$$ return super.saveWithFullMetadata(registries);
//#else
      CompoundTag compoundTag = new CompoundTag();
      this.saveAdditional(compoundTag, registries);
      return compoundTag;
//#endif
   }

//#if MC < 12104
   public boolean onlyOpCanSetNbt() {
      return true;
   }
//#endif

   public Component getDisplayName() {
      return Component.literal("");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
      return new ProjectorScreenHandler(syncId, new ProjectorOpenScreenPayload(this.worldPosition));
   }

   public ProjectorOpenScreenPayload getScreenOpeningData(ServerPlayer serverPlayerEntity) {
      return new ProjectorOpenScreenPayload(this.worldPosition);
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
