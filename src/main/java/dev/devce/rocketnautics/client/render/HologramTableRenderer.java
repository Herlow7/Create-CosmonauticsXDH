package dev.devce.rocketnautics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.api.orbit.DeepSpaceHelper;
import dev.devce.rocketnautics.client.DeepSpaceHandler;
import dev.devce.rocketnautics.content.blocks.HologramTableBlockEntity;
import dev.devce.rocketnautics.content.orbit.universe.CubePlanet;
import dev.devce.rocketnautics.content.orbit.universe.DeepSpacePosition;
import dev.devce.rocketnautics.content.orbit.universe.UniverseDefinition;
import dev.devce.rocketnautics.mixin.GameRendererAccessor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class HologramTableRenderer extends SafeBlockEntityRenderer<HologramTableBlockEntity> {

    public HologramTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(HologramTableBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        UniverseDefinition universe = DeepSpaceHandler.getUniverse();
        if (universe == null || be.getLevel() == null) return;
        final int holoSize = be.getHoloSize();
        final double holoScale = be.getHoloScale();
        DeepSpacePosition position = null;
        AbsoluteDate renderDate;
        long renderTicks = Minecraft.getInstance().levelRenderer.getTicks();
        if (DeepSpaceHandler.hasReceivedPosition() && DeepSpaceHelper.isDeepSpace(be.getLevel())) {
            position = DeepSpaceHandler.getReceivedPosition();
            renderDate = DeepSpaceHandler.getRenderDate(partialTicks);
        } else {
            renderDate = DeepSpaceHandler.getPredictedUniverseDate(partialTicks);
            if (renderDate == null) {
                renderDate = DeepSpaceHelper.EPOCH;
            }
        }
        Frame centerFrame;
        Vector3D posInFrame;
        if (position != null) {
            centerFrame = position.getFrame();
            posInFrame = position.getPosition(renderDate);
        } else {
            CubePlanet inhabiting = universe.getPlanetByDimension(be.getLevel().dimension());
            if (inhabiting == null) return;
            centerFrame = inhabiting.orekitFrame();
            posInFrame = DeepSpaceHelper.localPositionToGlobalPositionAndRotation(Sable.HELPER.projectOutOfSubLevel(be.getLevel(), JOMLConversion.atCenterOf(be.getBlockPos(), new Vector3d())), null, be.getLevel(), inhabiting, renderDate).first().getPosition();
        }
        ms.pushPose();
        ms.translate(0.5, holoSize / 2d + 1, 0.5);
        ms.pushPose();
        // remove our sub level's rotation
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(be);
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (subLevel != null) {
            ms.mulPose(subLevel.renderPose().orientation().get(new Matrix4f()).invert());
        }
        // finish hologram rendering
        Pair<Frame, List<CubePlanet>> pair = DeepSpaceHandler.renderHologram(posInFrame, centerFrame, universe, renderDate, holoScale / holoSize, holoScale / 2, ms, bufferSource);
        Frame largestFrame = pair.left();
        double fov = ((GameRendererAccessor)minecraft.gameRenderer).rocketnautics$getFov(camera, partialTicks, true);
        float s = (float) (0.01 * Math.sqrt(holoSize) * Math.tan(Math.toRadians(fov) / 2));
        DebugRenderer.renderFilledBox(ms, bufferSource, -s, -s, -s, s, s, s, 0.0f, 1.0f, 0.8f, 0.8f);
        double scaleFactor = holoSize / holoScale;
        if (position != null) {
            renderVelocityVector(position.getCurrentOrbit().getPVCoordinates(renderDate, largestFrame).getVelocity(), bufferSource, ms, camera);
        }
        ms.pushPose();
        Vector3D scaledPos = centerFrame.getStaticTransformTo(largestFrame, renderDate).transformPosition(posInFrame).scalarMultiply(scaleFactor);
        ms.translate(-scaledPos.getX(), -scaledPos.getY(), -scaledPos.getZ());
        int count = 0;
        Predicate<Vector3D> distancePred = v -> 4 * v.distanceSq(scaledPos) > holoSize * holoSize;
        // TODO make the speed and size of the cycle configurable
        float[] cycle = new float[] { ((renderTicks + partialTicks) * getCycleSpeed()) % getCycleLength(), ((renderTicks + partialTicks) * getCycleSpeed() * 4) % (getCycleLength() * 4), ((renderTicks + partialTicks) * getCycleSpeed() * 16) % (getCycleLength() * 16) };
        if (position != null) {
            int steps = RocketConfig.CLIENT.orbitPredictionSteps.getAsInt();
//            ms.pushPose();
//            ms.translate(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
//            DebugRenderer.renderFloatingText(ms, bufferSource, String.format("Speed: %.2e", velocity.getNorm()), 0, -sPos * 5, 0, FastColor.ARGB32.color(255, 255, 255, 255));
//            DebugRenderer.renderFloatingText(ms, bufferSource, String.format("Velocity: %.2e, %.2e, %.2e", velocity.getX(), velocity.getY(), velocity.getZ()), 0, -sPos * 10, 0, FastColor.ARGB32.color(255, 255, 255, 255));
//            ms.popPose();
            if (steps > 0) {
                // render our orbit prediction
                Iterator<Vector3D> iter = DeepSpaceHandler.getPositionPrediction(largestFrame, steps);
                count = renderChainedPositions(iter, scaleFactor, bufferSource, ms, distancePred, cycle, s, 0.3f, false);
                renderIntersects(bufferSource, ms, largestFrame, pair.right(), scaleFactor, s);
            }
        }
        // render planetary orbits
        for (CubePlanet planet : pair.right()) {
            // only do orbit predictions for celestial bodies orbiting the dominant object
            if (planet.orekitFrame() == largestFrame || planet.orekitFrame().getParent() != largestFrame) continue;
            // orbit lines
            planet.frame().ifOrbit(o -> renderPlanetOrbit(bufferSource, ms, scaleFactor, o, distancePred));
            if (count > 0) {
                // velocity
                ms.pushPose();
                PVCoordinates c = planet.getPVCoordinates(renderDate, largestFrame);
                ms.translate(c.getPosition().getX() * scaleFactor, c.getPosition().getY() * scaleFactor, c.getPosition().getZ() * scaleFactor);
                renderVelocityVector(c.getVelocity(), bufferSource, ms, camera);
                ms.popPose();
                // red dots
                var iter = DeepSpaceHandler.getPredictionDates(count)
                        .map(d -> planet.getPosition(d, largestFrame))
                        .iterator();
                renderChainedPositions(iter, scaleFactor, bufferSource, ms, distancePred, cycle, s, 1, true);
            }
        }
        ms.popPose();
        ms.popPose();
        ms.popPose();
    }

    private void renderPlanetOrbit(MultiBufferSource bufferSource, PoseStack ms, double scaleFactor, KeplerianOrbit orbit, Predicate<Vector3D> skipCondition) {
        double period = orbit.getKeplerianPeriod();
        if (Double.isInfinite(period)) return;
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lineStrip());
        recurseRenderPlanetOrbit(buffer, ms, scaleFactor, orbit, orbit.getDate(), period, skipCondition);
        PVCoordinates coords = orbit.getPVCoordinates(orbit.getDate(), orbit.getFrame());
        Vector3f norm = DeepSpaceHelper.adaptf(coords.getVelocity()).normalize();
        buffer.addVertex(ms.last(), DeepSpaceHelper.adaptf(coords.getPosition()).mul((float) scaleFactor))
                .setColor(0.8f, 0.8f, 1f, skipCondition.test(coords.getPosition().scalarMultiply(scaleFactor)) ? 0f : 0.8f)
                .setNormal(ms.last(), norm.x(), norm.y(), norm.z());
    }

    private void recurseRenderPlanetOrbit(VertexConsumer buffer, PoseStack ms, double scaleFactor, KeplerianOrbit orbit, AbsoluteDate point, double len, Predicate<Vector3D> skipCondition) {
        double threshold = Math.PI / 32;
        PVCoordinates coords = orbit.getPVCoordinates(point, orbit.getFrame());
        // alternative approach -- acceleration norm divided by velocity norm
        double angularVelocity = coords.getAngularVelocity().getNorm();
        if (angularVelocity * len > threshold) {
            recurseRenderPlanetOrbit(buffer, ms, scaleFactor, orbit, point, len / 2, skipCondition);
            recurseRenderPlanetOrbit(buffer, ms, scaleFactor, orbit, point.shiftedBy(len / 2), len / 2, skipCondition);
        } else {
            Vector3f norm = DeepSpaceHelper.adaptf(coords.getVelocity()).normalize();
            buffer.addVertex(ms.last(), DeepSpaceHelper.adaptf(coords.getPosition()).mul((float) scaleFactor))
                    .setColor(0.8f, 0.8f, 1f, skipCondition.test(coords.getPosition().scalarMultiply(scaleFactor)) ? 0f : 0.8f)
                    .setNormal(ms.last(), norm.x(), norm.y(), norm.z());
        }
    }

    private void renderVelocityVector(Vector3D velocity, MultiBufferSource bufferSource, PoseStack ms, Camera camera) {
        VertexConsumer bufVel = bufferSource.getBuffer(RenderType.lineStrip());
        Vector3D normed = velocity.normalize();
        Vector3D cross = normed.crossProduct(DeepSpaceHelper.adaptf(camera.getLookVector())).normalize();
        Vector3D pointer = new Vector3D(0.8, normed, -0.1, cross);
        Vector3D normal = pointer.subtract(normed).normalize();
        bufVel.addVertex(ms.last(), 0f, 0f, 0f)
                .setColor(1.0f, 0f, 1.0f, 0.8f)
                .setNormal(ms.last(), (float) normed.getX(), (float) normed.getY(), (float) normed.getZ());
        bufVel.addVertex(ms.last(), (float) normed.getX(), (float) normed.getY(), (float) normed.getZ())
                .setColor(1.0f, 0f, 1.0f, 0.8f)
                .setNormal(ms.last(), (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        bufVel.addVertex(ms.last(), (float) pointer.getX(), (float) pointer.getY(), (float) pointer.getZ())
                .setColor(1.0f, 0f, 1.0f, 0.8f)
                .setNormal(ms.last(), (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        pointer = new Vector3D(0.8, normed, 0.1, cross);
        normal = pointer.subtract(normed).normalize();
        bufVel.addVertex(ms.last(), (float) pointer.getX(), (float) pointer.getY(), (float) pointer.getZ())
                .setColor(1.0f, 0f, 1.0f, 0.8f)
                .setNormal(ms.last(), (float) cross.getX(), (float) cross.getY(), (float) cross.getZ());
        bufVel.addVertex(ms.last(), (float) normed.getX(), (float) normed.getY(), (float) normed.getZ())
                .setColor(1.0f, 0f, 1.0f, 0.8f)
                .setNormal(ms.last(), (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
    }

    private int renderChainedPositions(Iterator<Vector3D> iter, double scaleFactor, MultiBufferSource bufferSource, PoseStack ms, Predicate<Vector3D> stopCondition, float[] cycle, double s, float b, boolean onlyCycle) {
        if (onlyCycle && !doCycle()) return 0;
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lineStrip());
        List<List<Vector3D>> cycling = new ArrayList<>(cycle.length);
        for (int i = 0; i < cycle.length; i++) {
            cycling.add(new ObjectArrayList<>());
        }
        int count = 0;
        if (iter.hasNext()) {
            Vector3D v = iter.next().scalarMultiply(scaleFactor);
            Vector3D norm = null;
            while (iter.hasNext()) {
                Vector3D vNext = iter.next().scalarMultiply(scaleFactor);
                if (stopCondition.test(v)) {
                    break;
                }
                if (doCycle()) {
                    for (int i = 0; i < cycle.length; i++) {
                        if (count == Math.ceil(cycle[i])) {
                            float partialCycle = cycle[i] % 1;
                            cycling.get(i).add(new Vector3D(partialCycle, vNext, 1 - partialCycle, v));
                        }
                    }
                }
                count++;
                if (!onlyCycle) {
                Vector3D dif = vNext.subtract(v);
                if (dif.getNormSq() > 1e-20) {
                    norm = dif.normalize();
                } else if (norm == null) {
                    v = vNext;
                    continue;
                }
                buffer.addVertex(ms.last(), (float) v.getX(), (float) v.getY(), (float) v.getZ())
                        .setColor(0.8f, 0.8f, b, 0.8f)
                        .setNormal(ms.last(), (float) norm.getX(), (float) norm.getY(), (float) norm.getZ());
                }
                v = vNext;
            }
        }
        if (doCycle()) {
            for (int i = 0; i < cycling.size(); i++) {
                List<Vector3D> points = cycling.get(i);
                float factor = 1 - 0.3f * i / cycle.length;
                for (Vector3D c : points) {
                    ms.pushPose();
                    ms.translate(c.getX(), c.getY(), c.getZ());
                    DebugRenderer.renderFilledBox(ms, bufferSource, -s, -s, -s, s, s, s, factor, factor, b, 1.0f);
                    ms.popPose();
                }
            }
        }
        return count;
    }

    private void renderIntersects(MultiBufferSource bufferSource, PoseStack ms, Frame referenceFrame, List<CubePlanet> planets, double scaleFactor, float s) {
        Iterator<Orbit> iter = DeepSpaceHandler.getPredictionOrbits();
        Orbit prevOrbit = iter.next();
        while (iter.hasNext()) {
            Orbit orbit = iter.next();
            ms.pushPose();
            Vector3D v = orbit.getPosition(referenceFrame).scalarMultiply(scaleFactor);
            ms.translate(v.getX(), v.getY(), v.getZ());
            DebugRenderer.renderFilledBox(ms, bufferSource, -s, -s, -s, s, s, s, 0f, 0.0f, 1.0f, 1.0f);
            ms.popPose();
            List<Vector3D> vPs = new ArrayList<>();
            for (CubePlanet planet : planets) {
                if (planet.orekitFrame() != prevOrbit.getFrame() && planet.orekitFrame() != orbit.getFrame()) continue;
                ms.pushPose();
                Vector3D vP = planet.getPosition(orbit.getDate(), referenceFrame).scalarMultiply(scaleFactor);
                ms.translate(vP.getX(), vP.getY(), vP.getZ());
                DebugRenderer.renderFilledBox(ms, bufferSource, -s, -s, -s, s, s, s, 0f, 0.0f, 1.0f, 1.0f);
                ms.popPose();
                vPs.add(vP);
            }
            if (!vPs.isEmpty()) {
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
                for (Vector3D vP : vPs) {
                    Vector3D norm = v.subtract(vP).normalize();
                    buffer.addVertex(ms.last(), (float) vP.getX(), (float) vP.getY(), (float) vP.getZ())
                            .setColor(0f, 0.0f, 1.0f, 1.0f)
                            .setNormal(ms.last(), (float) norm.getX(), (float) norm.getY(), (float) norm.getZ());
                    buffer.addVertex(ms.last(), (float) v.getX(), (float) v.getY(), (float) v.getZ())
                            .setColor(0f, 0.0f, 1.0f, 1.0f)
                            .setNormal(ms.last(), (float) norm.getX(), (float) norm.getY(), (float) norm.getZ());
                }
            }
            prevOrbit = orbit;
        }
    }

    private boolean doCycle() {
        return true;
    }

    private float getCycleSpeed() {
        return 0.25f;
    }

    private int getCycleLength() {
        return 20;
    }

    @Override
    public boolean shouldRenderOffScreen(HologramTableBlockEntity p_112306_) {
        return true;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NonNull HologramTableBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        int size = blockEntity.getHoloSize();
        int halfSize = size / 2;
        return new AABB(pos.getX() - halfSize, pos.getY(), pos.getZ() - halfSize, pos.getX() + halfSize + 1, pos.getY() + size + 1, pos.getZ() + halfSize + 1);
    }
}
