package net.arcanum.world;

import net.arcanum.Arcanum;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/** Собственные фичи генерации мира. */
public final class ModFeatures {
    private ModFeatures() {
    }

    public static final Feature<DefaultFeatureConfig> WIZARD_TOWER = Registry.register(
            Registries.FEATURE, Arcanum.id("wizard_tower"),
            new WizardTowerFeature(DefaultFeatureConfig.CODEC));

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
