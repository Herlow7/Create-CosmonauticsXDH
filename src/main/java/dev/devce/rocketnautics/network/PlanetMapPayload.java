package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.api.orbit.ColorPalette;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanetMapPayload(int powerSize, int centerX, int centerZ, ColorPalette mapDataPosXPosZ, ColorPalette mapDataPosXNegZ, ColorPalette mapDataNegXPosZ, ColorPalette mapDataNegXNegZ) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlanetMapPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "planet_map_data"));
    
    public static final StreamCodec<FriendlyByteBuf, PlanetMapPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeInt(payload.powerSize());
            buf.writeInt(payload.centerX());
            buf.writeInt(payload.centerZ());
            ColorPalette.CODEC_S.encode(buf, payload.mapDataPosXPosZ());
            ColorPalette.CODEC_S.encode(buf, payload.mapDataPosXNegZ());
            ColorPalette.CODEC_S.encode(buf, payload.mapDataNegXPosZ());
            ColorPalette.CODEC_S.encode(buf, payload.mapDataNegXNegZ());
        },
        buf -> {
            int powerSize = buf.readInt();
            int negXCorner = buf.readInt();
            int negZCorner = buf.readInt();
            ColorPalette posXPosZ = ColorPalette.CODEC_S.decode(buf);
            ColorPalette posXNegZ = ColorPalette.CODEC_S.decode(buf);
            ColorPalette negXPosZ = ColorPalette.CODEC_S.decode(buf);
            ColorPalette negXNegZ = ColorPalette.CODEC_S.decode(buf);
            return new PlanetMapPayload(powerSize, negXCorner, negZCorner, posXPosZ, posXNegZ, negXPosZ, negXNegZ);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
