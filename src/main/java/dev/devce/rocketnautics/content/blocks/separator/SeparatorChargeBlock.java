package dev.devce.rocketnautics.content.blocks.separator;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.EnumMap;

import static dev.devce.rocketnautics.content.blocks.separator.SeparatorBlock.LINK_SHAPE;

public class SeparatorChargeBlock extends AbstractDirectionalAxisBlock implements IWrenchable, ISeparatorChaining {
    public static final EnumMap<Direction, BooleanProperty> LINKS = SeparatorBlock.LINKS;

    public static final VoxelShaper SHAPE = VoxelShaper.forDirectional(Block.box(3.0, 4.0, 3.0, 13.0, 12.0, 13.0), Direction.UP);

    public static final MapCodec<SeparatorChargeBlock> CODEC = simpleCodec(SeparatorChargeBlock::new);

    public SeparatorChargeBlock(Properties p_52591_) {
        super(p_52591_);
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(AXIS_ALONG_FIRST_COORDINATE, true);
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
        super.createBlockStateDefinition(builder);
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
        return state.getValue(FACING).getOpposite() != connectionDirection;
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

    public void triggerChainReaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ISeparatorChaining)) return;

        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 5.0f, 0.8f + level.random.nextFloat() * 0.5f);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 5; i++) {
                double px = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
                double py = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5);
                double pz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0, 0, 0, 0.05);
                serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0, 0, 0.02);
            }
            for (int i = 0; i < 5; i++) {
                Vector3f step = state.getValue(FACING).getOpposite().step().mul(level.random.nextFloat() + 0.5f);
                double px = step.x() + pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) / 2;
                double py = step.y() + pos.getY() + 0.5 + (level.random.nextDouble() - 0.5) / 2;
                double pz = step.z() + pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) / 2;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0, 0, 0, 0.05);
            }
        }
        BlockPos destroy = pos.relative(state.getValue(FACING).getOpposite());
        BlockState destroyState = level.getBlockState(destroy);
        if (BlockBreakingKineticBlockEntity.isBreakable(destroyState, destroyState.getDestroySpeed(level, destroy))) {
            BlockHelper.destroyBlock(level, destroy, 0f);
        }

        for (Direction direction : Direction.values()) {
            if (state.getValue(LINKS.get(direction))) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ISeparatorChaining) {
                    triggerChainReaction(level, neighborPos);
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
        return super.onWrenched(state, context);
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        Direction dir = newState.getValue(FACING).getOpposite();
        if (newState.getValue(LINKS.get(dir))) {
            newState = newState.cycle(LINKS.get(newState.getValue(FACING).getOpposite()));
            BlockPos neighbor = context.getClickedPos().relative(dir);
            BlockState neighborState = context.getLevel().getBlockState(neighbor);
            if (neighborState.getBlock() instanceof ISeparatorChaining chain) {
                chain.disconnect(context.getLevel(), neighbor, neighborState, dir.getOpposite());
            }
        }
        return super.updateAfterWrenched(newState, context);
    }
}
