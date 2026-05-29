package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UniverseTimeSyncPayload(long universeTicks, float serverTickRate) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UniverseTimeSyncPayload> TYPE = new CustomPacketPayload.Type<>(RocketNautics.path("universe_time"));

    public static final StreamCodec<FriendlyByteBuf, UniverseTimeSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarLong(payload.universeTicks());
                buf.writeFloat(payload.serverTickRate());
            },
            buf -> {
                long ticks = buf.readVarLong();
                float rate = buf.readFloat();
                return new UniverseTimeSyncPayload(ticks, rate);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
