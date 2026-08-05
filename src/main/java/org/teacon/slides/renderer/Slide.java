package org.teacon.slides.renderer;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//#if MC >= 12110
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//#endif
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.Slideshow;
import org.teacon.slides.texture.TextureProvider;

public abstract class Slide implements AutoCloseable {

	public abstract void render(@NotNull MultiBufferSource source, @NotNull Matrix4f matrix,
								@NotNull PoseStack.Pose normal, float width, float height, int color,
								int light, int overlay, boolean front, boolean back, long tick, float partialTick);

//#if MC >= 12110
//$$ 	@NotNull
//$$ 	public abstract RenderType getRenderType(long tick, float partialTick);
//$$
//$$ 	public abstract void submitTo(SubmitNodeCollector collector, @NotNull Matrix4f matrix,
//$$ 								  @NotNull PoseStack.Pose normal, float width, float height, int color,
//$$ 								  int light, int overlay, boolean front, boolean back, long tick, float partialTick);
//#endif

	@Override
	public void close() {
	}

	public int getWidth() {
		return 0;
	}

	public int getHeight() {
		return 0;
	}

	public int getGPUMemorySize() {
		return (getWidth() * getHeight()) << 2;
	}

	@NotNull
	static Slide make(TextureProvider texture) {
		return new Image(texture);
	}

	public static Slide empty() {
		return Icon.DEFAULT_EMPTY;
	}

	public static Slide failed() {
		return Icon.DEFAULT_FAILED;
	}

	public static Slide loading() {
		return Icon.DEFAULT_LOADING;
	}

	public static final class Image extends Slide {

		private final TextureProvider mTexture;

		private Image(TextureProvider texture) {
			mTexture = texture;
		}

		@Override
		public void render(@NotNull MultiBufferSource source, @NotNull Matrix4f matrix,
						   @NotNull PoseStack.Pose normal, float width, float height, int color,
						   int light, int overlay, boolean front, boolean back, long tick, float partialTick) {
//#if MC >= 12111
			//$$ renderQuads(source.getBuffer(mTexture.updateAndGet(tick, partialTick).asRenderType()), matrix, normal, width, height, color, light, overlay, front, back, tick, partialTick);
//#else
			renderQuads(source.getBuffer(mTexture.updateAndGet(tick, partialTick)), matrix, normal, width, height, color, light, overlay, front, back, tick, partialTick);
//#endif
		}

//#if MC >= 12110
		//$$ @NotNull
		//$$ @Override
		//$$ public RenderType getRenderType(long tick, float partialTick) {
//#if MC >= 12111
			//$$ return mTexture.updateAndGet(tick, partialTick).asRenderType();
//#else
			//$$ return mTexture.updateAndGet(tick, partialTick);
//#endif
		//$$ }
		//$$
		//$$ @Override
		//$$ public void submitTo(SubmitNodeCollector collector, @NotNull Matrix4f matrix,
								//$$ @NotNull PoseStack.Pose normal, float width, float height, int color,
								//$$ int light, int overlay, boolean front, boolean back, long tick, float partialTick) {
//#if MC >= 12111
			//$$ RenderType renderType = mTexture.updateAndGet(tick, partialTick).asRenderType();
//#else
			//$$ RenderType renderType = mTexture.updateAndGet(tick, partialTick);
//#endif
			//$$ collector.submitCustomGeometry(new PoseStack(), renderType, (pose, consumer) ->
					//$$ renderQuads(consumer, matrix, normal, width, height, color, light, overlay, front, back, tick, partialTick));
		//$$ }
//#endif

