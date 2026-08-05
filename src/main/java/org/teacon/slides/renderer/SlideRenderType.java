package org.teacon.slides.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
//#if MC >= 12111
//$$ import com.mojang.blaze3d.textures.GpuSampler;
//$$ import com.mojang.blaze3d.textures.GpuTexture;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.RenderPipelines;
//$$ import net.minecraft.client.renderer.rendertype.RenderSetup;
//$$ import net.minecraft.client.renderer.rendertype.RenderType;
//$$ import net.minecraft.client.renderer.texture.AbstractTexture;
//#elseif MC >= 12108
//$$ import com.mojang.blaze3d.buffers.GpuBuffer;
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice;
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline;
//$$ import com.mojang.blaze3d.pipeline.RenderTarget;
//$$ import com.mojang.blaze3d.systems.RenderPass;
//$$ import com.mojang.blaze3d.systems.ScissorState;
//$$ import com.mojang.blaze3d.textures.GpuTexture;
//$$ import com.mojang.blaze3d.textures.GpuTextureView;
//$$ import com.mojang.blaze3d.vertex.MeshData;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.RenderPipelines;
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import org.joml.Vector4f;
//$$ import org.joml.Vector3f;
//$$ import java.util.OptionalDouble;
//$$ import java.util.OptionalInt;
//$$ import java.util.function.Supplier;
//#elseif MC >= 12105
//$$ import com.mojang.blaze3d.buffers.GpuBuffer;
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline;
//$$ import com.mojang.blaze3d.pipeline.RenderTarget;
//$$ import com.mojang.blaze3d.systems.RenderPass;
//$$ import com.mojang.blaze3d.textures.GpuTexture;
//$$ import com.mojang.blaze3d.vertex.MeshData;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import java.util.OptionalDouble;
//$$ import java.util.OptionalInt;
//$$ import java.util.function.Supplier;
//#else
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
//#endif
import net.minecraft.resources.ResourceLocation;
import org.teacon.slides.Slideshow;

