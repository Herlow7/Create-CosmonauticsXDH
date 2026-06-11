package dev.devce.rocketnautics.content.orbit.universe;

import dev.devce.rocketnautics.api.orbit.AllowedTransfer;
import dev.devce.rocketnautics.api.orbit.AtmosphereFlags;
import dev.ryanhcode.sable.physics.config.dimension_physics.BezierResourceFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record PlanetDimensionData(@NotNull ResourceKey<Level> key, @NotNull AllowedTransfer allowedTransfer,
                                  int transitionHeight, @NotNull Int2ObjectSortedMap<EnumSet<AtmosphereFlags>> atmosphere,
                                  boolean renderUniverseInDimension, int dimensionDayTimeControllerID,
                                  boolean applyGravityCorrectionToEntities, @NotNull BezierResourceFunction entityDragMultiplier) {

    private static final StreamCodec<FriendlyByteBuf, BezierResourceFunction.BezierPoint> pointCodec = StreamCodec.of(
            (buf, v) -> {
                buf.writeDouble(v.altitude());
                buf.writeDouble(v.value());
                buf.writeDouble(v.slope());
            }, buf -> {
                double alt = buf.readDouble();
                double val = buf.readDouble();
                double slope = buf.readDouble();
                return new BezierResourceFunction.BezierPoint(alt, val, slope);
            }
    );

    // always evaluates to 1
    public static final BezierResourceFunction EMPTY_BEZIER = new BezierResourceFunction(List.of());

    public boolean controlsDimensionDayTime() {
        return dimensionDayTimeControllerID >= 0;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceKey(key);
        buf.writeEnum(allowedTransfer);
        buf.writeVarInt(transitionHeight);
        buf.writeMap(atmosphere, ByteBufCodecs.INT, (b, c) -> b.writeEnumSet(c, AtmosphereFlags.class));
        buf.writeBoolean(renderUniverseInDimension);
        buf.writeVarInt(dimensionDayTimeControllerID);
        buf.writeBoolean(applyGravityCorrectionToEntities);
        buf.writeCollection(entityDragMultiplier.getPoints(), pointCodec);
    }

    public static PlanetDimensionData read(FriendlyByteBuf buf) {
        ResourceKey<Level> key = buf.readResourceKey(Registries.DIMENSION);
        AllowedTransfer allowedTransfer = buf.readEnum(AllowedTransfer.class);
        int transitionHeight = buf.readVarInt();
        Map<Integer, EnumSet<AtmosphereFlags>> map = buf.readMap(ByteBufCodecs.INT, b -> b.readEnumSet(AtmosphereFlags.class));
        boolean renderUniverseInDimension = buf.readBoolean();
        int controlDimensionDayTimeID = buf.readVarInt();
        boolean applyGravityCorrectionToEntities = buf.readBoolean();
        ArrayList<BezierResourceFunction.BezierPoint> entityDragPoints = buf.readCollection(ArrayList::new, pointCodec);
        return new PlanetDimensionData(key, allowedTransfer,
                transitionHeight, new Int2ObjectAVLTreeMap<>(map),
                renderUniverseInDimension, controlDimensionDayTimeID,
                applyGravityCorrectionToEntities, entityDragPoints.isEmpty() ? EMPTY_BEZIER : new BezierResourceFunction(entityDragPoints));
    }

}
