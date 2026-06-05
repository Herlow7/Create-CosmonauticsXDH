package dev.devce.rocketnautics.content.orbit.universe;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record PlanetDimensionData(@NotNull ResourceKey<Level> key, AllowedTransfer allowedTransfer, int transitionHeight,
                                  boolean renderUniverseInDimension, int dimensionDayTimeControllerID,
                                  boolean applyGravityCorrectionToEntities) {

    public boolean controlsDimensionDayTime() {
        return dimensionDayTimeControllerID >= 0;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceKey(key);
        buf.writeEnum(allowedTransfer);
        buf.writeVarInt(transitionHeight);
        buf.writeBoolean(renderUniverseInDimension);
        buf.writeVarInt(dimensionDayTimeControllerID);
        buf.writeBoolean(applyGravityCorrectionToEntities);
    }

    public static PlanetDimensionData read(FriendlyByteBuf buf) {
        ResourceKey<Level> key = buf.readResourceKey(Registries.DIMENSION);
        AllowedTransfer allowedTransfer = buf.readEnum(AllowedTransfer.class);
        int transitionHeight = buf.readVarInt();
        boolean renderUniverseInDimension = buf.readBoolean();
        int controlDimensionDayTimeID = buf.readVarInt();
        boolean applyGravityCorrectionToEntities = buf.readBoolean();
        return new PlanetDimensionData(key, allowedTransfer, transitionHeight, renderUniverseInDimension, controlDimensionDayTimeID, applyGravityCorrectionToEntities);
    }

    public enum AllowedTransfer implements StringRepresentable {
        ALL, NONE, TO_SPACE, TO_DIMENSION;
        public static final Codec<AllowedTransfer> CODEC = StringRepresentable.fromEnum(AllowedTransfer::values);

        public boolean allowToSpace() {
            return this == ALL || this == TO_SPACE;
        }

        public boolean allowToDimension() {
            return this == ALL || this == TO_DIMENSION;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
