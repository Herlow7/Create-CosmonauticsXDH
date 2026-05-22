package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DampenersTogglePayload() implements CustomPacketPayload {
    public static final Type<DampenersTogglePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "dampeners_toggle"));

    public static final StreamCodec<FriendlyByteBuf, DampenersTogglePayload> CODEC = StreamCodec.unit(new DampenersTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
