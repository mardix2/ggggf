package net.ironman.registry;

import net.ironman.IronMan;
import net.ironman.entity.MicroMissileEntity;
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

    /** Ракету рисует ванильный рендерер летящих предметов — отдельная модель не нужна. */
    public static final EntityType<MicroMissileEntity> MICRO_MISSILE = register("micro_missile",
            EntityType.Builder.<MicroMissileEntity>create(MicroMissileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.3f, 0.3f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(1));

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, IronMan.id(name));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
