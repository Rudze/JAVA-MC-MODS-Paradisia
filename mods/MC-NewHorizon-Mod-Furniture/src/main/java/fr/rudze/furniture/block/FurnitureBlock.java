package fr.rudze.furniture.block;

import com.mojang.serialization.MapCodec;
import fr.rudze.furniture.furniture.FurnitureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Generic decorative "furniture" block: one instance per {@link FurnitureDefinition}, all sharing
 * this same class. Handles floor/wall/ceiling placement (rotated to face the player, like a
 * chest), the extra solid/light cells it drags along ({@link FurniturePartBlock}), an optional
 * toggleable light, and its sounds - everything a converted config needs, driven entirely by data
 * instead of a bespoke subclass.
 *
 * <p>The block model itself is never drawn ({@link RenderShape#INVISIBLE}); a
 * {@code BlockEntityRenderer} draws the item's own model dynamically instead (see
 * {@code FurnitureBlockEntityRenderer}). That's the whole drag-and-drop point: drop the raw model
 * + texture, write the config, done - no geometry math, ever.
 *
 * <p>Floor/ceiling placement stores two things: {@link #FACING} (nearest of the 4 cardinal
 * directions - grid-safe, used to rotate {@link FurnitureDefinition.Part} offsets onto real block
 * positions) and {@link #ROTATION_8} (nearest of 8, i.e. cardinals plus diagonals - purely visual,
 * read by the renderer for the item's yaw). Wall placement only ever needs {@link #FACING} - a
 * wall-mounted piece can only face directly away from the one wall it's flush against, so a
 * diagonal doesn't mean anything there.
 */
public class FurnitureBlock extends BaseEntityBlock {

    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ROTATION_8 = IntegerProperty.create("rotation_8", 0, 7);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = BlockStateProperties.LIT;

    /**
     * {@link #createBlockStateDefinition} is invoked by {@code Block}'s own constructor, i.e. before
     * our {@code definition} field is assigned - so it can't just read {@code this.definition}. This
     * thread-local hands it the definition anyway, set for the duration of the {@code super(...)} call
     * that triggers it. Construction is synchronous and single-threaded (mod init), so this is safe.
     */
    private static final ThreadLocal<FurnitureDefinition> PENDING = new ThreadLocal<>();

    private final FurnitureDefinition definition;
    private final boolean hasFaceProperty;
    private final boolean hasRotation8Property;
    private final boolean hasLitProperty;
    private final VoxelShape mainShape;

    public FurnitureBlock(FurnitureDefinition definition, Properties properties) {
        super(withPending(definition, configure(definition, properties)));
        PENDING.remove();
        this.definition = definition;
        this.hasFaceProperty = definition.placementCount() > 1;
        // Diagonal rotation is purely visual - it never touches how `parts` offsets are placed
        // (those always snap to the grid-safe 4-way `facing`). For a piece with a *horizontal*
        // part (e.g. a bench with extra seats to its left/right), that would desync the visual
        // angle from the physical companion cells, so those pieces stay 4-way only.
        this.hasRotation8Property = (definition.placeFloor() || definition.placeRoof()) && hasOnlyVerticalParts(definition);
        this.hasLitProperty = definition.toggleableLight();
        this.mainShape = buildShape(definition.mainShape());

        BlockState state = this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH);
        if (hasFaceProperty) {
            state = state.setValue(FACE, fixedOrDefaultFace());
        }
        if (hasRotation8Property) {
            state = state.setValue(ROTATION_8, 0);
        }
        if (hasLitProperty) {
            state = state.setValue(LIT, true);
        }
        registerDefaultState(state);
    }

    private static Properties withPending(FurnitureDefinition definition, Properties properties) {
        PENDING.set(definition);
        return properties;
    }

    private static Properties configure(FurnitureDefinition definition, Properties properties) {
        SoundEvent place = resolveSound(definition.placeSound(), SoundEvents.WOOD_PLACE);
        SoundEvent brk = resolveSound(definition.breakSound(), SoundEvents.WOOD_BREAK);
        return properties
                .sound(new SoundType(1.0F, 1.0F, brk, brk, place, brk, brk))
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(s -> definition.toggleableLight()
                        ? (s.getValue(LIT) ? definition.mainLight() : 0)
                        : definition.mainLight());
    }

    private static SoundEvent resolveSound(String id, SoundEvent fallback) {
        try {
            return BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse(id)).orElse(fallback);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static VoxelShape buildShape(FurnitureDefinition.MainShape shape) {
        if (shape.solid()) {
            return Shapes.block();
        }
        if (shape.box() != null) {
            double[] b = shape.box();
            return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        return Shapes.empty();
    }

    private AttachFace fixedOrDefaultFace() {
        if (definition.placeFloor()) {
            return AttachFace.FLOOR;
        }
        return definition.placeWall() ? AttachFace.WALL : AttachFace.CEILING;
    }

    private static boolean hasOnlyVerticalParts(FurnitureDefinition definition) {
        return definition.parts().stream().allMatch(p -> p.dx() == 0 && p.dz() == 0);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(props -> new FurnitureBlock(this.definition, props));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnitureBlockEntity(pos, state);
    }

    public FurnitureDefinition getDefinition() {
        return definition;
    }

    public AttachFace getResolvedFace(BlockState state) {
        return resolvedFace(state);
    }

    public Direction getResolvedFacing(BlockState state) {
        return resolvedFacing(state);
    }

    /** Whether this piece uses the finer 8-direction yaw at all (see the constructor comment). */
    public boolean hasDiagonalRotation() {
        return hasRotation8Property;
    }

    /** The 8-direction yaw (cardinals + diagonals) for floor/ceiling pieces that have one; meaningless otherwise. */
    public int getResolvedRotation8(BlockState state) {
        return hasRotation8Property ? state.getValue(ROTATION_8) : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        FurnitureDefinition def = PENDING.get();
        if (def == null) {
            return;
        }
        if (def.placementCount() > 1) {
            builder.add(FACE);
        }
        builder.add(FACING);
        if ((def.placeFloor() || def.placeRoof()) && hasOnlyVerticalParts(def)) {
            builder.add(ROTATION_8);
        }
        if (def.toggleableLight()) {
            builder.add(LIT);
        }
    }

    private AttachFace resolvedFace(BlockState state) {
        return hasFaceProperty ? state.getValue(FACE) : fixedOrDefaultFace();
    }

    private Direction resolvedFacing(BlockState state) {
        return state.getValue(FACING);
    }

    private boolean isFaceAllowed(AttachFace face) {
        return switch (face) {
            case FLOOR -> definition.placeFloor();
            case WALL -> definition.placeWall();
            case CEILING -> definition.placeRoof();
        };
    }

    private static Direction supportDirection(AttachFace face, Direction facing) {
        return switch (face) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> facing.getOpposite();
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return mainShape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return definition.mainShape().solid() ? mainShape : Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction support = supportDirection(resolvedFace(state), resolvedFacing(state));
        BlockPos supportPos = pos.relative(support);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, support.getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction dir : context.getNearestLookingDirections()) {
            AttachFace face;
            Direction facing;
            int rotation8 = 0;
            if (dir.getAxis() == Direction.Axis.Y) {
                face = dir == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR;
                // Every piece faces the player on placement, chest-style - no fixed-direction
                // fallback for any piece, regardless of Nexo's "rotatable" yml flag (which used to
                // gate this and is no longer read at all).
                facing = context.getHorizontalDirection().getOpposite();
                if (context.getPlayer() != null) {
                    // Same "faces the player" angle as `facing`, just snapped to 8 steps instead
                    // of 4 so a player looking e.g. south-east places it facing south-east too.
                    rotation8 = Math.floorMod(Math.round(context.getPlayer().getYRot() / 45.0F), 8);
                }
            } else {
                face = AttachFace.WALL;
                facing = dir.getOpposite();
            }
            if (!isFaceAllowed(face)) {
                continue;
            }

            BlockState candidate = defaultBlockState().setValue(FACING, facing);
            if (hasFaceProperty) {
                candidate = candidate.setValue(FACE, face);
            }
            if (hasRotation8Property) {
                candidate = candidate.setValue(ROTATION_8, rotation8);
            }
            if (candidate.canSurvive(context.getLevel(), context.getClickedPos())) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof FurnitureBlockEntity blockEntity) {
            blockEntity.setDyedColor(stack.get(DataComponents.DYED_COLOR));
        }
        Direction facing = resolvedFacing(state);
        for (FurnitureDefinition.Part part : definition.parts()) {
            BlockPos partPos = pos.offset(rotate(part, facing));
            level.setBlock(partPos, ModBlocks.FURNITURE_PART.get().defaultBlockState()
                    .setValue(FurniturePartBlock.SOLID, part.solid())
                    .setValue(FurniturePartBlock.LEVEL, part.light()), 3);
            if (level.getBlockEntity(partPos) instanceof FurniturePartBlockEntity part_be) {
                part_be.setMaster(pos.immutable());
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && !movedByPiston) {
            Direction facing = resolvedFacing(state);
            for (FurnitureDefinition.Part part : definition.parts()) {
                BlockPos partPos = pos.offset(rotate(part, facing));
                if (level.getBlockState(partPos).is(ModBlocks.FURNITURE_PART.get())) {
                    level.removeBlock(partPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!hasLitProperty) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(LIT), 3);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Rotates a part's offset around Y from the NORTH baseline to match the block's actual facing.
     */
    private static BlockPos rotate(FurnitureDefinition.Part part, Direction facing) {
        int x = part.dx();
        int z = part.dz();
        return switch (facing) {
            case EAST -> new BlockPos(-z, part.dy(), x);
            case SOUTH -> new BlockPos(-x, part.dy(), -z);
            case WEST -> new BlockPos(z, part.dy(), -x);
            default -> new BlockPos(x, part.dy(), z);
        };
    }
}
