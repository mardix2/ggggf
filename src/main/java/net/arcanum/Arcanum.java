package net.arcanum;

import net.arcanum.command.ArcanumCommands;
import net.arcanum.mana.ManaAttachments;
import net.arcanum.recipe.InfusionRecipeManager;
import net.arcanum.registry.ModBlockEntities;
import net.arcanum.registry.ModBlocks;
import net.arcanum.registry.ModComponents;
import net.arcanum.registry.ModEffects;
import net.arcanum.registry.ModEntities;
import net.arcanum.registry.ModEvents;
import net.arcanum.registry.ModItemGroups;
import net.arcanum.registry.ModItems;
import net.arcanum.net.ModNetworking;
import net.arcanum.spell.Spells;
import net.arcanum.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arcanum Mysticum — большой магический мод.
 *
 * <p>Точка входа общей (сервер + клиент) части. Порядок инициализации важен:
 * компоненты и эффекты регистрируются раньше предметов, потому что предметы
 * ссылаются на них в своих {@code Settings}.
 */
public class Arcanum implements ModInitializer {
    public static final String MOD_ID = "arcanum";
    public static final Logger LOGGER = LoggerFactory.getLogger("Arcanum");

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.init();
        ModEffects.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModEntities.init();
        ModItemGroups.init();

        ManaAttachments.init();
        Spells.init();

        ModNetworking.initCommon();
        ModEvents.init();
        ModWorldGen.init();
        ArcanumCommands.init();
        InfusionRecipeManager.init();

        LOGGER.info("Arcanum Mysticum: загружено {} заклинаний", net.arcanum.spell.SpellRegistry.size());
    }
}
