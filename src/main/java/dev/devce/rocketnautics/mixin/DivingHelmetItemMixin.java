package dev.devce.rocketnautics.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DivingHelmetItem.class, remap = false)
public class DivingHelmetItemMixin {

    @ModifyExpressionValue(method = "breatheUnderwater", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isClientSide:Z", ordinal = 0, opcode = Opcodes.GETFIELD))
    private static boolean timerDisplayOverride(boolean original, @Local(argsOnly = true, name = "arg0") LivingBreatheEvent event) {
        if (!original) return false;
        LivingEntity entity = event.getEntity();
        return !(entity instanceof Player player && GlobalSpacePhysicsHandler.shouldDisplayTimer(player));
    }
}
