package dev.devce.rocketnautics.content.items;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllEnchantments;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.api.capability.IBacktank;
import dev.devce.rocketnautics.api.capability.JetpackFluidHandlerItemStack;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import dev.devce.rocketnautics.mixin.LivingEntityAccessor;
import dev.devce.rocketnautics.registry.RocketDataComponents;
import dev.devce.rocketnautics.registry.RocketItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

@EventBusSubscriber
public class JetpackItem extends ArmorItem implements IBacktank {
    public JetpackItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, ArmorItem.Type.CHESTPLATE, properties);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        AnchorBootsItem.accelerateDescentNearBlock(event);

        if (isActive(player)) {
            boolean thrusting = applyJetpackPhysics(player);
            if (thrusting && player.level().isClientSide && player.level().getGameTime() % 2 == 0) {
                spawnJetpackParticles(player);
            }
        }
    }

    /**
     * Calculates and applies jetpack flight physics to the player.
     * Uses the player's look vector for thrust direction and applies drag to simulate flight.
     */
    private static boolean applyJetpackPhysics(Player player) {
        Level level = player.level();
        if (level.isClientSide && !GlobalSpacePhysicsHandler.shouldDisplayTimer(player))
            player.getPersistentData().remove("VisualBacktankAir");

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof JetpackItem)) {
            return false;
        }
        var cap = chest.getCapability(Capabilities.FluidHandler.ITEM);
        if (cap == null) return false;

        Vec3 motion = player.getDeltaMovement();
        // Check if player is holding the jump key using a Mixin accessor
        boolean thrusting = ((LivingEntityAccessor) player).rocketnautics$isJumping();
        boolean sprinting = player.isSprinting();
        int thrustConsumption = sprinting ? RocketConfig.SERVER.jetpackSprintThrustConsumption.get() : RocketConfig.SERVER.jetpackThrustConsumption.get();
        int drain = cap.drain(thrustConsumption, IFluidHandler.FluidAction.SIMULATE).getAmount();
        if (drain < thrustConsumption) {
            return false;
        }
        if (!player.hasInfiniteMaterials()) {
            cap.drain(thrustConsumption, IFluidHandler.FluidAction.EXECUTE);
        }

        if (level.isClientSide) {
            List<ItemStack> backtanks = BacktankUtil.getAllWithAir(player);
            if (!backtanks.isEmpty()) {
                float visualBacktankAir = 0f;
                for (ItemStack stack : backtanks)
                    visualBacktankAir += BacktankUtil.getAir(stack);
                player.getPersistentData()
                        .putInt("VisualBacktankAir", Math.round(visualBacktankAir));
            }
        }

        Vec3 thrust = Vec3.ZERO;
        if (thrusting) {
            Vec3 look = player.getLookAngle();

            // Scaled thrust power from config
            double thrustPower = sprinting ? RocketConfig.SERVER.jetpackSprintThrust.get() : RocketConfig.SERVER.jetpackThrust.get();
            thrust = look.scale(thrustPower);

            // Add a small constant upward lift to assist horizontal flight
            if (look.y > -0.5) {
                thrust = thrust.add(0, 0.08 * (1 - GlobalSpacePhysicsHandler.calculateGravityFactor(player.level(), player.getY())), 0);
            }
        }
        // Final motion calculation: Motion = (OldMotion * Drag) + Thrust
        Vec3 newMotion = motion.add(thrust);

        // Speed capping for stability
        double maxSpeed = 10 * ((sprinting ? RocketConfig.SERVER.jetpackSprintThrust.get() : 0) + RocketConfig.SERVER.jetpackThrust.get());
        if (newMotion.length() > maxSpeed) {
            newMotion = newMotion.normalize().scale(maxSpeed);
        }

        player.setDeltaMovement(newMotion);
        player.fallDistance = 0; // Prevent fall damage while using jetpack
        return true;
    }

    /**
     * Spawns exhaust cloud particles behind the player's shoulders.
     */
    private static void spawnJetpackParticles(Player player) {
        // Calculate offset positions relative to player body rotation
        float yaw = player.yBodyRot;
        float rad = yaw * (float) (Math.PI / 180.0);
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);

        // Position offsets for the two nozzles
        double backDist = 0.35;
        double sideDist = 0.22;
        double height = 0.7;

        // Transform local nozzle coordinates to world coordinates
        double lx = player.getX() + (cos * sideDist + sin * backDist);
        double lz = player.getZ() + (sin * sideDist - cos * backDist);

        double rx = player.getX() + (-cos * sideDist + sin * backDist);
        double rz = player.getZ() + (-sin * sideDist - cos * backDist);

        // Spawn particles
        player.level().addParticle(ParticleTypes.CLOUD, lx, player.getY() + height, lz, 0, -0.15, 0);
        player.level().addParticle(ParticleTypes.CLOUD, rx, player.getY() + height, rz, 0, -0.15, 0);
    }

    @SubscribeEvent
    public static void onCapabilityRegister(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new JetpackFluidHandlerItemStack(() -> RocketDataComponents.JETPACK_FUEL, stack, 8000, 4000), RocketItems.JETPACK);
    }

    public static void toggle(ServerPlayer player) {
        ItemStack worn = getWornItem(player);
        if (worn.getItem() instanceof JetpackItem j) {
            boolean wasActive = j.setActive(worn, !j.isActive(worn));
            if (wasActive) {
                player.displayClientMessage(Component.translatable("rocketnautics.jetpack.disabled").withStyle(ChatFormatting.RED), true);
            } else {
                player.displayClientMessage(Component.translatable("rocketnautics.jetpack.enabled").withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    public static boolean isActive(Player player) {
        ItemStack worn = getWornItem(player);
        if (worn.getItem() instanceof JetpackItem j) {
            return j.isActive(worn);
        }
        return false;
    }

    public boolean isActive(ItemStack stack) {
        return stack.getOrDefault(RocketDataComponents.SYSTEMS_ACTIVE, false);
    }

    public boolean setActive(ItemStack stack, boolean active) {
        return Boolean.TRUE.equals(stack.set(RocketDataComponents.SYSTEMS_ACTIVE, active));
    }

    public static boolean isWornBy(Entity entity) {
        return !getWornItem(entity).isEmpty();
    }

    public static ItemStack getWornItem(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof JetpackItem)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack s = new ItemStack(this);
        var cap = s.getCapability(Capabilities.FluidHandler.ITEM);
        if (cap != null) {
            cap.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
        }
        return s;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack me, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        if (action != ClickAction.SECONDARY) return false;
        var otherCap = other.getCapability(Capabilities.FluidHandler.ITEM);
        if (otherCap != null) {
            FluidStack drainable = otherCap.drain(new FluidStack(Fluids.LAVA, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
            if (!drainable.isEmpty()) {
                var ourCap = me.getCapability(Capabilities.FluidHandler.ITEM);
                if (ourCap != null) {
                    int fillable = ourCap.fill(drainable, IFluidHandler.FluidAction.SIMULATE);
                    FluidStack drained = otherCap.drain(new FluidStack(Fluids.LAVA, fillable), IFluidHandler.FluidAction.EXECUTE);
                    ourCap.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    carriedSlotAccess.set(otherCap.getContainer());
                    if (other.getItem() instanceof BucketItem b) {
                        b.playEmptySound(player, player.level(), player.blockPosition());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack p_77616_1_) {
        return true;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.MENDING) || enchantment.is(Enchantments.UNBREAKING))
            return false;
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBarVisible(ItemStack p_150899_) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Mth.clamp(getAir(stack) / ((float) getMaxAirCapacity(stack)), 0, 1));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xfa9600;
    }

    @Override
    public int getMaxAirCapacity(ItemStack backtank) {
        var cap = backtank.getCapability(Capabilities.FluidHandler.ITEM);
        if (cap == null) return IBacktank.super.getMaxAirCapacity(backtank);
        return cap.getTankCapacity(0);
    }

    @Override
    public int getAir(ItemStack backtank) {
        var cap = backtank.getCapability(Capabilities.FluidHandler.ITEM);
        if (cap == null) return IBacktank.super.getAir(backtank);
        return cap.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).getAmount();
    }

    @Override
    public Integer setAir(ItemStack backtank, int newAir) {
        var cap = backtank.getCapability(Capabilities.FluidHandler.ITEM);
        if (cap == null) return IBacktank.super.setAir(backtank, newAir);
        FluidStack contained = cap.getFluidInTank(0);
        if (contained.isEmpty() && newAir > 0) {
            contained = new FluidStack(Fluids.LAVA, newAir);
            cap.fill(contained, IFluidHandler.FluidAction.EXECUTE);
        } else if (newAir > contained.getAmount()) {
            FluidStack fill = contained.copyWithAmount(newAir - contained.getAmount());
            cap.fill(fill, IFluidHandler.FluidAction.EXECUTE);
        } else if (newAir < contained.getAmount()) {
            cap.drain(contained.getAmount() - newAir, IFluidHandler.FluidAction.EXECUTE);
        }
        return contained.getAmount();
    }
}