		private void renderQuads(@NotNull VertexConsumer builder, @NotNull Matrix4f matrix,
								 @NotNull PoseStack.Pose normal, float width, float height, int color,
								 int light, int overlay, boolean front, boolean back, long tick, float partialTick) {
			int red = (color >> 16) & 255, green = (color >> 8) & 255, blue = color & 255, alpha = color >>> 24;
			if (front) {
				builder.addVertex(matrix, 0, 1 / 192F, 1)
						.setColor(red, green, blue, alpha).setUv(0, 1)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1, 1 / 192F, 1)
						.setColor(red, green, blue, alpha).setUv(1, 1)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1, 1 / 192F, 0)
						.setColor(red, green, blue, alpha).setUv(1, 0)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 0, 1 / 192F, 0)
						.setColor(red, green, blue, alpha).setUv(0, 0)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
			}
			if (back) {
				builder.addVertex(matrix, 0, -1 / 256F, 0)
						.setColor(red, green, blue, alpha).setUv(0, 0)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1, -1 / 256F, 0)
						.setColor(red, green, blue, alpha).setUv(1, 0)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1, -1 / 256F, 1)
						.setColor(red, green, blue, alpha).setUv(1, 1)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 0, -1 / 256F, 1)
						.setColor(red, green, blue, alpha).setUv(0, 1)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
			}
		}

		@Override
		public void close() {
			mTexture.close();
		}

		@Override
		public int getWidth() {
			return mTexture.getWidth();
		}

		@Override
		public int getHeight() {
			return mTexture.getHeight();
		}

		@Override
		public String toString() {
			return "ImageSlide{texture=" + mTexture + "}";
		}
	}

	public static final class Icon extends Slide {

		private static final ResourceLocation
				BACKGROUND = ResourceLocation.fromNamespaceAndPath(Slideshow.ID, "textures/gui/slide_default.png"),
				ICON_EMPTY = ResourceLocation.fromNamespaceAndPath(Slideshow.ID, "textures/gui/slide_icon_empty.png"),
				ICON_FAILED = ResourceLocation.fromNamespaceAndPath(Slideshow.ID, "textures/gui/slide_icon_failed.png"),
				ICON_LOADING = ResourceLocation.fromNamespaceAndPath(Slideshow.ID, "textures/gui/slide_icon_loading.png");

//#if MC >= 12111
		//$$ private static final RenderType sBackgroundRenderType = new SlideRenderType(BACKGROUND).asRenderType();
//#else
		private static final RenderType sBackgroundRenderType = new SlideRenderType(BACKGROUND);
//#endif

		private static final Icon DEFAULT_EMPTY = new Icon(ICON_EMPTY);
		private static final Icon DEFAULT_FAILED = new Icon(ICON_FAILED);
		private static final Icon DEFAULT_LOADING = new Icon(ICON_LOADING);

		private final RenderType mIconRenderType;

		private Icon(ResourceLocation icon) {
//#if MC >= 12111
			//$$ mIconRenderType = new SlideRenderType(icon).asRenderType();
//#else
			mIconRenderType = new SlideRenderType(icon);
//#endif
		}

		private static float getFactor(float width, float height) {
			return Math.min(width, height) / (24 + Mth.fastInvCubeRoot(0.00390625F / (width * width + height * height)));
		}

		@Override
		public void render(@NotNull MultiBufferSource source, @NotNull Matrix4f matrix,
						   @NotNull PoseStack.Pose normal, float width, float height, int color,
						   int light, int overlay, boolean front, boolean back, long tick, float partialTick) {
			if(front || back) {
				int alpha = color >>> 24;
				if (alpha > 0) {
					float factor = getFactor(width, height);
					int xSize = Math.round(width / factor), ySize = Math.round(height / factor);
					renderIcon(source.getBuffer(mIconRenderType), matrix, normal, alpha, light, xSize, ySize, front, back);
					renderBackground(source.getBuffer(sBackgroundRenderType), matrix, normal, alpha, light, xSize, ySize, front, back);
				}
			}
		}

//#if MC >= 12110
		//$$ @NotNull
		//$$ @Override
		//$$ public RenderType getRenderType(long tick, float partialTick) {
			//$$ return mIconRenderType;
		//$$ }
		//$$
		//$$ @Override
		//$$ public void submitTo(SubmitNodeCollector collector, @NotNull Matrix4f matrix,
								//$$ @NotNull PoseStack.Pose normal, float width, float height, int color,
								//$$ int light, int overlay, boolean front, boolean back, long tick, float partialTick) {
			//$$ if (front || back) {
				//$$ int alpha = color >>> 24;
				//$$ if (alpha > 0) {
					//$$ float factor = getFactor(width, height);
					//$$ int xSize = Math.round(width / factor), ySize = Math.round(height / factor);
					//$$ collector.submitCustomGeometry(new PoseStack(), mIconRenderType, (pose, consumer) ->
							//$$ renderIcon(consumer, matrix, normal, alpha, light, xSize, ySize, front, back));
					//$$ collector.submitCustomGeometry(new PoseStack(), sBackgroundRenderType, (pose, consumer) ->
							//$$ renderBackground(consumer, matrix, normal, alpha, light, xSize, ySize, front, back));
				//$$ }
			//$$ }
		//$$ }
//#endif

		private void renderIcon(@NotNull VertexConsumer builder, Matrix4f matrix, PoseStack.Pose normal,
								int alpha, int light, int xSize, int ySize, boolean front, boolean back) {
			float x1 = (1F - 19F / xSize) / 2F, x2 = 1F - x1, y1 = (1F - 16F / ySize) / 2F, y2 = 1F - y1;
			if (front) {
				builder.addVertex(matrix, x1, 1F / 128F, y2)
						.setColor(255, 255, 255, alpha).setUv(0F, 1F)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 128F, y2)
						.setColor(255, 255, 255, alpha).setUv(1F, 1F)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 128F, y1)
						.setColor(255, 255, 255, alpha).setUv(1F, 0F)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 128F, y1)
						.setColor(255, 255, 255, alpha).setUv(0F, 0F)
						.setLight(light)
						.setNormal(normal, 0, 1, 0);
			}
			if (back) {
				builder.addVertex(matrix, x1, -1F / 128F, y1)
						.setColor(255, 255, 255, alpha).setUv(0F, 0F)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 128F, y1)
						.setColor(255, 255, 255, alpha).setUv(1F, 0F)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 128F, y2)
						.setColor(255, 255, 255, alpha).setUv(1F, 1F)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 128F, y2)
						.setColor(255, 255, 255, alpha).setUv(0F, 1F)
						.setLight(light)
						.setNormal(normal, 0, -1, 0);
			}
		}

		private void renderBackground(@NotNull VertexConsumer builder, Matrix4f matrix, PoseStack.Pose normal,
									  int alpha, int light, int xSize, int ySize, boolean front, boolean back) {
			float u1 = 9F / 19F, u2 = 10F / 19F, x1 = 9F / xSize, x2 = 1F - x1, y1 = 9F / ySize, y2 = 1F - y1;
			// code for generate
			/*
			 * #!/usr/bin/python3
			 *
			 * xs = [('0F', '0F'), ('x1', 'u1'), ('x2', 'u2'), ('1F', '1F')]
			 * ys = [('0F', '0F'), ('y1', 'u1'), ('y2', 'u2'), ('1F', '1F')]
			 *
			 * fmt = '    builder.vertex(matrix, {}, {}, {}).color(255, 255, 255, alpha)\n'
			 * fmt += '            .uv({}, {}).uv2(light)\n'
			 * fmt += '            .normal(normal, 0, {}, 0).endVertex();'
			 *
			 * print('if (front) {')
			 * for i in range(3):
			 *     for j in range(3):
			 *         a, b, c, d = xs[i], xs[i + 1], ys[j], ys[j + 1]
			 *         for k, l in [(a, d), (b, d), (b, c), (a, c)]:
			 *             print(fmt.format(k[0], '1F / 256F', l[0], k[1], l[1], 1))
			 * print('}')
			 *
			 * print('if (back) {')
			 * for i in range(3):
			 *     for j in range(3):
			 *         a, b, c, d = xs[i], xs[i + 1], ys[j], ys[j + 1]
			 *         for k, l in [(a, c), (b, c), (b, d), (a, d)]:
			 *             print(fmt.format(k[0], '-1F / 256F', l[0], k[1], l[1], -1))
			 * print('}')
			 */
			if (front) {
				builder.addVertex(matrix, 0F, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(0F, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u1, 0F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 0F, 1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(0F, 0F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 0F, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(0F, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 0F, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(0F, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 0F, 1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(0F, 1F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u1, 1F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 0F, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(0F, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u2, 0F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u1, 0F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u1, 1F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u2, 1F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x1, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1F, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(1F, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1F, 1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(1F, 0F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u2, 0F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1F, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(1F, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1F, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(1F, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u2, 1F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1F, 1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(1F, 1F).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, 1F, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(1F, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
				builder.addVertex(matrix, x2, 1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, 1, 0);
			}
			if (back) {
				builder.addVertex(matrix, 0F, -1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(0F, 0F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u1, 0F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 0F, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(0F, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 0F, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(0F, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 0F, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(0F, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 0F, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(0F, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u1, 1F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 0F, -1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(0F, 1F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u1, 0F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u2, 0F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u1, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u1, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u2, 1F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x1, -1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u1, 1F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(u2, 0F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1F, -1F / 256F, 0F).setColor(255, 255, 255, alpha)
						.setUv(1F, 0F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1F, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(1F, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(u2, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1F, -1F / 256F, y1).setColor(255, 255, 255, alpha)
						.setUv(1F, u1).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1F, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(1F, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(u2, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1F, -1F / 256F, y2).setColor(255, 255, 255, alpha)
						.setUv(1F, u2).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, 1F, -1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(1F, 1F).setLight(light)
						.setNormal(normal, 0, -1, 0);
				builder.addVertex(matrix, x2, -1F / 256F, 1F).setColor(255, 255, 255, alpha)
						.setUv(u2, 1F).setLight(light)
						.setNormal(normal, 0, -1, 0);
			}
		}

		@Override
		public String toString() {
			return "IconSlide{" +
					"iconRenderType=" + mIconRenderType +
					'}';
		}
	}
}
