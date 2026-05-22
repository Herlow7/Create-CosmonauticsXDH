package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.equipment.armor.RemainingAirOverlay;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RemainingAirOverlay.class, remap = false)
public class RemainingAirOverlayMixin {

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInLava()Z"))
    private static boolean timerDisplayOverride(boolean original) {
        if (original) return true;
        assert Minecraft.getInstance().player != null;
        return GlobalSpacePhysicsHandler.shouldDisplayTimer(Minecraft.getInstance().player);
    }
}
