package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.arcanum.entity.SpellProjectileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

/** Типы сущностей мода. */
public final class ModEntities {
    private ModEntities() {
    }

    /**
     * Единственный тип снаряда на все школы — подробности в
     * {@link SpellProjectileEntity}.
     */
    public static final EntityType<SpellProjectileEntity> SPELL_PROJECTILE = register("spell_projectile",
            EntityType.Builder.<SpellProjectileEntity>create(SpellProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.4f, 0.4f)
                    .maxTrackingRange(6)
                    .trackingTickInterval(2));

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Arcanum.id(name));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
