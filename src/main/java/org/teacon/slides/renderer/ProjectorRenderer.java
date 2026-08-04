package org.teacon.slides.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
//#if MC >= 12110
//$$ import java.util.List;
//#endif
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
//#if MC >= 12110
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
//$$ import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//$$ import net.minecraft.client.renderer.state.CameraRenderState;
//#endif
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
//#if MC >= 12105
//$$ import net.minecraft.world.phys.Vec3;
//#endif
//#if MC >= 12110
//$$ import org.joml.Quaternionf;
//#endif
import org.teacon.slides.config.Config;
import org.teacon.slides.projector.ImageProperties;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.projector.SourceType;

//#if MC >= 12110
//$$ public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity, ProjectorRenderer.ProjectorRenderState> {
//#else
public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
//#endif
//#if MC >= 12110
	//$$ @Override
	//$$ public ProjectorRenderState createRenderState() {
		//$$ return new ProjectorRenderState();
	//$$ }

	//$$ @Override
	//$$ public void extractRenderState(ProjectorBlockEntity blockEntity, ProjectorRenderState state, float partialTick,
									 //$$ Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		//$$ BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
		//$$ state.location = blockEntity.getLocation();
		//$$ state.fromID = blockEntity.getFromID();
		//$$ state.mSourceType = blockEntity.mSourceType;
		//$$ state.mCNextLocation = blockEntity.mCNextLocation;
		//$$ state.mCNextFromID = blockEntity.mCNextFromID;
		//$$ state.color = blockEntity.mColor;
		//$$ state.doubleSided = blockEntity.mDoubleSided;
		//$$ state.imageProps = blockEntity.imageProps;
		//$$ state.slide = SlideState.getSlide(state.location, state.fromID);
		//$$ state.partialTick = partialTick;
		//$$ if (state.mSourceType == SourceType.ContainerBlock) {
			//$$ SlideState.cacheSlide(state.mCNextLocation, state.mCNextFromID);
		//$$ }
	//$$ }

	//$$ @Override
	//$$ public void submit(ProjectorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		//$$ Slide slide = state.slide;
		//$$ if (slide != null) {
			//$$ int color = state.color;
			//$$ if ((color & 0xFF000000) != 0 && !state.blockState.getValue(BlockStateProperties.POWERED)) {
				//$$ boolean doubleSided = state.doubleSided;
				//$$ boolean flipped = state.blockState.getValue(ProjectorBlock.ROTATION).isFlipped();
				//$$ for (ImageProperties prop : state.imageProps) {
					//$$ poseStack.pushPose();
					//$$ Pose lastPose = poseStack.last();
					//$$ Matrix4f pose = new Matrix4f(lastPose.pose());
					//$$ Matrix3f normal = new Matrix3f(lastPose.normal());
					//$$ transformToSlideSpace(pose, normal, prop, state.blockState);
					//$$ slide.submitTo(collector, pose, lastPose, prop.width, prop.height, color,
							//$$ state.lightCoords, OverlayTexture.NO_OVERLAY,
							//$$ flipped || doubleSided, !flipped || doubleSided,
							//$$ SlideState.getAnimationTick(), state.partialTick);
					//$$ poseStack.popPose();
				//$$ }
			//$$ }
		//$$ }
	//$$ }

	//$$ public boolean shouldRenderOffScreen() {
		//$$ return true;
	//$$ }
//#else
//#if MC >= 12105
   //$$ public void render(
      //$$ ProjectorBlockEntity blockEntity, float partialTick, PoseStack matrices, MultiBufferSource source, int packedLight, int packedOverlay, Vec3 cameraPosition
   //$$ ) {
//#else
   public void render(
      ProjectorBlockEntity blockEntity, float partialTick, PoseStack matrices, MultiBufferSource source, int packedLight, int packedOverlay
   ) {
//#endif
      BlockState state = blockEntity.getBlockState();
      Slide slide = SlideState.getSlide(blockEntity.getLocation(), blockEntity.getFromID());
      if (blockEntity.mSourceType == SourceType.ContainerBlock) {
         SlideState.cacheSlide(blockEntity.mCNextLocation, blockEntity.mCNextFromID);
      }

      if (slide != null) {
         int color = blockEntity.mColor;
         if ((color & 0xFF000000) != 0) {
            if (!(Boolean)state.getValue(BlockStateProperties.POWERED)) {
               boolean doubleSided = blockEntity.mDoubleSided;
               boolean flipped = ((ProjectorBlock.InternalRotation)state.getValue(ProjectorBlock.ROTATION)).isFlipped();

               for (ImageProperties prop : blockEntity.imageProps) {
                  matrices.pushPose();
                  Pose lastPose = matrices.last();
                  Matrix4f pose = new Matrix4f(lastPose.pose());
                  Matrix3f normal = new Matrix3f(lastPose.normal());
                  blockEntity.transformToSlideSpace(pose, normal, prop);
                  slide.render(
                     source,
                     pose,
                     lastPose,
                     prop.width,
                     prop.height,
                     color,
                     packedLight,
                     OverlayTexture.NO_OVERLAY,
                     flipped || doubleSided,
                     !flipped || doubleSided,
                     SlideState.getAnimationTick(),
                     partialTick
                  );
                  matrices.popPose();
               }
            }
         }
      }
   }

   public boolean rendersOutsideBoundingBox(ProjectorBlockEntity tile) {
      return true;
   }
//#endif

   public int getViewDistance() {
      return Config.getRenderDistance();
   }

//#if MC >= 12110
	//$$ private static void transformToSlideSpace(Matrix4f pose, Matrix3f normal, ImageProperties prop, BlockState state) {
		//$$ pose.translate(0.5F, 0.5F, 0.5F);
		//$$ Quaternionf q = prop.anglesUseDefault ? state.getValue(BlockStateProperties.FACING).getRotation() : prop.getRotation();
		//$$ pose.rotate(q);
		//$$ normal.rotate(q);
		//$$ pose.translate(0.0F, 0.5F, 0.0F);
		//$$ ProjectorBlock.InternalRotation rotation = state.getValue(ProjectorBlock.ROTATION);
		//$$ rotation.transform(pose);
		//$$ rotation.transform(normal);
		//$$ pose.translate(-0.5F, 0.0F, 0.5F - prop.height);
		//$$ pose.translate(prop.offsetX, -prop.offsetZ, prop.offsetY);
		//$$ pose.scale(prop.width, 1.0F, prop.height);
	//$$ }

	//$$ public static final class ProjectorRenderState extends BlockEntityRenderState {
		//$$ public String location;
		//$$ public boolean fromID;
		//$$ public SourceType mSourceType;
		//$$ public String mCNextLocation;
		//$$ public boolean mCNextFromID;
		//$$ public int color;
		//$$ public boolean doubleSided;
		//$$ public List<ImageProperties> imageProps;
		//$$ public Slide slide;
		//$$ public float partialTick;
	//$$ }
//#endif
}
