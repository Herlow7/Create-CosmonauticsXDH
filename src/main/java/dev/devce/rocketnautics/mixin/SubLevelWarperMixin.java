package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.devce.rocketnautics.content.physics.SpaceTransitionHandler;
import dev.egg.DimensionalSable;
import dev.egg.SubLevelWarper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

@Mixin(value = SubLevelWarper.class, remap = false)
public class SubLevelWarperMixin {

    @Inject(method = "WarpSubLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"))
    private static void inflateBox(Collection<SubLevel> compoundSubLevel, ServerSubLevelContainer sourceContainer, ServerSubLevelContainer destinationContainer, Vector3d center, Vector3d position, CallbackInfo ci, @Local(name = "box") LocalRef<AABB> aabb) {
        aabb.set(aabb.get().inflate(SpaceTransitionHandler.WARP_ENTITY_DETECTION_TOLERANCE));
    }

    @WrapOperation(method = "WarpSubLevels", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/physics/PhysicsPipeline;teleport(Ldev/ryanhcode/sable/api/physics/PhysicsPipelineBody;Lorg/joml/Vector3dc;Lorg/joml/Quaterniondc;)V"))
    private static void applyRotation(PhysicsPipeline instance, PhysicsPipelineBody physicsPipelineBody, Vector3dc vector3dc, Quaterniondc quaterniondc, Operation<Void> original) {
        if (SpaceTransitionHandler.PREMUL_ROTATION != null) {
            quaterniondc = quaterniondc.premul(SpaceTransitionHandler.PREMUL_ROTATION, new Quaterniond());
        }
        original.call(instance, physicsPipelineBody, vector3dc, quaterniondc);
    }
}
