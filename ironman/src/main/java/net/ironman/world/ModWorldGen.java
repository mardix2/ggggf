package net.ironman.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.ironman.IronMan;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Палладиевая руда в Верхнем мире. Сама жила описана файлами
 * {@code data/ironman/worldgen/...} — её можно поправить датапаком.
 */
public final class ModWorldGen {
    private ModWorldGen() {
    }

    private static final RegistryKey<PlacedFeature> PALLADIUM_ORE =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, IronMan.id("palladium_ore_placed"));

    private static final RegistryKey<PlacedFeature> PALLADIUM_ORE_DEEP =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, IronMan.id("palladium_ore_deep_placed"));

    public static void init() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES, PALLADIUM_ORE);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES, PALLADIUM_ORE_DEEP);
    }
}
