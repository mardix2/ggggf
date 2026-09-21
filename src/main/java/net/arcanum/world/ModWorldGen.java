package net.arcanum.world;

import net.arcanum.Arcanum;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Подключение генерации мода к мирам.
 *
 * <p>Сами фичи описаны файлами в {@code data/arcanum/worldgen/...} — так их
 * может переопределить любой датапак, не трогая код.
 */
public final class ModWorldGen {
    private ModWorldGen() {
    }

    private static final RegistryKey<PlacedFeature> ARCANE_CRYSTAL_ORE =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Arcanum.id("arcane_crystal_ore_placed"));

    private static final RegistryKey<PlacedFeature> ARCANE_CRYSTAL_ORE_DEEP =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Arcanum.id("arcane_crystal_ore_deep_placed"));

    private static final RegistryKey<PlacedFeature> MOONPETAL =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Arcanum.id("moonpetal_placed"));

    public static void init() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES, ARCANE_CRYSTAL_ORE);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES, ARCANE_CRYSTAL_ORE_DEEP);
        // Лунный лепесток растёт только там, где бывает ночная прохлада.
        BiomeModifications.addFeature(
                BiomeSelectors.tag(BiomeTags.IS_FOREST),
                GenerationStep.Feature.VEGETAL_DECORATION, MOONPETAL);
    }
}
