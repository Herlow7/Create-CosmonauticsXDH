package dev.devce.rocketnautics.network;

import com.mojang.datafixers.util.Either;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.api.orbit.ColorPalette;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanetRenderPayload(int id, Either<ColorPalette, ResourceLocation> renderData, int powerSize) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlanetRenderPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "planet_render"));

    public static final StreamCodec<FriendlyByteBuf, PlanetRenderPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.id());
                buf.writeBoolean(payload.renderData().left().isPresent());
                payload.renderData().ifLeft(p -> ColorPalette.CODEC_S.encode(buf, p));
                payload.renderData().ifRight(buf::writeResourceLocation);
                buf.writeInt(payload.powerSize());
            }, (buf) -> {
                int id = buf.readInt();
                boolean isPalette = buf.readBoolean();
                Either<ColorPalette, ResourceLocation> renderData;
                if (isPalette) {
                    renderData = Either.left(ColorPalette.CODEC_S.decode(buf));
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
