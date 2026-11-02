package com.rudze.waystonetp.block;

import com.mojang.serialization.MapCodec;
import com.rudze.waystonetp.data.WaystoneNetwork;
import com.rudze.waystonetp.teleport.WaystoneTeleportService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A two-tall, unbreakable-in-survival pedestal. Right-clicking either half warps the
 * player to a random Waystone of the opposite {@link WaystoneVariant}. Placement of every
 * "A"/"B" is tracked in {@link WaystoneNetwork}, keyed by the lower half's position.
 */
public class WaystoneBlock extends BaseEntityBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final MapCodec<WaystoneBlock> CODEC_A = simpleCodec(props -> new WaystoneBlock(WaystoneVariant.A, props));
    private static final MapCodec<WaystoneBlock> CODEC_B = simpleCodec(props -> new WaystoneBlock(WaystoneVariant.B, props));

    private static final VoxelShape LOWER_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(1, 3, 1, 15, 7, 15),
            Block.box(2, 7, 2, 14, 9, 14),
            Block.box(3, 9, 3, 13, 16, 13)
    );

    private static final VoxelShape UPPER_SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 8, 13),
            Block.box(2, 8, 2, 14, 10, 14),
            Block.box(1, 10, 1, 15, 12, 15),
            Block.box(3, 12, 3, 13, 14, 13),
            Block.box(4, 14, 4, 12, 16, 12)
    );

    private final WaystoneVariant variant;

    public WaystoneBlock(WaystoneVariant variant, Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public WaystoneVariant getVariant() {
        return variant;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return variant == WaystoneVariant.A ? CODEC_A : CODEC_B;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaystoneBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return true;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() != Direction.Axis.Y
                || (half == DoubleBlockHalf.LOWER) != (direction == Direction.UP)
                || (neighborState.is(this) && neighborState.getValue(HALF) != half)) {
            return half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos)
                    ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        BlockPos basePos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        WaystoneTeleportService.activate(serverLevel, serverPlayer, variant, basePos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide
                && !state.is(oldState.getBlock())
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && level instanceof ServerLevel serverLevel) {
            WaystoneNetwork.get(serverLevel).register(variant, GlobalPos.of(serverLevel.dimension(), pos.immutable()));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide
                && !state.is(newState.getBlock())
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && level instanceof ServerLevel serverLevel) {
            WaystoneNetwork.get(serverLevel).unregister(variant, GlobalPos.of(serverLevel.dimension(), pos.immutable()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
