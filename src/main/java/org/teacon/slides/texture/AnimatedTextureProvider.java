package org.teacon.slides.texture;

import com.mojang.blaze3d.systems.RenderSystem;
//#if MC >= 12105
//$$ import com.mojang.blaze3d.platform.NativeImage;
//$$ import com.mojang.blaze3d.textures.AddressMode;
//$$ import com.mojang.blaze3d.textures.FilterMode;
//$$ import com.mojang.blaze3d.textures.GpuTexture;
//$$ import com.mojang.blaze3d.textures.TextureFormat;
//#endif
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.teacon.slides.Slideshow;
import org.teacon.slides.renderer.SlideRenderType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionException;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;

public final class AnimatedTextureProvider implements TextureProvider {

	private static final LZWDecoder gRenderThreadDecoder = new LZWDecoder();

	private static float sMaxAnisotropic = -1;

	private final GIFDecoder mDecoder;

//#if MC >= 12105
//$$ 	private GpuTexture mTexture;
//#else
	private int mTexture;
//#endif
	private final SlideRenderType mRenderType;

	private long mFrameStartTime;
	private long mFrameDelayTime;

	public AnimatedTextureProvider(byte[] data) {
//#if MC >= 12105
//$$ 		ByteBuffer buffer = null;
//$$ 		try {
//$$ 			mDecoder = new GIFDecoder(data, gRenderThreadDecoder, false);
//$$ 			final int width = mDecoder.getScreenWidth();
//$$ 			final int height = mDecoder.getScreenHeight();
//$$ 
//$$ 			buffer = MemoryUtil.memAlloc(width * height * 4);
//$$ 			mFrameDelayTime = mDecoder.decodeNextFrame(buffer);
//$$ 
//#if MC >= 12108
//$$ 			mTexture = RenderSystem.getDevice().createTexture("slide_show_animated",
//$$ 					GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, TextureFormat.RGBA8, width, height, 1, 1);
//$$ 			mTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
//$$ 			mTexture.setTextureFilter(FilterMode.NEAREST, FilterMode.LINEAR, false);
//#if MC >= 12110
//$$ 			RenderSystem.getDevice().createCommandEncoder().writeToTexture(
//$$ 					mTexture, buffer.rewind(), NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
//#else
//$$ 			RenderSystem.getDevice().createCommandEncoder().writeToTexture(
//$$ 					mTexture, buffer.rewind().asIntBuffer(), NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
//#endif
//#elseif MC >= 12105
//$$ 			mTexture = RenderSystem.getDevice().createTexture("slide_show_animated", TextureFormat.RGBA8, width, height, 1);
//$$ 			mTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
//$$ 			mTexture.setTextureFilter(FilterMode.NEAREST, FilterMode.LINEAR, false);
//$$ 			RenderSystem.getDevice().createCommandEncoder().writeToTexture(
//$$ 					mTexture, buffer.rewind().asIntBuffer(), NativeImage.Format.RGBA, 0, 0, 0, width, height);
//#endif
//$$ 			mRenderType = new SlideRenderType(mTexture);
//$$ 		} catch (Throwable t) {
//$$ 			close();
//$$ 			throw new CompletionException(t);
//$$ 		} finally {
//$$ 			MemoryUtil.memFree(buffer);
//$$ 		}
//#else
		if (sMaxAnisotropic < 0) {
			GLCapabilities caps = GL.getCapabilities();
			if (caps.OpenGL46 ||
					caps.GL_ARB_texture_filter_anisotropic ||
					caps.GL_EXT_texture_filter_anisotropic) {
				sMaxAnisotropic = Math.max(1, glGetFloat(GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY));
				Slideshow.LOGGER.info("Max anisotropic: {}", sMaxAnisotropic);
			} else {
				sMaxAnisotropic = 0;
			}
		}
		ByteBuffer buffer = null;
		try {
			mDecoder = new GIFDecoder(data, gRenderThreadDecoder, false);
			final int width = mDecoder.getScreenWidth();
			final int height = mDecoder.getScreenHeight();

			buffer = MemoryUtil.memAlloc(width * height * 4);
			mFrameDelayTime = mDecoder.decodeNextFrame(buffer);

			mTexture = glGenTextures();
			RenderSystem.bindTexture(mTexture);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

			if (sMaxAnisotropic > 0) {
				glTexParameterf(GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAX_ANISOTROPY, sMaxAnisotropic);
			}

			glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
			glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
			mRenderType = new SlideRenderType(mTexture);
		} catch (Throwable t) {
			close();
			throw new CompletionException(t);
		} finally {
			MemoryUtil.memFree(buffer);
		}
//#endif
	}

	@NotNull
	@Override
	public SlideRenderType updateAndGet(long tick, float partialTick) {
		long timeMillis = (long) ((tick + partialTick) * 50);
		if (mFrameStartTime == 0) {
			mFrameStartTime = timeMillis;
		} else if (mFrameStartTime + mFrameDelayTime <= timeMillis) {
			ByteBuffer buffer = null;
			try {
				final int width = getWidth();
				final int height = getHeight();
				buffer = MemoryUtil.memAlloc(width * height * 4);
				mFrameDelayTime = mDecoder.decodeNextFrame(buffer);
//#if MC >= 12110
//$$ 			RenderSystem.getDevice().createCommandEncoder().writeToTexture(
//$$ 					mTexture, buffer.rewind(), NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
//#elseif MC >= 12108
//$$ 			RenderSystem.getDevice().createCommandEncoder().writeToTexture(
//$$ 					mTexture, buffer.rewind().asIntBuffer(), NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
//#elseif MC >= 12105
//$$ 				RenderSystem.getDevice().createCommandEncoder().writeToTexture(
//$$ 						mTexture, buffer.rewind().asIntBuffer(), NativeImage.Format.RGBA, 0, 0, 0, width, height);
//#else
				RenderSystem.bindTexture(mTexture);
				glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
				glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
				glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
				glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
				glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
//#endif
			} catch (IOException e) {
				mFrameDelayTime = Integer.MAX_VALUE;
			} finally {
				MemoryUtil.memFree(buffer);
			}
			mFrameStartTime = timeMillis;
		}
		return mRenderType;
	}

	@Override
	public int getWidth() {
		return mDecoder.getScreenWidth();
	}

	@Override
	public int getHeight() {
		return mDecoder.getScreenHeight();
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
}
