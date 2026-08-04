package org.teacon.slides.projector;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
//#if MC >= 12104
//$$ import net.minecraft.world.level.redstone.Orientation;
//#endif
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.teacon.slides.Slideshow;
import org.teacon.slides.config.ServerConfig;

public final class ProjectorBlock extends BaseEntityBlock implements EntityBlock {
   public static final EnumProperty<ProjectorBlock.InternalRotation> ROTATION = EnumProperty.create("rotation", ProjectorBlock.InternalRotation.class);
   public static final EnumProperty<Direction> BASE = EnumProperty.create("base", Direction.class, Plane.VERTICAL);
   private static final VoxelShape SHAPE_WITH_BASE_UP = Block.box(0.0, 4.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape SHAPE_WITH_BASE_DOWN = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

   public ProjectorBlock(Properties settings) {
      super(settings);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(BASE, Direction.DOWN)).setValue(BlockStateProperties.FACING, Direction.EAST))
               .setValue(BlockStateProperties.POWERED, Boolean.FALSE))
            .setValue(ROTATION, ProjectorBlock.InternalRotation.NONE)
      );
   }

   @NotNull
   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return switch ((Direction)state.getValue(BASE)) {
         case DOWN -> SHAPE_WITH_BASE_DOWN;
         case UP -> SHAPE_WITH_BASE_UP;
         default -> throw new AssertionError();
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{BASE, BlockStateProperties.FACING, BlockStateProperties.POWERED, ROTATION});
   }

   @NotNull
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction facing = ctx.getNearestLookingDirection().getOpposite();
      Direction horizontalFacing = ctx.getNearestLookingVerticalDirection().getOpposite();
      Direction base = Arrays.stream(ctx.getNearestLookingDirections()).filter(Plane.VERTICAL).findFirst().orElse(Direction.DOWN);
      ProjectorBlock.InternalRotation rotation = ProjectorBlock.InternalRotation.VALUES[4
         + Math.floorMod(facing.getStepY() * horizontalFacing.get2DDataValue(), 4)];
      return (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(BASE, base)).setValue(BlockStateProperties.FACING, facing))
            .setValue(BlockStateProperties.POWERED, Boolean.FALSE))
         .setValue(ROTATION, rotation);
   }

//#if MC >= 12104
   //$$ protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
//#else
   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
//#endif
      boolean powered = world.hasNeighborSignal(pos);
      if (powered != (Boolean)state.getValue(BlockStateProperties.POWERED)) {
         world.setBlockAndUpdate(pos, (BlockState)state.setValue(BlockStateProperties.POWERED, powered));
      }
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
      if (!oldState.is(state.getBlock())) {
         boolean powered = world.hasNeighborSignal(pos);
         if (powered != (Boolean)state.getValue(BlockStateProperties.POWERED)) {
            world.setBlockAndUpdate(pos, (BlockState)state.setValue(BlockStateProperties.POWERED, powered));
         }
      }
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
//#if MC >= 12110
      //$$ return world.isClientSide()
         //$$ ? null
         //$$ : createTickerHelper(type, Slideshow.PROJECTOR_BLOCK_ENTITY, (world1, pos, state1, entity) -> ProjectorBlockEntity.tick(world1, pos, entity));
//#else
      return world.isClientSide
         ? null
         : createTickerHelper(type, Slideshow.PROJECTOR_BLOCK_ENTITY, (world1, pos, state1, entity) -> ProjectorBlockEntity.tick(world1, pos, entity));
