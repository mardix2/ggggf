package net.ironman.suit;

import net.ironman.net.ModNetworking;
import net.ironman.net.payload.SuitStatePayload;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Серверная логика костюма: энергия реактора, полёт, пассивные системы.
 *
 * <p><b>Как устроен полёт.</b> Пока полётные системы включены, сервер выдаёт
 * игроку ванильное право летать ({@code allowFlying}) со скоростью модели
 * костюма. Благодаря этому античит ванили не выкидывает летящего игрока,
 * а управление привычное: двойной пробел — взлёт, пробел/Shift — вверх/вниз.
 * Реактивный рывок (бег в полёте) добавляет клиент — см. {@code FlightController}.
 */
public final class SuitManager {
    private SuitManager() {
    }

    /** Базовый расход энергии на зависание, за тик. */
    private static final float HOVER_COST = 1.5f;
    /** Дополнительный расход за каждый блок/тик скорости сверх крейсерской. */
    private static final float SPEED_COST = 3.0f;
    private static final double CRUISE_SPEED = 0.6;
    /** Сколько тиков после траты энергии реактор не заряжается. */
    private static final int RECHARGE_DELAY = 20;
    /** Энергия на гашение одной единицы урона от падения. */
    private static final float FALL_COST_PER_DAMAGE = 12.0f;
    private static final float DEFAULT_FLY_SPEED = 0.05f;

    /** Временное состояние, которое не нужно сохранять в мир. */
    static final class Runtime {
        boolean flightArmed;
        boolean grantedFlight;
        boolean nightVision = true;
        int primaryCooldown;
        int unibeamCooldown;
        int missileCooldown;
        int idleTicks;
        Vec3d lastPos;
        SuitStatePayload lastSent;
        boolean lowPowerWarned;
    }

    private static final Map<UUID, Runtime> RUNTIME = new HashMap<>();

    static Runtime runtime(PlayerEntity player) {
        return RUNTIME.computeIfAbsent(player.getUuid(), uuid -> new Runtime());
    }

    // ---- энергия ---------------------------------------------------------

    public static ReactorData reactor(PlayerEntity player) {
        return player.getAttachedOrCreate(ReactorAttachments.REACTOR);
    }

    public static void setReactor(ServerPlayerEntity player, ReactorData data) {
        player.setAttached(ReactorAttachments.REACTOR, data);
        sync(player);
    }

    /** Списывает энергию; {@code false}, если её не хватило (тогда ничего не списано). */
    public static boolean consume(ServerPlayerEntity player, float amount) {
        if (player.isCreative()) {
            runtime(player).idleTicks = 0;
            return true;
        }
        ReactorData data = reactor(player);
        if (!data.installed() || data.energy() < amount) {
            return false;
        }
        player.setAttached(ReactorAttachments.REACTOR, data.withEnergy(data.energy() - amount));
        runtime(player).idleTicks = 0;
        return true;
    }

    /**
     * Списывает сколько есть, не больше остатка. Нужна для непрерывных трат
     * вроде полёта: иначе при заряде меньше цены тика энергия «зависала» бы
     * на остатке и полёт не кончался никогда.
     */
    public static void drain(ServerPlayerEntity player, float amount) {
        runtime(player).idleTicks = 0;
        if (player.isCreative()) {
            return;
        }
        ReactorData data = reactor(player);
        if (data.installed()) {
            player.setAttached(ReactorAttachments.REACTOR, data.withEnergy(data.energy() - amount));
        }
    }

    /** Костюм полностью надет, реактор стоит и в нём есть энергия. */
    public static SuitTier onlineTier(PlayerEntity player) {
        SuitTier tier = Suit.tierOf(player);
        if (tier == null) {
            return null;
        }
        ReactorData data = reactor(player);
        if (!data.installed() || (data.energy() <= 0.0f && !player.isCreative())) {
            return null;
        }
        return tier;
    }

    // ---- тик ---------------------------------------------------------------

    public static void tick(ServerPlayerEntity player) {
        Runtime rt = runtime(player);
        if (rt.primaryCooldown > 0) {
            rt.primaryCooldown--;
        }
        if (rt.unibeamCooldown > 0) {
            rt.unibeamCooldown--;
        }
        if (rt.missileCooldown > 0) {
            rt.missileCooldown--;
        }

        SuitTier tier = onlineTier(player);
        tickFlight(player, rt, tier);
        tickRecharge(player, rt);
        tickPassives(player, rt, tier);

        rt.lastPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        sync(player);
    }

