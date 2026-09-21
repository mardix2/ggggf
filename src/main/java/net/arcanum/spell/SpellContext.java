package net.arcanum.spell;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Всё, что нужно заклинанию в момент применения.
 *
 * @param world  серверный мир (заклинания выполняются только на сервере)
 * @param caster игрок-заклинатель
 * @param tool   посох/свиток, которым колдуют (может быть пустым)
 * @param power  итоговый множитель силы: 1.0 — база, 1.5 — +50%
 * @param tier   уровень посоха: 1 — ученический, 4 — архимага
 */
public record SpellContext(ServerWorld world, ServerPlayerEntity caster, ItemStack tool, float power, int tier) {

    public Vec3d eyes() {
        return caster.getEyePos();
    }

    public Vec3d look() {
        return caster.getRotationVec(1.0f);
    }

    public Vec3d feet() {
        return caster.getPos();
    }

    public Random random() {
        return world.getRandom();
    }

    /** Масштабирует базовое значение силой заклинателя. */
    public float scaled(float base) {
        return base * power;
    }

    public SpellContext withPower(float newPower) {
        return new SpellContext(world, caster, tool, newPower, tier);
    }
}
