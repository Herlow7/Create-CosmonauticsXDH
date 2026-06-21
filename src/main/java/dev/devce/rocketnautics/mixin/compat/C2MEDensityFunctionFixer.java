package dev.devce.rocketnautics.mixin.compat;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.devce.rocketnautics.content.world.StretchedNoise;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(com.ishland.c2me.opts.dfc.common.ast.McToAst.class)
public class C2MEDensityFunctionFixer {

    @ModifyReturnValue(method = "toAst", at = @At(value = "RETURN"))
    private static AstNode rocketnautics$adaptStretchedNoise(AstNode original, DensityFunction df) {
        if (df instanceof StretchedNoise(DensityFunction.NoiseHolder noise, double xScale, double yScale, double zScale)) {
            return new GenericShiftedNoiseNode(new MulNode(CoordinateNode.AXIS_X, new ConstantNode(xScale)), new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(yScale)), new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(zScale)), noise);
        }
        return original;
    }
}
