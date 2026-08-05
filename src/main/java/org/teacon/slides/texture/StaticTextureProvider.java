package org.teacon.slides.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
//#if MC >= 12105
//$$ import com.mojang.blaze3d.systems.CommandEncoder;
//$$ import com.mojang.blaze3d.textures.AddressMode;
//$$ import com.mojang.blaze3d.textures.FilterMode;
//$$ import com.mojang.blaze3d.textures.GpuTexture;
//$$ import com.mojang.blaze3d.textures.TextureFormat;
//$$ import net.minecraft.client.renderer.texture.MipmapGenerator;
//#endif
//#if MC >= 12111
//$$ import com.mojang.blaze3d.textures.GpuSampler;
//$$ import net.minecraft.client.renderer.texture.MipmapStrategy;
//$$ import net.minecraft.resources.Identifier;
//$$ import org.teacon.slides.Slideshow;
//#endif
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.teacon.slides.renderer.SlideRenderType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CompletionException;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public final class StaticTextureProvider implements TextureProvider {

//#if MC >= 12105
//$$ 	private GpuTexture mTexture;
//#else
	private int mTexture;
//#endif
	private final SlideRenderType mRenderType;
	private final int mWidth, mHeight;

	public StaticTextureProvider(byte @NotNull [] data, boolean isWebP) {
		ByteBuffer buffer = isWebP ? MemoryUtil.memAlloc(0) : MemoryUtil.memAlloc(data.length).put(data).rewind();

		try (NativeImage image = isWebP ? WebPDecoder.toNativeImage(data) : createNativeImage(NativeImage.Format.RGBA, buffer)) {
			if(image == null) {
				throw new IOException();
			}
			mWidth = image.getWidth();
			mHeight = image.getHeight();
			final int maxLevel = Math.min(31 - Integer.numberOfLeadingZeros(Math.max(mWidth, mHeight)), 4);

//#if MC >= 12108
//$$ 			mTexture = RenderSystem.getDevice().createTexture("slide_show_static",
//$$ 					GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, TextureFormat.RGBA8, mWidth, mHeight, 1, maxLevel + 1);
//#if MC >= 12111
//$$ 			GpuSampler sampler = RenderSystem.getSamplerCache().getSampler(
//$$ 					AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.LINEAR, true);
//$$ 			NativeImage[] levels = MipmapGenerator.generateMipLevels(
//$$ 					Identifier.fromNamespaceAndPath(Slideshow.ID, "textures/generated/slide_show_static"),
//$$ 					new NativeImage[]{image}, maxLevel, MipmapStrategy.AUTO, 0.5F);
//#else
//$$ 			mTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
//$$ 			mTexture.setTextureFilter(FilterMode.NEAREST, FilterMode.LINEAR, true);
//$$ 			NativeImage[] levels = MipmapGenerator.generateMipLevels(new NativeImage[]{image}, maxLevel);
//#endif
//$$ 			CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
//$$ 			for (int level = 0; level <= maxLevel; ++level) {
//$$ 				encoder.writeToTexture(mTexture, levels[level], level, 0, 0, 0,
//$$ 						Math.max(1, mWidth >> level), Math.max(1, mHeight >> level), 0, 0);
//$$ 			}
//$$ 			for (int level = 1; level <= maxLevel; ++level) {
//$$ 				levels[level].close();
//$$ 			}
//#if MC >= 12111
//$$ 			mRenderType = new SlideRenderType(mTexture, sampler);
//#else
//$$ 			mRenderType = new SlideRenderType(mTexture);
//#endif
//#elseif MC >= 12105
//$$ 			mTexture = RenderSystem.getDevice().createTexture("slide_show_static", TextureFormat.RGBA8, mWidth, mHeight, maxLevel + 1);
//$$ 			mTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
//$$ 			mTexture.setTextureFilter(FilterMode.NEAREST, FilterMode.LINEAR, true);
//$$ 			NativeImage[] levels = MipmapGenerator.generateMipLevels(new NativeImage[]{image}, maxLevel);
//$$ 			CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
//$$ 			for (int level = 0; level <= maxLevel; ++level) {
//$$ 				encoder.writeToTexture(mTexture, levels[level], level, 0, 0,
//$$ 						Math.max(1, mWidth >> level), Math.max(1, mHeight >> level), 0, 0);
//$$ 			}
//$$ 			for (int level = 1; level <= maxLevel; ++level) {
//$$ 				levels[level].close();
//$$ 			}
//$$ 			mRenderType = new SlideRenderType(mTexture);
//#else
			mTexture = glGenTextures();
			RenderSystem.bindTexture(mTexture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, maxLevel);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);

			for (int level = 0; level <= maxLevel; ++level) {
				glTexImage2D(GL_TEXTURE_2D, level, GL_RGBA8, mWidth >> level, mHeight >> level,
						0, GL_RED, GL_UNSIGNED_BYTE, (IntBuffer) null);
			}

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

			glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
			glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

			try (image) {
				glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, mWidth, mHeight,
						GL_RGBA, GL_UNSIGNED_BYTE, image.pixels);
			}

			glGenerateMipmap(GL_TEXTURE_2D);
			mRenderType = new SlideRenderType(mTexture);
//#endif
		} catch (Throwable t) {
			close();
			throw new CompletionException(t);
		} finally {
			MemoryUtil.memFree(buffer);
		}
	}

	@Override
	public @NotNull SlideRenderType updateAndGet(long tick, float partialTick) {
		return mRenderType;
	}

	@Override
	public int getWidth() {
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	@Override
	public void close() {
//#if MC >= 12105
//$$ 		if (mTexture != null) {
//$$ 			mTexture.close();
//$$ 		}
//$$ 		mTexture = null;
//#else
		if (mTexture != 0) {
			RenderSystem.deleteTexture(mTexture);
		}
		mTexture = 0;
//#endif
	}

	public static NativeImage createNativeImage(@Nullable NativeImage.Format format, ByteBuffer buffer) throws IOException {
		if (format != null && !format.supportedByStb()) {
			throw new UnsupportedOperationException("Don't know how to read format " + format);
		} else if (MemoryUtil.memAddress(buffer) == 0L) {
			throw new IllegalArgumentException("Invalid buffer");
		} else {
			try (MemoryStack memoryStack = MemoryStack.stackPush()) {
				IntBuffer intBuffer = memoryStack.mallocInt(1);
				IntBuffer intBuffer2 = memoryStack.mallocInt(1);
				IntBuffer intBuffer3 = memoryStack.mallocInt(1);
				ByteBuffer byteBuffer = STBImage.stbi_load_from_memory(buffer, intBuffer, intBuffer2, intBuffer3, format == null ? 0 : format.components());
				if (byteBuffer == null) {
					throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
				} else {
					return new NativeImage(format == null ? NativeImage.Format.getStbFormat(intBuffer3.get(0)) : format, intBuffer.get(0), intBuffer2.get(0), true, MemoryUtil.memAddress(byteBuffer));
				}
			}
		}
	}

}