//#if MC >= 12111
//$$ public class SlideRenderType {
//$$ 	private static final String ID_ICON = Slideshow.ID + "icon";
//$$
//$$ 	private final RenderType delegate;
//$$
//$$ 	public SlideRenderType(GpuTexture texture, GpuSampler sampler) {
//#if MC >= 26_00_00
//$$ 		this.delegate = RenderType.create(Slideshow.ID, RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
//$$ 				.withTexture("Sampler0", registerTexture(texture, sampler))
//$$ 				.useLightmap()
//$$ 				.createRenderSetup());
//$$ 	}
//$$
//$$ 	SlideRenderType(Identifier texture) {
//$$ 		this.delegate = RenderType.create(ID_ICON, RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
//$$ 				.withTexture("Sampler0", texture)
//$$ 				.useLightmap()
//$$ 				.createRenderSetup());
//$$ 	}
//#else
//$$ 		this.delegate = RenderType.create(Slideshow.ID, RenderSetup.builder(RenderPipelines.TRANSLUCENT_MOVING_BLOCK)
//$$ 				.withTexture("Sampler0", registerTexture(texture, sampler))
//$$ 				.useLightmap()
//$$ 				.createRenderSetup());
//$$ 	}
//$$
//$$ 	SlideRenderType(Identifier texture) {
//$$ 		this.delegate = RenderType.create(ID_ICON, RenderSetup.builder(RenderPipelines.TRANSLUCENT_MOVING_BLOCK)
//$$ 				.withTexture("Sampler0", texture)
//$$ 				.useLightmap()
//$$ 				.createRenderSetup());
//$$ 	}
//#endif
//$$
//$$ 	public RenderType asRenderType() {
//$$ 		return this.delegate;
//$$ 	}
//$$
//$$ 	private static Identifier registerTexture(GpuTexture texture, GpuSampler sampler) {
//$$ 		Identifier location = Identifier.fromNamespaceAndPath(Slideshow.ID, "textures/generated/" + sTextureIndex++);
//$$ 		Minecraft.getInstance().getTextureManager().register(location, new SlideAbstractTexture(texture, sampler));
//$$ 		return location;
//$$ 	}
//$$
//$$ 	private static int sTextureIndex;
//$$
//$$ 	private static final class SlideAbstractTexture extends AbstractTexture {
//$$ 		private SlideAbstractTexture(GpuTexture texture, GpuSampler sampler) {
//$$ 			this.texture = texture;
//$$ 			this.textureView = RenderSystem.getDevice().createTextureView(texture);
//$$ 			this.sampler = sampler;
//$$ 		}
//$$ 	}
//$$ }
//#elseif MC >= 12108
//$$ public class SlideRenderType extends RenderType {
//$$ 	private static final String ID_ICON = Slideshow.ID + "icon";
//$$ 
//$$ 	private final RenderPipeline pipeline;
//$$ 	private final Supplier<GpuTexture> texture;
//$$ 	private final GpuTextureView textureView;
//$$ 
//$$ 	public SlideRenderType(GpuTexture texture) {
//$$ 		this(Slideshow.ID, () -> texture);
//$$ 	}
//$$ 
//$$ 	SlideRenderType(ResourceLocation texture) {
//$$ 		this(ID_ICON, () -> Minecraft.getInstance().getTextureManager().getTexture(texture).getTexture());
//$$ 	}
//$$ 
//$$ 	private SlideRenderType(String name, Supplier<GpuTexture> texture) {
//$$ 		this(name, texture, RenderSystem.getDevice().createTextureView(texture.get()));
//$$ 	}
//$$ 
//$$ 	private SlideRenderType(String name, Supplier<GpuTexture> texture, GpuTextureView textureView) {
//$$ 		super(name, 256, false, true,
//$$ 				() -> RenderSystem.setShaderTexture(0, textureView),
//$$ 				() -> RenderSystem.setShaderTexture(0, (GpuTextureView) null));
//$$ 		this.pipeline = RenderPipelines.TRANSLUCENT;
//$$ 		this.texture = texture;
//$$ 		this.textureView = textureView;
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public void draw(MeshData meshData) {
//$$ 		setupRenderState();
//$$ 		GpuBufferSlice slice = RenderSystem.getDynamicUniforms().writeTransform(
//$$ 				RenderSystem.getModelViewMatrix(), new Vector4f(1, 1, 1, 1),
//#if MC >= 12110
//$$ 				new Vector3f(),
//#else
//$$ 				RenderSystem.getModelOffset(),
//#endif
//$$ 				RenderSystem.getTextureMatrix(), RenderSystem.getShaderLineWidth());
//$$ 		GpuBuffer vertexBuffer = this.pipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
//$$ 		GpuBuffer indexBuffer;
//$$ 		VertexFormat.IndexType indexType;
//$$ 		if (meshData.indexBuffer() == null) {
//$$ 			RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
//$$ 			indexBuffer = sequential.getBuffer(meshData.drawState().indexCount());
//$$ 			indexType = sequential.type();
//$$ 		} else {
//$$ 			indexBuffer = this.pipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
//$$ 			indexType = meshData.drawState().indexType();
//$$ 		}
//$$ 		RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
//$$ 		GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
//$$ 				? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
//$$ 		GpuTextureView depthView = target.useDepth
//$$ 				? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView())
//$$ 				: null;
//$$ 		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
//$$ 				() -> Slideshow.ID, colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
//$$ 			pass.setPipeline(this.pipeline);
//$$ 			ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
//$$ 			if (scissor.enabled()) {
//$$ 				pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
//$$ 			}
//$$ 			RenderSystem.bindDefaultUniforms(pass);
//$$ 			pass.setUniform("DynamicTransforms", slice);
//$$ 			pass.setVertexBuffer(0, vertexBuffer);
//$$ 			for (int i = 0; i < 12; i++) {
//$$ 				GpuTextureView t = RenderSystem.getShaderTexture(i);
//$$ 				if (t != null) {
//$$ 					pass.bindSampler("Sampler" + i, t);
//$$ 				}
//$$ 			}
//$$ 			pass.setIndexBuffer(indexBuffer, indexType);
//$$ 			pass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
//$$ 		}
//$$ 		meshData.close();
//$$ 		clearRenderState();
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public VertexFormat format() {
//$$ 		return this.pipeline.getVertexFormat();
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public VertexFormat.Mode mode() {
//$$ 		return this.pipeline.getVertexFormatMode();
//$$ 	}
//$$
//#if MC >= 12110
//$$ 	@Override
//$$ 	public RenderPipeline pipeline() {
//$$ 		return this.pipeline;
//$$ 	}
//#endif
//$$ }
//#elseif MC >= 12105
//$$ public class SlideRenderType extends RenderType {
//$$ 	private static final String ID_ICON = Slideshow.ID + "icon";
//$$ 
//$$ 	private final RenderPipeline pipeline;
//$$ 	private final Supplier<GpuTexture> texture;
//$$ 
//$$ 	public SlideRenderType(GpuTexture texture) {
//$$ 		this(Slideshow.ID, () -> texture);
//$$ 	}
//$$ 
//$$ 	SlideRenderType(ResourceLocation texture) {
//$$ 		this(ID_ICON, () -> Minecraft.getInstance().getTextureManager().getTexture(texture).getTexture());
//$$ 	}
//$$ 
//$$ 	private SlideRenderType(String name, Supplier<GpuTexture> texture) {
//$$ 		super(name, 256, false, true,
//$$ 				() -> RenderSystem.setShaderTexture(0, texture.get()),
//$$ 				() -> RenderSystem.setShaderTexture(0, (GpuTexture) null));
//$$ 		this.pipeline = RenderType.translucent().getRenderPipeline();
//$$ 		this.texture = texture;
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public void draw(MeshData meshData) {
//$$ 		RenderPipeline pipeline = getRenderPipeline();
//$$ 		setupRenderState();
//$$ 		MeshData data = meshData;
//$$ 		GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(data.vertexBuffer());
//$$ 		GpuBuffer indexBuffer;
//$$ 		VertexFormat.IndexType indexType;
//$$ 		if (data.indexBuffer() == null) {
//$$ 			RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(data.drawState().mode());
//$$ 			indexBuffer = sequential.getBuffer(data.drawState().indexCount());
//$$ 			indexType = sequential.type();
//$$ 		} else {
//$$ 			indexBuffer = pipeline.getVertexFormat().uploadImmediateIndexBuffer(data.indexBuffer());
//$$ 			indexType = data.drawState().indexType();
//$$ 		}
//$$ 		RenderTarget target = getRenderTarget();
//$$ 		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
//$$ 				target.getColorTexture(), OptionalInt.empty(),
//$$ 				target.useDepth ? target.getDepthTexture() : null, OptionalDouble.empty())) {
//$$ 			pass.setPipeline(pipeline);
//$$ 			pass.setVertexBuffer(0, vertexBuffer);
//$$ 			if (RenderSystem.SCISSOR_STATE.isEnabled()) {
//$$ 				pass.enableScissor(RenderSystem.SCISSOR_STATE);
//$$ 			}
//$$ 			for (int i = 0; i < 12; i++) {
//$$ 				GpuTexture t = RenderSystem.getShaderTexture(i);
//$$ 				if (t != null) {
//$$ 					pass.bindSampler("Sampler" + i, t);
//$$ 				}
//$$ 			}
//$$ 			pass.setIndexBuffer(indexBuffer, indexType);
//$$ 			pass.drawIndexed(0, data.drawState().indexCount());
//$$ 		}
//$$ 		data.close();
//$$ 		clearRenderState();
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public RenderTarget getRenderTarget() {
//$$ 		return Minecraft.getInstance().getMainRenderTarget();
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public RenderPipeline getRenderPipeline() {
//$$ 		return pipeline;
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public VertexFormat format() {
//$$ 		return pipeline.getVertexFormat();
//$$ 	}
//$$ 
//$$ 	@Override
//$$ 	public VertexFormat.Mode mode() {
//$$ 		return pipeline.getVertexFormatMode();
//$$ 	}
//$$ }
//#else
public class SlideRenderType extends RenderType.CompositeRenderType {

	private static final String ID_ICON = Slideshow.ID + "icon";

	public SlideRenderType(int texture) {
		super(Slideshow.ID, DefaultVertexFormat.BLOCK,
				VertexFormat.Mode.QUADS, 256, false, true,
				RenderType.CompositeState.builder()
//#if MC >= 12102
						//$$ .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_SHADER)
//#else
						.setShaderState(RenderStateShard.RENDERTYPE_TEXT_SHADER)
//#endif
						.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
						.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
						.setCullState(RenderStateShard.CULL)
						.setLightmapState(RenderStateShard.LIGHTMAP)
						.setOverlayState(RenderStateShard.NO_OVERLAY)
						.setLayeringState(RenderStateShard.NO_LAYERING)
						.setOutputState(RenderStateShard.MAIN_TARGET)
						.setTexturingState(RenderStateShard.DEFAULT_TEXTURING)
						.setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
						.setLineState(RenderStateShard.DEFAULT_LINE)
						.setColorLogicState(RenderStateShard.NO_COLOR_LOGIC)
						.createCompositeState(true));
		Runnable baseAction = this.setupState;
		this.setupState = () -> {
			baseAction.run();
			RenderSystem.setShaderTexture(0, texture);
		};
	}

	SlideRenderType(ResourceLocation texture) {
		super(ID_ICON, DefaultVertexFormat.BLOCK,
				VertexFormat.Mode.QUADS, 256, false, true,
				RenderType.CompositeState.builder()
//#if MC >= 12102
						//$$ .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_SHADER)
//#else
						.setShaderState(RenderStateShard.RENDERTYPE_TEXT_SHADER)
//#endif
						.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
						.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
						.setCullState(RenderStateShard.CULL)
						.setLightmapState(RenderStateShard.LIGHTMAP)
						.setOverlayState(RenderStateShard.NO_OVERLAY)
						.setLayeringState(RenderStateShard.NO_LAYERING)
						.setOutputState(RenderStateShard.MAIN_TARGET)
						.setTexturingState(RenderStateShard.DEFAULT_TEXTURING)
						.setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
						.setLineState(RenderStateShard.DEFAULT_LINE)
						.setColorLogicState(RenderStateShard.NO_COLOR_LOGIC)
						.createCompositeState(true));
		Runnable baseAction = this.setupState;
		this.setupState = () -> {
			baseAction.run();
			RenderSystem.setShaderTexture(0, texture);
		};
	}
}
//#endif
