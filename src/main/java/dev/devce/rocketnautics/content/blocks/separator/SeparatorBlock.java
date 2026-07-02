package dev.devce.rocketnautics.content.blocks.separator;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class SeparatorBlock extends DirectionalBlock implements IWrenchable, ISeparatorChaining {
    public static final EnumMap<Direction, BooleanProperty> LINKS = new EnumMap<>(Map.of(
            Direction.UP, BlockStateProperties.UP,
            Direction.DOWN, BlockStateProperties.DOWN,
            Direction.EAST, BlockStateProperties.EAST,
            Direction.WEST, BlockStateProperties.WEST,
            Direction.NORTH, BlockStateProperties.NORTH,
            Direction.SOUTH, BlockStateProperties.SOUTH
    ));

    public static final VoxelShaper SHAPE = VoxelShaper.forDirectional(Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0),
            Block.box(4.0, 4.0, 4.0, 12.0, 13.0, 12.0),
            Block.box(3.0, 13.0, 3.0, 13.0, 16.0, 13.0)

    ), Direction.UP);

    public static final VoxelShaper LINK_SHAPE = VoxelShaper.forDirectional(Block.box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0), Direction.UP);

    public static final MapCodec<SeparatorBlock> CODEC = simpleCodec(SeparatorBlock::new);

    public SeparatorBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.UP);
        for (BooleanProperty p : LINKS.values()) {
            state = state.setValue(p, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected @NotNull MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        LINKS.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        
        if (clickedState.getBlock() == this && clickedState.getValue(FACING).getAxis().test(facing)) {
            facing = clickedState.getValue(FACING).getOpposite();
        }

        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            facing = context.getNearestLookingDirection().getOpposite();

        BlockState result = this.defaultBlockState().setValue(FACING, facing);

        for (Direction dir : Direction.values()) {
            if (!shouldConnectTo(context.getLevel(), context.getClickedPos(), result, dir)) continue;
            BlockPos n = context.getClickedPos().relative(dir);
            BlockState neighbor = context.getLevel().getBlockState(n);
            if (neighbor.getBlock() instanceof ISeparatorChaining chaining) {
                if (chaining.shouldConnectTo(context.getLevel(), n, neighbor, dir.getOpposite())) {
                    result = result.setValue(LINKS.get(dir), true);
                }
            }
        }
        
        return result;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean p_60570_) {
        for (Direction dir : Direction.values()) {
            if (state.getValue(LINKS.get(dir))) {
                BlockPos n = pos.relative(dir);
                BlockState neighbor = level.getBlockState(n);
                if (neighbor.getBlock() instanceof ISeparatorChaining chaining) {
                    chaining.connectTo(level, n, neighbor, dir.getOpposite());
                }
            }
        }
    }

    @Override
    public boolean shouldConnectTo(BlockGetter level, BlockPos pos, BlockState state, Direction connectionDirection) {
        return true;
    }

    @Override
    public void connectTo(LevelAccessor level, BlockPos pos, BlockState state, Direction connectionDirection) {
        BooleanProperty prop = LINKS.get(connectionDirection);
        if (!state.getValue(prop)) {
            level.setBlock(pos, state.setValue(prop, true), 2);
        }
    }

    @Override
    public void disconnect(LevelAccessor level, BlockPos pos, BlockState state, Direction connectionDirection) {
        BooleanProperty prop = LINKS.get(connectionDirection);
        if (state.getValue(prop)) {
            level.setBlock(pos, state.setValue(prop, false), 2);
        }
    }

    @Override
    public boolean isConnected(LevelAccessor level, BlockPos pos, BlockState state, Direction connectionDirection) {
        return state.getValue(LINKS.get(connectionDirection));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            if (level.hasNeighborSignal(pos)) {
                triggerChainReaction(level, pos);
            }
            Direction incomingDir = Direction.getNearest(pos.getX() - fromPos.getX(), pos.getY() - fromPos.getY(), pos.getZ() - fromPos.getZ());
            BooleanProperty prop = LINKS.get(incomingDir.getOpposite());
            if (state.getValue(prop)) {
                if (!(level.getBlockState(fromPos).getBlock() instanceof ISeparatorChaining)) {
                    level.setBlock(pos, state.setValue(prop, false), 2);
                }
            }
        }
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isHoldingItem(AllItems.WRENCH.asItem())) {
            VoxelShape shape = SHAPE.get(state.getValue(FACING));
            for (Direction dir : Direction.values()) {
                BlockPos nPos = pos.relative(dir);
                BlockState nState = level.getBlockState(nPos);
                if (nState.getBlock() instanceof ISeparatorChaining chaining
                        && chaining.shouldConnectTo(level, nPos, nState, dir.getOpposite()))
                    shape = Shapes.or(shape, LINK_SHAPE.get(dir));
            }
            return shape;
        } else {
            return this.getCollisionShape(state, level, pos, context);
        }
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPE.get(state.getValue(FACING));
        for (Direction dir : Direction.values()) {
            if (state.getValue(LINKS.get(dir))) {
                shape = Shapes.or(shape, LINK_SHAPE.get(dir));
            }
        }
        return shape;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        for (Direction dir : Direction.values()) {
            Vec3 location = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));
            if (LINK_SHAPE.get(dir).closestPointTo(location).map(v -> v.distanceToSqr(location) < 1e-4).orElse(false)) {
                Level level = context.getLevel();
                BlockPos pos = context.getClickedPos();
                boolean linked = state.getValue(LINKS.get(dir));
                BlockPos relative = pos.relative(dir);
                BlockState adjacent = level.getBlockState(relative);
                if (adjacent.getBlock() instanceof ISeparatorChaining chaining) {
                    if (linked || chaining.shouldConnectTo(level, relative, adjacent, dir.getOpposite())) {
                        BlockState relinked = state.setValue(LINKS.get(dir), !linked);

                        KineticBlockEntity.switchToBlockState(level, pos, updateAfterWrenched(relinked, context));

                        if (level.getBlockState(pos) != state)
                            IWrenchable.playRotateSound(level, pos);

                        chaining.disconnect(level, relative, adjacent, dir.getOpposite());

                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return IWrenchable.super.onWrenched(state, context);
    }
}
