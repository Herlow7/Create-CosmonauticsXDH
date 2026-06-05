package dev.devce.rocketnautics.network;

import com.mojang.datafixers.util.Either;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.client.PlanetColors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanetRenderPayload(int id, Either<byte[], ResourceLocation> renderData, int powerSize) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlanetRenderPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "planet_render"));

    public static final StreamCodec<FriendlyByteBuf, PlanetRenderPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.id());
                buf.writeBoolean(payload.renderData().left().isPresent());
                payload.renderData().ifLeft(buf::writeBytes);
                payload.renderData().ifRight(buf::writeResourceLocation);
                buf.writeInt(payload.powerSize());
            }, (buf) -> {
                int id = buf.readInt();
                boolean isArray = buf.readBoolean();
                Either<byte[], ResourceLocation> renderData;
                if (isArray) {
                    byte[] data = new byte[PlanetColors.ARRAY_SIZE];
                    buf.readBytes(data);
                    renderData = Either.left(data);
                } else {
                    renderData = Either.right(buf.readResourceLocation());
                }
                int powerSize = buf.readInt();
                return new PlanetRenderPayload(id, renderData, powerSize);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
