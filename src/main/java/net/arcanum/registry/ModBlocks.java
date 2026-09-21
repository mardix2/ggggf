package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.arcanum.block.ArcaneLampBlock;
import net.arcanum.block.ArcanePedestalBlock;
import net.arcanum.block.InfusionAltarBlock;
import net.arcanum.block.ManaFontBlock;
import net.arcanum.block.RunestoneBlock;
import net.arcanum.block.TeleportAnchorBlock;
import net.arcanum.spell.SpellSchool;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Все блоки мода. */
public final class ModBlocks {
    private ModBlocks() {
    }

    /** Порядок регистрации = порядок в творческой вкладке. */
    private static final List<Block> ORDERED = new ArrayList<>();

    // ---- руда и сырьё -------------------------------------------------

    public static final Block ARCANE_CRYSTAL_ORE = register("arcane_crystal_ore",
            settings -> new ExperienceDroppingBlock(UniformIntProvider.create(3, 7), settings),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresTool()
                    .strength(3.0f, 3.0f));

    public static final Block DEEPSLATE_ARCANE_CRYSTAL_ORE = register("deepslate_arcane_crystal_ore",
            settings -> new ExperienceDroppingBlock(UniformIntProvider.create(3, 7), settings),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.DEEPSLATE_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresTool()
                    .strength(4.5f, 3.0f)
                    .sounds(BlockSoundGroup.DEEPSLATE));

    public static final Block ARCANE_CRYSTAL_BLOCK = register("arcane_crystal_block",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.PURPLE)
                    .requiresTool()
                    .strength(4.0f, 6.0f)
                    .luminance(state -> 6)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK));

    public static final Block ARCANE_CRYSTAL_CLUSTER = register("arcane_crystal_cluster",
            settings -> new AmethystClusterBlock(7.0f, 3.0f, settings),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.PURPLE)
                    .nonOpaque()
                    .strength(1.5f)
                    .luminance(state -> 7)
                    .sounds(BlockSoundGroup.AMETHYST_CLUSTER)
                    .pistonBehavior(PistonBehavior.DESTROY));

    // ---- магические устройства ----------------------------------------

    public static final Block INFUSION_ALTAR = register("infusion_altar",
            InfusionAltarBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .requiresTool()
                    .strength(5.0f, 8.0f)
                    .nonOpaque()
                    .luminance(state -> 4));

    public static final Block MANA_FONT = register("mana_font",
            ManaFontBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.LAPIS_BLUE)
                    .requiresTool()
                    .strength(4.0f, 8.0f)
                    .nonOpaque()
                    .luminance(state -> 10)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK));

    public static final Block TELEPORT_ANCHOR = register("teleport_anchor",
            TeleportAnchorBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.PURPLE)
                    .requiresTool()
                    .strength(4.0f, 8.0f)
                    .nonOpaque()
                    .luminance(state -> 8)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK));

    public static final Block ARCANE_PEDESTAL = register("arcane_pedestal",
            ArcanePedestalBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .requiresTool()
                    .strength(3.0f, 6.0f)
                    .nonOpaque());

    public static final Block ARCANE_LAMP = register("arcane_lamp",
            ArcaneLampBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.PALE_PURPLE)
                    .strength(0.3f)
                    .luminance(ArcaneLampBlock::luminance)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK));

    public static final Block WARDED_STONE = register("warded_stone",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.DEEPSLATE_GRAY)
                    .requiresTool()
                    .strength(25.0f, 600.0f)
                    .sounds(BlockSoundGroup.DEEPSLATE_BRICKS));

    // ---- рунные камни по школам ---------------------------------------

    public static final Map<SpellSchool, Block> RUNESTONES = new EnumMap<>(SpellSchool.class);

    static {
        for (SpellSchool school : SpellSchool.values()) {
            RUNESTONES.put(school, register("runestone_" + school.asString(),
                    settings -> new RunestoneBlock(school, settings),
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.STONE_GRAY)
                            .requiresTool()
                            .strength(3.0f, 9.0f)
                            .luminance(state -> 3)
                            .sounds(BlockSoundGroup.DEEPSLATE_TILES)));
        }
    }

    // ---- растение ------------------------------------------------------

    public static final Block MOONPETAL = register("moonpetal",
            settings -> new FlowerBlock(ModEffects.MANA_SURGE, 8.0f, settings),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.PALE_PURPLE)
                    .noCollision()
                    .breakInstantly()
                    .luminance(state -> 4)
                    .sounds(BlockSoundGroup.GRASS)
                    .pistonBehavior(PistonBehavior.DESTROY)
                    .offset(AbstractBlock.OffsetType.XZ));

    // ---- служебное -----------------------------------------------------

    private static Block register(String name, Function<AbstractBlock.Settings, Block> factory,
                                  AbstractBlock.Settings settings) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Arcanum.id(name));
        Block block = factory.apply(settings.registryKey(blockKey));
        Registry.register(Registries.BLOCK, blockKey, block);

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Arcanum.id(name));
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
