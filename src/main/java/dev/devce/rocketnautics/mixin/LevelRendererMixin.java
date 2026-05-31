package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.devce.rocketnautics.SkyDataHandler;
import dev.devce.rocketnautics.client.StarBufferExposer;
import dev.devce.rocketnautics.content.orbit.DeepSpaceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements StarBufferExposer {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void renderSnowAndRain(net.minecraft.client.renderer.LightTexture pLightTexture, float pPartialTick, double pCamX, double pCamY, double pCamZ);

    @Shadow
    @Nullable
    private VertexBuffer starBuffer;

    @Override
    public VertexBuffer rocketnautics$starBuffer() {
        return starBuffer;
    }

    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"))
    private float rocketnautics$boostStarBrightness(ClientLevel instance, float partial, Operation<Float> original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return original.call(instance, partial);

        double y = mc.player.getY() + SkyDataHandler.getHeightOffsetForLevel(mc.level.dimension());
        if (y > 1000.0) {
            // Disable vanilla stars above 1000m to let our beautiful custom HD stars render
            return 0.0f;
        }
        return original.call(instance, partial);
    }

    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 rocketnautics$forceBlackSky(ClientLevel instance, Vec3 pos, float partialTick, Operation<Vec3> original) {
        Vec3 color = original.call(instance, pos, partialTick);

        double y = pos.y + SkyDataHandler.getHeightOffsetForLevel(Minecraft.getInstance().level.dimension());
        if (y > 1000.0) {
            float factor = (float) Mth.clamp((y - 1000.0) / 2000.0, 0.0, 1.0);
            
            return new Vec3(
                Mth.lerp(factor, color.x, 0.0),
                Mth.lerp(factor, color.y, 0.0),
                Mth.lerp(factor, color.z, 0.0)
            );
        }
        return color;
    }

    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F"))
    private float[] rocketnautics$disableSunriseAtAltitude(DimensionSpecialEffects instance, float angle, float partialTick, Operation<float[]> original) {
        float[] color = original.call(instance, angle, partialTick);
        if (color == null) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getY() > 1000.0) {
            float factor = (float) Mth.clamp((mc.player.getY() - 1000.0) / 1000.0, 0.0, 1.0);
            if (factor >= 1.0f) return null;
            
            float[] faded = color.clone();
            faded[3] *= (1.0f - factor);
            return faded;
        }
        return color;
    }

    
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V"))
    private void rocketnautics$fadeOutClouds(LevelRenderer instance, com.mojang.blaze3d.vertex.PoseStack pPoseStack, org.joml.Matrix4f pProjectionMatrix, org.joml.Matrix4f pCloudProjectionMatrix, float pPartialTick, double pCamX, double pCamY, double pCamZ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double y = mc.player.getY();
            
            if (y > 2500.0) return;
        }
        instance.renderClouds(pPoseStack, pProjectionMatrix, pCloudProjectionMatrix, pPartialTick, pCamX, pCamY, pCamZ);
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V"))
    private void rocketnautics$disableWeatherAtAltitude(LevelRenderer instance, net.minecraft.client.renderer.LightTexture pLightTexture, float pPartialTick, double pCamX, double pCamY, double pCamZ) {
        if (pCamY > 400.0) return;
        this.renderSnowAndRain(pLightTexture, pPartialTick, pCamX, pCamY, pCamZ);
    }
}
