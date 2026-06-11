package dev.devce.rocketnautics.content.items;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.BaseArmorItem;
import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.api.orbit.AtmosphereFlags;
import dev.devce.rocketnautics.content.physics.GlobalSpacePhysicsHandler;
import dev.devce.rocketnautics.registry.RocketDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@EventBusSubscriber
public class LegThrustersItem extends BaseArmorItem {

    public static final AttributeModifier flightAttributeModifier =
            new AttributeModifier(RocketNautics.path("flight_attribute_modifier"), 1,
                    AttributeModifier.Operation.ADD_VALUE);

    private static final Supplier<Multimap<Holder<Attribute>, AttributeModifier>> flightModifier = Suppliers.memoize(() ->
            ImmutableMultimap.of(NeoForgeMod.CREATIVE_FLIGHT, flightAttributeModifier));

    public LegThrustersItem(Holder<ArmorMaterial> armorMaterial, Properties properties, ResourceLocation textureLoc) {
        super(armorMaterial, Type.LEGGINGS, properties, textureLoc);
    }

    public static void toggle(ServerPlayer player) {
        ItemStack worn = getWornItem(player);
        if (worn.getItem() instanceof LegThrustersItem j) {
            boolean wasActive = j.setActive(worn, !j.isActive(worn));
            if (wasActive) {
                player.displayClientMessage(Component.translatable("rocketnautics.dampeners.disabled").withStyle(ChatFormatting.RED), true);
                worn.remove(RocketDataComponents.DAMPENER_RELATIVE_SUBLEVEL);
            } else {
                SubLevel containing = Sable.HELPER.getContaining(player);
                if (containing == null) {
                    containing = ((EntityMovementExtension) player).sable$getTrackingSubLevel();
                }
                if (containing == null || containing.isRemoved()) {
                    player.displayClientMessage(Component.translatable("rocketnautics.dampeners.enabled").withStyle(ChatFormatting.GREEN), true);
                    worn.remove(RocketDataComponents.DAMPENER_RELATIVE_SUBLEVEL);
                } else {
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    worn.set(RocketDataComponents.DAMPENER_RELATIVE_SUBLEVEL, containing.getUniqueId());
                    player.displayClientMessage(Component.translatable("rocketnautics.dampeners.relative").withStyle(ChatFormatting.GREEN), true);
                }
            }
        }
    }

    public static boolean isWornBy(Entity entity) {
        return !getWornItem(entity).isEmpty();
    }

    public static ItemStack getWornItem(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = livingEntity.getItemBySlot(EquipmentSlot.LEGS);
        if (!(stack.getItem() instanceof LegThrustersItem)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @SubscribeEvent
    public static void entityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player entity))
            return;
        Level level = entity.level();
        if (level.isClientSide && !GlobalSpacePhysicsHandler.shouldDisplayTimer(entity))
            entity.getPersistentData().remove("VisualBacktankAir");

        List<ItemStack> backtanks = BacktankUtil.getAllWithAir(entity);
        if (backtanks.isEmpty() && !entity.hasInfiniteMaterials()) {
            removeAttribute(entity);
            return;
        }

        boolean active = allowCreativeFlight(entity);
        active |= handleInertialDamping(entity);

        if (!active || entity.hasInfiniteMaterials()) return;

        if (level.isClientSide) {
            float visualBacktankAir = 0f;
            for (ItemStack stack : backtanks)
                visualBacktankAir += BacktankUtil.getAir(stack);

            entity.getPersistentData()
                    .putInt("VisualBacktankAir", Math.round(visualBacktankAir));
        }

        boolean inFluid = !entity.isEyeInFluidType(NeoForgeMod.EMPTY_TYPE.value());
        boolean inSpace = GlobalSpacePhysicsHandler.getFlags(entity).contains(AtmosphereFlags.LOW_DENSITY);

        int period = inSpace ? 4 : inFluid ? 2 : 1;

        if (level.getGameTime() % period == 0)
            BacktankUtil.consumeAir(entity, backtanks.getFirst(), RocketConfig.SERVER.legThrusterBaseConsumption.getAsInt());
    }

    private static boolean handleInertialDamping(Player entity) {
        ItemStack wornItem = getWornItem(entity);
        if (wornItem.isEmpty()) {
            return false;
        }

        if (!((LegThrustersItem) wornItem.getItem()).isActive(wornItem)) {
            return false;
        }

        UUID relativeID = wornItem.get(RocketDataComponents.DAMPENER_RELATIVE_SUBLEVEL);
        SubLevelContainer c = relativeID != null ? SubLevelContainer.getContainer(entity.level()) : null;
        SubLevel relative = c != null ? c.getSubLevel(relativeID) : null;
        if (relative != null && ((EntityMovementExtension) entity).sable$getTrackingSubLevel() != relative) {
            ((LivingEntityMovementExtension) entity).sable$getInheritedVelocity().set(relative.logicalPose().position().sub(relative.lastPose().position(), new Vector3d()));
        }
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.9));
        return true;
    }

    private static boolean allowCreativeFlight(Player entity) {
        if (!isWornBy(entity)) {
            removeAttribute(entity);
            return false;
        }

        addAttribute(entity);

        if (!entity.getAbilities().flying)
            return false;
        // TODO advancements
//        if (entity instanceof ServerPlayer sp)
//            CRAdvancements.COPPER_THRUSTER.awardTo(sp);
        return true;
    }

    public boolean isActive(ItemStack stack) {
        return stack.getOrDefault(RocketDataComponents.SYSTEMS_ACTIVE, false);
    }

    public boolean setActive(ItemStack stack, boolean active) {
        return Boolean.TRUE.equals(stack.set(RocketDataComponents.SYSTEMS_ACTIVE, active));
    }

    public static boolean legThrustersActive(Player entity) {
        if (entity.hasInfiniteMaterials() || !isWornBy(entity)) {
            return false;
        }

        List<ItemStack> backtanks = BacktankUtil.getAllWithAir(entity);
        if (backtanks.isEmpty()) {
            return false;
        }

        ItemStack worn = getWornItem(entity);
        if (((LegThrustersItem) worn.getItem()).isActive(worn)) {
            return true;
        }

        if (!entity.getAbilities().flying)
            return false;
        return true;
    }

    private static void addAttribute(Player entity) {
        if (entity.getPersistentData().contains("FlightLeggings")) {
            NBTHelper.putMarker(entity.getPersistentData(), "FlightLeggings");
        }
        if (!entity.getAttributes().hasModifier(NeoForgeMod.CREATIVE_FLIGHT, flightAttributeModifier.id())) {
            entity.getAttributes().addTransientAttributeModifiers(flightModifier.get());
        }
    }

    private static void removeAttribute(Player entity) {
        if (!entity.getPersistentData().contains("FlightLeggings")) {
            entity.getPersistentData().remove("FlightLeggings");
        }
        if (entity.getAttributes().hasModifier(NeoForgeMod.CREATIVE_FLIGHT, flightAttributeModifier.id())) {
            entity.getAttributes().removeAttributeModifiers(flightModifier.get());
        }
    }
}
