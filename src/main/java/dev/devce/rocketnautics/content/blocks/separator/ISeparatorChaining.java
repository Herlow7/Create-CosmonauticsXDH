package dev.devce.rocketnautics.content.blocks.separator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public interface ISeparatorChaining {

    boolean shouldConnectTo(BlockGetter level, BlockPos pos, BlockState state, Direction connectionDirection);

    void connectTo(LevelAccessor level, BlockPos pos, BlockState state, Direction connectionDirection);

    void disconnect(LevelAccessor level, BlockPos pos, BlockState state, Direction connectionDirection);

    boolean isConnected(LevelAccessor level, BlockPos pos, BlockState state, Direction connectionDirection);

    default void triggerChainReaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ISeparatorChaining)) return;

        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 5.0f, 1.5f + level.random.nextFloat());

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 5; i++) {
                double px = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
                double py = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5);
                double pz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0, 0, 0, 0.05);
                serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0, 0, 0.02);
            }
        }

        for (Direction direction : Direction.values()) {
            if (isConnected(level, pos, state, direction)) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ISeparatorChaining sep) {
                    sep.triggerChainReaction(level, neighborPos);
                }
            }
        }
    }
}
