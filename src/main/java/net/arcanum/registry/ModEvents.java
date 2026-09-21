package net.arcanum.registry;

import net.arcanum.mana.ManaManager;
import net.arcanum.net.ModNetworking;
import net.arcanum.spell.util.SpellTicker;
import net.arcanum.spell.util.SpellUtil;
import net.arcanum.spell.util.SummonManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.arcanum.spell.util.Fx;

import java.util.List;

/**
 * Серверные события: тик маны, длительные заклинания, призванные существа
 * и поведение эффектов мода.
 */
public final class ModEvents {
    private ModEvents() {
    }

    /** Сколько маны списывается за одну единицу поглощённого урона. */
    private static final float SHIELD_MANA_PER_DAMAGE = 8.0f;
    /** Как часто «Взор мага» подсвечивает существ. */
    private static final int SIGHT_INTERVAL = 20;
    private static final double SIGHT_RANGE = 24.0;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            SpellTicker.tick(server);
            SummonManager.tick(server);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ManaManager.tick(player);
                tickArcaneSight(player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ModNetworking.syncAll(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            ManaManager.onDisconnect(player);
            ModNetworking.forget(player.getUuid());
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                ModNetworking.syncAll(newPlayer));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(ModEvents::arcaneShield);

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SpellTicker.clear());
    }

    /**
     * «Магический щит»: урон уходит в ману, а не в здоровье.
     *
     * <p>Событие умеет только полностью отменить урон, поэтому щит работает
     * по принципу «всё или ничего»: не хватило маны — эффект спадает и удар
     * проходит целиком.
     */
    private static boolean arcaneShield(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }
        StatusEffectInstance effect = player.getStatusEffect(ModEffects.ARCANE_SHIELD);
        if (effect == null || amount <= 0.0f) {
            return true;
        }
        // Урон «из-за границ мира» и команды щит не держит.
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }

        float cost = amount * SHIELD_MANA_PER_DAMAGE / (1.0f + effect.getAmplifier() * 0.25f);
        if (!ManaManager.consume(player, cost)) {
            player.removeStatusEffect(ModEffects.ARCANE_SHIELD);
            return true;
        }

        if (player.getWorld() instanceof ServerWorld world) {
            Fx.sphere(world, player.getPos().add(0.0, 1.0, 0.0), 0.9,
                    Fx.dust(0xB388FF, 1.0f), 16);
            Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.6f);
        }
        return false;
    }

    /** «Взор мага» подсвечивает всё живое вокруг. */
    private static void tickArcaneSight(ServerPlayerEntity player) {
        if (player.age % SIGHT_INTERVAL != 0 || !player.hasStatusEffect(ModEffects.ARCANE_SIGHT)) {
            return;
        }
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        List<LivingEntity> targets = SpellUtil.around(world, player.getPos(), SIGHT_RANGE,
                entity -> entity != player);
        for (LivingEntity target : targets) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,
                    SIGHT_INTERVAL + 10, 0, false, false, false));
        }
        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getEyeY(), player.getZ(),
                3, 0.3, 0.3, 0.3, 0.01);
    }
}
