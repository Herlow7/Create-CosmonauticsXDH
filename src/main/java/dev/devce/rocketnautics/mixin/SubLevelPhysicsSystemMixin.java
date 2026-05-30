package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.orbit.DeepSpaceData;
import dev.devce.rocketnautics.content.orbit.DeepSpaceInstance;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SubLevelPhysicsSystem.class, remap = false)
public abstract class SubLevelPhysicsSystemMixin {

    @Shadow
    public abstract RigidBodyHandle getPhysicsHandle(@NotNull ServerSubLevel subLevel);

    // we need this correction to occur immediately before the Rapier engine does a physics tick, so we mixin.
    @Inject(method = "tickPipelinePhysics", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/ServerSubLevel;applyQueuedForces(Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;D)V", shift = At.Shift.AFTER))
    private void applyVelocityToOrbit(ServerSubLevelContainer container, CallbackInfo ci, @Local(name = "subLevel") ServerSubLevel subLevel) {
        if (!DeepSpaceData.isDeepSpace(container.getLevel())) return;
        Vector3dc position = subLevel.logicalPose().position();
        if (position == null) {
            return;
        }
        DeepSpaceInstance handling = DeepSpaceData.getInstance(container.getLevel().getServer()).getInstanceForPos((int) position.x(), (int) position.z());
        RigidBodyHandle handle = this.getPhysicsHandle(subLevel);
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        if (handling == null) {
            // no movement allowed if you're not in an instance.
            handle.addLinearAndAngularVelocity(velocity.negate(), new Vector3d());
            return;
        }
        // basic idea:
        // take the ship CoM's distance from the box center and project it outward by the ship BB's diagonal length.
        // if this projection lands outside the box radius, cancel the component of velocity moving outward, and halve the orthogonal component of velocity.
        Vector3dc center = handling.getCenter();
        Vector3d offset = position.sub(center, new Vector3d());
        if (offset.lengthSquared() <= 1e-20 || (offset.length() + subLevel.boundingBox().size().length()) * 2 <= handling.getSideLength()) {
            // just register our mass, skipping any calculation
            handling.applyVelocity(subLevel.getUniqueId(), offset.zero(), subLevel.getMassTracker().getMass());
            return;
        }
        offset.normalize();
        double dot = offset.dot(velocity);
        Vector3d aligned = offset.mul(dot, new Vector3d());
        Vector3d orthogonal = velocity.sub(aligned, new Vector3d());
        if (dot < 0) {
            aligned.zero(); // don't cancel velocity moving towards the center
        }
        Vector3d cancelledComponent = orthogonal.mulAdd(0.5, aligned);
        handling.applyVelocity(subLevel.getUniqueId(), cancelledComponent, subLevel.getMassTracker().getMass());
        handle.addLinearAndAngularVelocity(cancelledComponent.negate(), offset.zero());
    }
}
