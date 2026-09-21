package net.arcanum.spell;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Всё, что нужно заклинанию в момент применения.
 *
 * @param world      серверный мир (заклинания выполняются только на сервере)
 * @param caster     игрок-заклинатель
 * @param tool       посох/свиток, которым колдуют (может быть пустым)
 * @param spell      само заклинание — нужно для реакций школ и сообщений
 * @param power      итоговый множитель силы: 1.0 — база, 1.5 — +50%
 * @param range      множитель дальности от руны дальнобойности
 * @param tier       уровень посоха: 1 — ученический, 4 — архимага
 * @param extraShots сколько дополнительных снарядов даёт руна расщепления
 */
public record SpellContext(ServerWorld world, ServerPlayerEntity caster, ItemStack tool,
                           Spell spell, float power, float range, int tier, int extraShots) {

    public SpellSchool school() {
        return spell.school();
    }

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

    /** Масштабирует базовую дальность рунами посоха. */
    public double reach(double base) {
        return base * range;
    }

    public SpellContext withPower(float newPower) {
        return new SpellContext(world, caster, tool, spell, newPower, range, tier, extraShots);
    }
}