    private static void tickFlight(ServerPlayerEntity player, Runtime rt, SuitTier tier) {
        if (rt.flightArmed && Suit.tierOf(player) == null) {
            // Костюм сняли — полётные системы отключаются сами.
            rt.flightArmed = false;
        }

        boolean shouldFly = rt.flightArmed && tier != null;
        if (shouldFly && !rt.grantedFlight) {
            grantFlight(player, tier);
            rt.grantedFlight = true;
        } else if (!shouldFly && rt.grantedFlight) {
            revokeFlight(player);
            rt.grantedFlight = false;
            if (rt.flightArmed) {
                player.sendMessage(Text.translatable("message.ironman.flight_no_power")
                        .formatted(Formatting.RED), true);
                rt.flightArmed = false;
            }
        }

        if (!rt.grantedFlight || tier == null) {
            return;
        }
        PlayerAbilities abilities = player.getAbilities();
        if (!abilities.flying) {
            return;
        }

        double speed = rt.lastPos == null ? 0.0 : Math.sqrt(player.squaredDistanceTo(rt.lastPos));
        float cost = (HOVER_COST + (float) Math.max(0.0, speed - CRUISE_SPEED) * SPEED_COST)
                * tier.costMultiplier();
        drain(player, cost);
        player.fallDistance = 0.0;

        if (player.getEntityWorld() instanceof ServerWorld world) {
            spawnThrusters(world, player, speed > CRUISE_SPEED);
            if (player.age % 12 == 0) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.PLAYERS,
                        0.4f, 1.6f + player.getRandom().nextFloat() * 0.3f);
            }
        }
    }

    private static void grantFlight(ServerPlayerEntity player, SuitTier tier) {
        PlayerAbilities abilities = player.getAbilities();
        if (!player.isCreative() && !player.isSpectator()) {
            abilities.allowFlying = true;
            // Если игрок уже в воздухе — сразу подхватываем его.
            if (!player.isOnGround()) {
                abilities.flying = true;
            }
        }
        abilities.setFlySpeed(tier.flySpeed());
        player.sendAbilitiesUpdate();
        player.setAttached(ReactorAttachments.FLIGHT_GRANTED, Boolean.TRUE);
    }

    private static void revokeFlight(ServerPlayerEntity player) {
        PlayerAbilities abilities = player.getAbilities();
        if (!player.isCreative() && !player.isSpectator()) {
            abilities.allowFlying = false;
            abilities.flying = false;
        }
        abilities.setFlySpeed(DEFAULT_FLY_SPEED);
        player.sendAbilitiesUpdate();
        player.removeAttached(ReactorAttachments.FLIGHT_GRANTED);
    }

    /** Струи ускорителей под ботинками; при рывке пламя становится голубым. */
    private static void spawnThrusters(ServerWorld world, ServerPlayerEntity player, boolean boosting) {
        float yaw = player.getBodyYaw() * MathHelper.RADIANS_PER_DEGREE;
        double sideX = Math.cos(yaw) * 0.18;
        double sideZ = Math.sin(yaw) * 0.18;
        double y = player.getY() - 0.1;
        for (int side = -1; side <= 1; side += 2) {
            double x = player.getX() + sideX * side;
            double z = player.getZ() + sideZ * side;
            world.spawnParticles(ParticleTypes.FLAME, x, y, z, 1, 0.02, 0.0, 0.02, 0.01);
            world.spawnParticles(ParticleTypes.SMOKE, x, y - 0.2, z, 1, 0.03, 0.02, 0.03, 0.0);
            if (boosting) {
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y - 0.1, z, 2, 0.04, 0.0, 0.04, 0.02);
            }
        }
    }

    private static void tickRecharge(ServerPlayerEntity player, Runtime rt) {
        rt.idleTicks++;
        if (rt.idleTicks < RECHARGE_DELAY) {
            return;
        }
        ReactorData data = reactor(player);
        if (!data.installed() || data.energy() >= data.capacity()) {
            rt.lowPowerWarned = false;
            return;
        }
        player.setAttached(ReactorAttachments.REACTOR, data.withEnergy(data.energy() + data.reactor().regen()));
    }

    private static void tickPassives(ServerPlayerEntity player, Runtime rt, SuitTier tier) {
        ReactorData data = reactor(player);

        // Ночное зрение даёт любой шлем, если в груди работает реактор.
        if (rt.nightVision && Suit.helmetTier(player) != null && data.installed() && data.energy() > 0.0f) {
            refresh(player, new StatusEffectInstance(StatusEffects.NIGHT_VISION, 320, 0, true, false, false));
        }
        if (tier == null) {
            return;
        }
        if (tier.slowness() > 0.0f && player.isOnGround()) {
            refresh(player, new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, false));
        }
        if (tier.sealed()) {
            refresh(player, new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 80, 0, true, false, false));
            refresh(player, new StatusEffectInstance(StatusEffects.WATER_BREATHING, 80, 0, true, false, false));
        }

        if (data.fraction() < 0.15f && !rt.lowPowerWarned && !player.isCreative()) {
            rt.lowPowerWarned = true;
            player.sendMessage(Text.translatable("message.ironman.low_power")
                    .formatted(Formatting.GOLD), true);
        }
    }

    /** Эффект обновляется заранее, чтобы ночное зрение не мигало перед концом. */
    private static void refresh(ServerPlayerEntity player, StatusEffectInstance effect) {
        StatusEffectInstance current = player.getStatusEffect(effect.getEffectType());
        if (current == null || current.getDuration() < effect.getDuration() / 2) {
            player.addStatusEffect(effect);
        }
    }

    // ---- команды клиента ---------------------------------------------------

    public static void handleAction(ServerPlayerEntity player, SuitAction action) {
        Runtime rt = runtime(player);
        switch (action) {
            case TOGGLE_FLIGHT -> toggleFlight(player, rt);
            case TOGGLE_NIGHT_VISION -> toggleNightVision(player, rt);
            case PRIMARY -> Weapons.firePrimary(player, rt);
            case UNIBEAM -> Weapons.fireUnibeam(player, rt);
            case MISSILES -> Weapons.fireMissiles(player, rt);
        }
        sync(player);
    }

    private static void toggleFlight(ServerPlayerEntity player, Runtime rt) {
        if (rt.flightArmed) {
            rt.flightArmed = false;
            player.sendMessage(Text.translatable("message.ironman.flight_off")
                    .formatted(Formatting.GRAY), true);
            return;
        }
        if (Suit.tierOf(player) == null) {
            player.sendMessage(Text.translatable("message.ironman.suit_incomplete")
                    .formatted(Formatting.RED), true);
            return;
        }
        if (onlineTier(player) == null) {
            player.sendMessage(Text.translatable(reactor(player).installed()
                    ? "message.ironman.flight_no_power" : "message.ironman.no_reactor")
                    .formatted(Formatting.RED), true);
            return;
        }
        rt.flightArmed = true;
        player.sendMessage(Text.translatable("message.ironman.flight_on")
                .formatted(Formatting.AQUA), true);
        if (player.getEntityWorld() instanceof ServerWorld world) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.8f);
        }
    }

    private static void toggleNightVision(ServerPlayerEntity player, Runtime rt) {
        rt.nightVision = !rt.nightVision;
        if (!rt.nightVision) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        player.sendMessage(Text.translatable(rt.nightVision
                ? "message.ironman.night_vision_on" : "message.ironman.night_vision_off")
                .formatted(Formatting.AQUA), true);
    }

    // ---- урон ----------------------------------------------------------------

    /**
     * Костюм гасит урон от падения за счёт энергии. Событие умеет только
     * отменить урон целиком, поэтому правило простое: хватает энергии —
     * удар поглощён, не хватает — проходит полностью.
     */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player) || !source.isIn(DamageTypeTags.IS_FALL)) {
            return true;
        }
        SuitTier tier = onlineTier(player);
        if (tier == null) {
            return true;
        }
        if (!consume(player, amount * FALL_COST_PER_DAMAGE * tier.costMultiplier())) {
            return true;
        }
        if (player.getEntityWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                    12, 0.4, 0.05, 0.4, 0.05);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_IRON_GOLEM_STEP, SoundCategory.PLAYERS, 1.0f, 0.6f);
        }
        return false;
    }

    // ---- синхронизация и жизненный цикл ---------------------------------------

    public static void sync(ServerPlayerEntity player) {
        Runtime rt = runtime(player);
        ReactorData data = reactor(player);
        SuitStatePayload state = new SuitStatePayload(
                data.reactor().ordinal(), (int) data.energy(), data.capacity(),
                rt.flightArmed, rt.nightVision,
                rt.primaryCooldown, rt.unibeamCooldown, rt.missileCooldown);
        if (!state.equals(rt.lastSent)) {
            rt.lastSent = state;
            ModNetworking.send(player, state);
        }
    }

    /** Игрок возродился: костюм остался на месте смерти, право полёта забираем. */
    public static void onRespawn(ServerPlayerEntity player) {
        Runtime rt = runtime(player);
        if (rt.grantedFlight) {
            revokeFlight(player);
        }
        rt.grantedFlight = false;
        rt.flightArmed = false;
        rt.lastSent = null;
        rt.lastPos = null;
        sync(player);
    }

    /**
     * Игрок выходит: право полёта сохраняется в файл игрока, поэтому его
     * обязательно нужно забрать, иначе после входа он летал бы без костюма.
     */
    public static void onDisconnect(ServerPlayerEntity player) {
        Runtime rt = RUNTIME.remove(player.getUuid());
        if (rt != null && rt.grantedFlight) {
            revokeFlight(player);
        }
    }

    /**
     * Страховка на случай, если сервер упал, не дав забрать право полёта
     * при выходе: метка в данных игрока говорит, что его выдал костюм.
     */
    public static void onJoin(ServerPlayerEntity player) {
        if (Boolean.TRUE.equals(player.getAttached(ReactorAttachments.FLIGHT_GRANTED))) {
            revokeFlight(player);
        }
        runtime(player).lastSent = null;
        sync(player);
    }
}
