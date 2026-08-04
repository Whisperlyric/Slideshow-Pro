package org.teacon.slides.texture;

import com.mojang.blaze3d.platform.NativeImage;
import dev.matrixlab.webp4j.internal.NativeWebP;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.teacon.slides.Slideshow;

public final class WebPDecoder {
   private static boolean initialized = false;
   private static boolean disabled = true;

   public static void init() {
      if (!initialized) {
         try {
            disabled = !NativeWebP.isAvailable();
         } catch (ExceptionInInitializerError var1) {
            Slideshow.LOGGER.warn("Failed to load WebP native library, WebPDecoder is disabled! ", var1);
         } catch (Throwable var2) {
            Slideshow.LOGGER.warn("WebPDecoder is disabled! ", var2);
         }

         initialized = true;
      }
   }

   public static boolean checkMagic(@NotNull byte[] buf) {
      if (buf.length >= 12) {
         ByteBuffer wr = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
         boolean riff = wr.getInt() == 1179011410;
         boolean size = wr.getInt() == buf.length - 8;
         boolean webp = wr.getInt() == 1346520407;
         boolean vp8_ = ArrayUtils.contains(new int[]{1480085590, 1278758998, 540561494}, wr.getInt());
         return riff && size && webp && vp8_;
      } else {
         return false;
      }
   }

   public static NativeImage toNativeImage(@NotNull byte[] buf) {
      if (disabled) {
         return null;
      } else {
         try {
            int[] dimensions = new int[2];
            byte[] output = decodeImage(buf, dimensions);
            NativeImage nativeImage = new NativeImage(dimensions[0], dimensions[1], false);
            int size = dimensions[0] * dimensions[1] * 4;
            ByteBuffer nativeBuffer = MemoryUtil.memByteBuffer(nativeImage.pixels, Math.toIntExact((long)size));
            Objects.requireNonNull(nativeBuffer).put(output);
            return nativeImage;
         } catch (Throwable var6) {
            return null;
         }
      }
   }

   private static byte[] decodeImage(byte[] webPData, int[] dimensions) throws IOException {
      if (webPData != null && webPData.length != 0) {
         boolean success0 = NativeWebP.getInfo(webPData, dimensions);
         if (!success0) {
            throw new IOException("Failed to retrieve WebP image information.");
         } else {
            int width = dimensions[0];
            int height = dimensions[1];
            int outputStride = width * 4;
            byte[] outputBuffer = new byte[height * outputStride];
            boolean success1 = NativeWebP.decodeRGBAInto(webPData, outputBuffer, outputStride);
            if (!success1) {
               throw new IOException("Failed to decode WebP data into RGB buffer.");
            } else {
               return outputBuffer;
            }
         }
      } else {
         throw new IllegalArgumentException("The input WebP data cannot be null or empty.");
      }
   }

   static {
      init();
   }
}
