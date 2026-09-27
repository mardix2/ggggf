package net.ironman;

import net.fabricmc.api.ModInitializer;
import net.ironman.command.IronManCommands;
import net.ironman.net.ModNetworking;
import net.ironman.registry.ModBlocks;
import net.ironman.registry.ModEntities;
import net.ironman.registry.ModEvents;
import net.ironman.registry.ModItemGroups;
import net.ironman.registry.ModItems;
import net.ironman.suit.ReactorAttachments;
import net.ironman.world.ModWorldGen;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stark Industries — мод, который делает из игрока Железного человека.
 *
 * <p>Точка входа общей части. Компоненты регистрируются раньше предметов:
 * настройки предметов на них ссылаются.
 */
public class IronMan implements ModInitializer {
    public static final String MOD_ID = "ironman";
    public static final Logger LOGGER = LoggerFactory.getLogger("IronMan");

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        ModEntities.init();
        ModItemGroups.init();

        ReactorAttachments.init();
        ModNetworking.initCommon();
        ModEvents.init();
        ModWorldGen.init();
        IronManCommands.init();

        LOGGER.info("Stark Industries: системы костюма готовы");
    }
}
