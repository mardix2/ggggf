package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.arcanum.entity.InfusionAltarBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Блок-сущности мода. */
public final class ModBlockEntities {
    private ModBlockEntities() {
    }

    public static final BlockEntityType<InfusionAltarBlockEntity> INFUSION_ALTAR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Arcanum.id("infusion_altar"),
            FabricBlockEntityTypeBuilder.create(InfusionAltarBlockEntity::new, ModBlocks.INFUSION_ALTAR).build());

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
