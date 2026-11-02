package fr.rudze.furniture.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The invisible companion cell a {@link FurnitureBlock} drags along for its extra solid/light
 * offsets ("barriers"/"lights"). A single registered block, shared by every furniture definition;
 * which cell it belongs to and whether it's solid/how bright it is is per-instance state
 * (blockstate properties + a back-reference to the master in its block entity).
 *
 * <p>Never placed directly by a player - only ever spawned by {@link FurnitureBlock#setPlacedBy}.
 */
public class FurniturePartBlock extends BaseEntityBlock {

    public static final MapCodec<FurniturePartBlock> CODEC = simpleCodec(FurniturePartBlock::new);
    public static final BooleanProperty SOLID = BooleanProperty.create("solid");
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public FurniturePartBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LEVEL)).pushReaction(PushReaction.BLOCK));
        registerDefaultState(this.stateDefinition.any().setValue(SOLID, true).setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<FurniturePartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOLID, LEVEL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurniturePartBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SOLID) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && !movedByPiston
                && level.getBlockEntity(pos) instanceof FurniturePartBlockEntity part
                && part.getMaster() != null) {
            BlockPos masterPos = part.getMaster();
            if (level.getBlockState(masterPos).getBlock() instanceof FurnitureBlock) {
                // destroyBlock (not removeBlock) so breaking a statue's arm still drops the item,
                // exactly as if the player had mined its base cell instead.
                level.destroyBlock(masterPos, true, null);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
