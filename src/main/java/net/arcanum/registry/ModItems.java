package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.arcanum.item.ArcaneRobeItem;
import net.arcanum.item.AugmentItem;
import net.arcanum.item.FallenGrimoireItem;
import net.arcanum.item.FocusItem;
import net.arcanum.item.ManaCrystalItem;
import net.arcanum.item.ManaGemItem;
import net.arcanum.item.ManaPotionItem;
import net.arcanum.item.ScrollItem;
import net.arcanum.item.SpellbookItem;
import net.arcanum.item.WandItem;
import net.arcanum.spell.Augment;
import net.arcanum.spell.SpellSchool;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Все предметы мода. */
public final class ModItems {
    private ModItems() {
    }

    private static final List<Item> ORDERED = new ArrayList<>();

    // ---- материалы -----------------------------------------------------

    public static final Item ARCANE_CRYSTAL = register("arcane_crystal", Item::new,
            new Item.Settings());

    public static final Item ARCANE_DUST = register("arcane_dust", Item::new,
            new Item.Settings());

    public static final Item INFUSED_CRYSTAL = register("infused_crystal", Item::new,
            new Item.Settings().rarity(Rarity.UNCOMMON));

    public static final Item SOUL_SHARD = register("soul_shard", Item::new,
            new Item.Settings().rarity(Rarity.UNCOMMON));

    public static final Item PHOENIX_FEATHER = register("phoenix_feather", Item::new,
            new Item.Settings().rarity(Rarity.RARE));

    public static final Item LUNAR_ESSENCE = register("lunar_essence", Item::new,
            new Item.Settings().rarity(Rarity.UNCOMMON));

    public static final Item BLANK_SCROLL = register("blank_scroll", Item::new,
            new Item.Settings());

    // ---- руны школ -----------------------------------------------------

    public static final Map<SpellSchool, Item> RUNES = new EnumMap<>(SpellSchool.class);
    public static final Map<SpellSchool, Item> FOCUSES = new EnumMap<>(SpellSchool.class);

    static {
        for (SpellSchool school : SpellSchool.values()) {
            RUNES.put(school, register("rune_" + school.asString(), Item::new,
                    new Item.Settings()));
        }
        for (SpellSchool school : SpellSchool.values()) {
            FOCUSES.put(school, register("focus_" + school.asString(),
                    settings -> new FocusItem(school, 0.30f, settings),
                    new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
        }
    }

    // ---- руны-модификаторы ----------------------------------------------

    public static final Map<Augment, Item> AUGMENTS = new EnumMap<>(Augment.class);

    static {
        for (Augment augment : Augment.values()) {
            AUGMENTS.put(augment, register(augment.itemName(),
                    settings -> new AugmentItem(augment, settings),
                    new Item.Settings().maxCount(8).rarity(Rarity.UNCOMMON)));
        }
    }

    // ---- запас маны ----------------------------------------------------

    public static final Item DIM_MANA_CRYSTAL = register("dim_mana_crystal", Item::new,
            new Item.Settings());

    public static final Item CHARGED_MANA_CRYSTAL = register("charged_mana_crystal",
            settings -> new ManaCrystalItem(300, settings),
            new Item.Settings()
                    .maxCount(16)
                    .rarity(Rarity.UNCOMMON)
                    .component(ModComponents.STORED_MANA, 300));

    public static final Item MANA_POTION = register("mana_potion",
            settings -> new ManaPotionItem(60, settings),
            new Item.Settings().maxCount(16));

    public static final Item GREATER_MANA_POTION = register("greater_mana_potion",
            settings -> new ManaPotionItem(160, settings),
            new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON));

    public static final Item MANA_GEM = register("mana_gem", ManaGemItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.RARE));

    // ---- посохи --------------------------------------------------------

    public static final Item APPRENTICE_WAND = register("apprentice_wand",
            settings -> new WandItem(1, 0, 1.00f, 1.00f, 0.00f, settings),
            new Item.Settings().maxCount(1));

    public static final Item ADEPT_STAFF = register("adept_staff",
            settings -> new WandItem(2, 25, 0.92f, 0.92f, 0.10f, settings),
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    public static final Item ARCHMAGE_STAFF = register("archmage_staff",
            settings -> new WandItem(3, 60, 0.84f, 0.84f, 0.25f, settings),
            new Item.Settings().maxCount(1).rarity(Rarity.RARE));

    public static final Item ELDRITCH_SCEPTER = register("eldritch_scepter",
            settings -> new WandItem(4, 120, 0.72f, 0.70f, 0.45f, settings),
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    // ---- книга и свитки -------------------------------------------------

    public static final Item SPELLBOOK = register("spellbook", SpellbookItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    public static final Item SPELL_SCROLL = register("spell_scroll", ScrollItem::new,
            new Item.Settings().maxCount(16));

    public static final Item FALLEN_GRIMOIRE = register("fallen_grimoire", FallenGrimoireItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    // ---- мантия чародея -------------------------------------------------

    public static final Item ARCANE_HOOD = register("arcane_hood",
            settings -> new ArcaneRobeItem(20, 0.04f, 0.98f, settings),
            new Item.Settings().armor(ModArmorMaterials.ARCANE_ROBE, EquipmentType.HELMET));

    public static final Item ARCANE_ROBE = register("arcane_robe",
            settings -> new ArcaneRobeItem(40, 0.08f, 0.94f, settings),
            new Item.Settings().armor(ModArmorMaterials.ARCANE_ROBE, EquipmentType.CHESTPLATE));

    public static final Item ARCANE_LEGGINGS = register("arcane_leggings",
            settings -> new ArcaneRobeItem(30, 0.06f, 0.96f, settings),
            new Item.Settings().armor(ModArmorMaterials.ARCANE_ROBE, EquipmentType.LEGGINGS));

    public static final Item ARCANE_BOOTS = register("arcane_boots",
            settings -> new ArcaneRobeItem(20, 0.04f, 0.98f, settings),
            new Item.Settings().armor(ModArmorMaterials.ARCANE_ROBE, EquipmentType.BOOTS));

    // ---- служебное ------------------------------------------------------

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Arcanum.id(name));
        Item item = Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
        ORDERED.add(item);
        return item;
    }

    public static List<Item> ordered() {
        return List.copyOf(ORDERED);
    }

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
