package net.ironman.registry;

import net.ironman.IronMan;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.intprovider.ConstantIntProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Блоки мода: палладиевая руда и блок палладия. */
public final class ModBlocks {
    private ModBlocks() {
    }

    private static final List<Block> ORDERED = new ArrayList<>();

    public static final Block PALLADIUM_ORE = register("palladium_ore",
            settings -> new ExperienceDroppingBlock(ConstantIntProvider.create(0), settings),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresTool()
                    .strength(3.0f, 3.0f));

    public static final Block DEEPSLATE_PALLADIUM_ORE = register("deepslate_palladium_ore",
            settings -> new ExperienceDroppingBlock(ConstantIntProvider.create(0), settings),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.DEEPSLATE_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresTool()
                    .strength(4.5f, 3.0f)
                    .sounds(BlockSoundGroup.DEEPSLATE));

    public static final Block PALLADIUM_BLOCK = register("palladium_block",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresTool()
                    .strength(5.0f, 6.0f)
                    .sounds(BlockSoundGroup.METAL));

    private static Block register(String name, Function<AbstractBlock.Settings, Block> factory,
                                  AbstractBlock.Settings settings) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, IronMan.id(name));
        Block block = factory.apply(settings.registryKey(blockKey));
        Registry.register(Registries.BLOCK, blockKey, block);

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, IronMan.id(name));
        Registry.register(Registries.ITEM, itemKey, new BlockItem(block,
                new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));

        ORDERED.add(block);
        return block;
    }

    public static List<Block> ordered() {
        return List.copyOf(ORDERED);
    }

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
