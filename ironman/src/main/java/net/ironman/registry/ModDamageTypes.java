package net.ironman.registry;

import net.ironman.IronMan;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;

/**
 * Типы урона оружия костюма. Сами типы описаны файлами
 * {@code data/ironman/damage_type/*.json}, здесь — только ключи.
 */
public final class ModDamageTypes {
    private ModDamageTypes() {
    }

    public static final RegistryKey<DamageType> REPULSOR = key("repulsor");
    public static final RegistryKey<DamageType> UNIBEAM = key("unibeam");
    public static final RegistryKey<DamageType> MICRO_MISSILE = key("micro_missile");
    public static final RegistryKey<DamageType> FLAMETHROWER = key("flamethrower");

    /**
     * @param direct   что непосредственно попало (снаряд), или сам атакующий
     * @param attacker кто стрелял — он попадёт в сообщение о смерти
     */
    public static DamageSource source(ServerWorld world, RegistryKey<DamageType> key,
                                      Entity direct, Entity attacker) {
        return new DamageSource(
                world.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(key),
                direct, attacker);
    }

    private static RegistryKey<DamageType> key(String name) {
        return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, IronMan.id(name));
    }
}
