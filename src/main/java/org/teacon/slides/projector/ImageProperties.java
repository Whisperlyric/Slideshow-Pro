package org.teacon.slides.projector;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ImageProperties {
   public float width = 1.0F;
   public float height = 1.0F;
   public float offsetX = 0.0F;
   public float offsetY = 0.0F;
   public float offsetZ = 0.0F;
   public boolean anglesUseDefault = true;
   public int angleX = 0;
   public int angleY = 0;
   public int angleZ = 0;

   public ImageProperties() {
   }

   public ImageProperties(CompoundTag nbt) {
//#if MC >= 12105
      //$$ this.width = nbt.getFloat("Width").orElse(0.0F);
      //$$ this.height = nbt.getFloat("Height").orElse(0.0F);
      //$$ this.offsetX = nbt.getFloat("OffsetX").orElse(0.0F);
      //$$ this.offsetY = nbt.getFloat("OffsetY").orElse(0.0F);
      //$$ this.offsetZ = nbt.getFloat("OffsetZ").orElse(0.0F);
      //$$ if (nbt.contains("Angles")) {
         //$$ CompoundTag nbtCompound = nbt.getCompound("Angles").orElseGet(CompoundTag::new);
         //$$ this.angleX = nbtCompound.getInt("X").orElse(0);
         //$$ this.angleY = nbtCompound.getInt("Y").orElse(0);
         //$$ this.angleZ = nbtCompound.getInt("Z").orElse(0);
         //$$ this.anglesUseDefault = false;
      //$$ }
//#else
      this.width = nbt.getFloat("Width");
      this.height = nbt.getFloat("Height");
      this.offsetX = nbt.getFloat("OffsetX");
      this.offsetY = nbt.getFloat("OffsetY");
      this.offsetZ = nbt.getFloat("OffsetZ");
      if (nbt.contains("Angles")) {
         CompoundTag nbtCompound = nbt.getCompound("Angles");
         this.angleX = nbtCompound.getInt("X");
         this.angleY = nbtCompound.getInt("Y");
         this.angleZ = nbtCompound.getInt("Z");
         this.anglesUseDefault = false;
      }
//#endif
   }

   public void updateFrom(Vec2 size, Vector3f offset) {
      this.width = size.x;
      this.height = size.y;
      this.offsetX = offset.x;
      this.offsetY = offset.y;
      this.offsetZ = offset.z;
   }

   public Vec2 getSize() {
      return new Vec2(this.width, this.height);
   }

   public Vector3f getOffset() {
      return new Vector3f(this.offsetX, this.offsetY, this.offsetZ);
   }

   public void updateFrom(Vector3f offset) {
      this.offsetX = offset.x;
      this.offsetY = offset.y;
      this.offsetZ = offset.z;
   }

   public void updateAngles(Direction direction) {
      switch (direction) {
         case DOWN:
            this.angleX = 180;
         case UP:
         case NORTH:
            this.angleX = 90;
            this.angleZ = 180;
            break;
         case SOUTH:
            this.angleX = 90;
            break;
         case WEST:
            this.angleX = 90;
            this.angleZ = 90;
            break;
         case EAST:
            this.angleX = 90;
            this.angleZ = -90;
         default:
             break;
      }
   }

   public CompoundTag getNbt() {
      CompoundTag nbt = new CompoundTag();
      nbt.putFloat("Width", this.width);
      nbt.putFloat("Height", this.height);
      nbt.putFloat("OffsetX", this.offsetX);
      nbt.putFloat("OffsetY", this.offsetY);
      nbt.putFloat("OffsetZ", this.offsetZ);
      if (!this.anglesUseDefault) {
         CompoundTag nbtCompound = new CompoundTag();
         nbtCompound.putInt("X", this.angleX);
         nbtCompound.putInt("Y", this.angleY);
         nbtCompound.putInt("Z", this.angleZ);
         nbt.put("Angles", nbtCompound);
      }

      return nbt;
   }

   public ImageProperties copy() {
      ImageProperties prop = new ImageProperties();
      prop.width = this.width;
      prop.height = this.height;
      prop.offsetX = this.offsetX;
      prop.offsetY = this.offsetY;
      prop.offsetZ = this.offsetZ;
      prop.anglesUseDefault = this.anglesUseDefault;
      if (!this.anglesUseDefault) {
         prop.angleX = this.angleX;
         prop.angleY = this.angleY;
         prop.angleZ = this.angleZ;
      }

      return prop;
   }

   public Quaternionf getRotation() {
      return new Quaternionf()
         .rotationXYZ(
            (float) Math.PI * (float)this.angleX / 180.0F, (float) Math.PI * (float)this.angleY / 180.0F, (float) Math.PI * (float)this.angleZ / 180.0F
         );
   }
}