//#endif
   }

   @NotNull
   public BlockState mirror(BlockState state, Mirror mirror) {
      Direction direction = (Direction)state.getValue(BlockStateProperties.FACING);

      return switch (direction) {
         case DOWN, UP -> (BlockState)state.setValue(ROTATION, ((ProjectorBlock.InternalRotation)state.getValue(ROTATION)).compose(Rotation.CLOCKWISE_180));
         default -> (BlockState)state.setValue(BlockStateProperties.FACING, mirror.getRotation(direction).rotate(direction));
      };
   }

   @NotNull
   public BlockState rotate(BlockState state, Rotation rotation) {
      Direction direction = (Direction)state.getValue(BlockStateProperties.FACING);

      return switch (direction) {
         case DOWN, UP -> (BlockState)state.setValue(ROTATION, ((ProjectorBlock.InternalRotation)state.getValue(ROTATION)).compose(rotation));
         default -> (BlockState)state.setValue(BlockStateProperties.FACING, rotation.rotate(direction));
      };
   }

   protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      if (!world.isClientSide()) {
         if (!hasProjectorPermission((ServerPlayer)player)) {
            return InteractionResult.FAIL;
         }

         if (player.getMainHandItem().is(Slideshow.FLIPPER_ITEM) || player.getOffhandItem().is(Slideshow.FLIPPER_ITEM)) {
            return InteractionResult.PASS;
         }

         MenuProvider factory = this.getMenuProvider(state, world, pos);
         if (factory != null) {
            player.openMenu(factory);
         }
      }

      return InteractionResult.SUCCESS;
   }

   public static boolean hasProjectorPermission(ServerPlayer serverPlayer) {
      return (!ServerConfig.isProjectorRequiresCreative() || serverPlayer.isCreative())
         && serverPlayer.hasPermissions(ServerConfig.getProjectorPermission());
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(ProjectorBlock::new);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   private static boolean hasPermission(GameType gameType) {
      return gameType == GameType.CREATIVE || gameType == GameType.SURVIVAL;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ProjectorBlockEntity(pos, state);
   }

   public static enum InternalRotation implements StringRepresentable {
      NONE(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F),
      CLOCKWISE_90(0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F),
      CLOCKWISE_180(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F),
      COUNTERCLOCKWISE_90(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F),
      HORIZONTAL_FLIPPED(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F),
      DIAGONAL_FLIPPED(0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F),
      VERTICAL_FLIPPED(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F),
      ANTI_DIAGONAL_FLIPPED(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F);

      public static final ProjectorBlock.InternalRotation[] VALUES = values();
      private static final int[] INV_INDICES = new int[]{0, 3, 2, 1, 4, 5, 6, 7};
      private static final int[] FLIP_INDICES = new int[]{4, 7, 6, 5, 0, 3, 2, 1};
      private static final int[][] ROTATION_INDICES = new int[][]{
         {0, 1, 2, 3, 4, 5, 6, 7}, {1, 2, 3, 0, 5, 6, 7, 4}, {2, 3, 0, 1, 6, 7, 4, 5}, {3, 0, 1, 2, 7, 4, 5, 6}
      };
      private final String mSerializedName;
      private final Matrix4f mMatrix;
      private final Matrix3f mNormal;

      private InternalRotation(
         float m00, float m10, float m20, float m30, float m01, float m11, float m21, float m31, float m02, float m12, float m22, float m32
      ) {
         this.mMatrix = new Matrix4f(m00, m01, m02, 0.0F, m10, m11, m12, 0.0F, m20, m21, m22, 0.0F, m30, m31, m32, 1.0F);
         this.mNormal = new Matrix3f(m00, m01, m02, m10, m11, m12, m20, m21, m22);
         this.mSerializedName = this.name().toLowerCase(Locale.ROOT);
      }

      public ProjectorBlock.InternalRotation compose(Rotation rotation) {
         return VALUES[ROTATION_INDICES[rotation.ordinal()][this.ordinal()]];
      }

      public ProjectorBlock.InternalRotation flip() {
         return VALUES[FLIP_INDICES[this.ordinal()]];
      }

      public ProjectorBlock.InternalRotation invert() {
         return VALUES[INV_INDICES[this.ordinal()]];
      }

      public boolean isFlipped() {
         return this.ordinal() >= 4;
      }

      public void transform(Vector4f vector) {
         vector.mul(this.mMatrix);
      }

      public void transform(Matrix4f poseMatrix) {
         poseMatrix.mul(this.mMatrix);
      }

      public void transform(Matrix3f normalMatrix) {
         normalMatrix.mul(this.mNormal);
      }

      @NotNull
      public final String getSerializedName() {
         return this.mSerializedName;
      }
   }
}
