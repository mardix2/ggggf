package net.ironman.registry;

import net.ironman.IronMan;
import net.ironman.item.ArcReactorItem;
import net.ironman.item.IronManArmorItem;
import net.ironman.item.PowerCellItem;
import net.ironman.suit.ReactorTier;
import net.ironman.suit.SuitTier;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterial;
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

    public static final Item RAW_PALLADIUM = register("raw_palladium", Item::new,
            new Item.Settings());

    public static final Item PALLADIUM_INGOT = register("palladium_ingot", Item::new,
            new Item.Settings());

    public static final Item GOLD_TITANIUM_ALLOY = register("gold_titanium_alloy", Item::new,
            new Item.Settings());

    /** Новый элемент, синтезированный Старком вместо палладия. */
    public static final Item STARKIUM = register("starkium", Item::new,
            new Item.Settings().rarity(Rarity.RARE));

    public static final Item NANOTECH_CORE = register("nanotech_core", Item::new,
            new Item.Settings().rarity(Rarity.EPIC).fireproof());

    // ---- компоненты костюма -------------------------------------------

    public static final Item REPULSOR_NODE = register("repulsor_node", Item::new,
            new Item.Settings());

    public static final Item JET_THRUSTER = register("jet_thruster", Item::new,
            new Item.Settings());

    public static final Item MICRO_MISSILE = register("micro_missile", Item::new,
            new Item.Settings().maxCount(64));

    public static final Item POWER_CELL = register("power_cell",
            settings -> new PowerCellItem(800, settings),
            new Item.Settings().maxCount(16));

    // ---- дуговые реакторы ---------------------------------------------

    public static final Item ARC_REACTOR_MK1 = register("arc_reactor_mk1",
            settings -> new ArcReactorItem(ReactorTier.MK_I, settings),
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    public static final Item ARC_REACTOR_MK2 = register("arc_reactor_mk2",
            settings -> new ArcReactorItem(ReactorTier.MK_II, settings),
            new Item.Settings().maxCount(1).rarity(Rarity.RARE));

    public static final Item STARKIUM_ARC_REACTOR = register("starkium_arc_reactor",
            settings -> new ArcReactorItem(ReactorTier.STARKIUM, settings),
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    // ---- костюмы -------------------------------------------------------

    /** Части костюма: модель → слот → предмет. */
    public static final Map<SuitTier, Map<EquipmentType, Item>> SUITS = new EnumMap<>(SuitTier.class);

    static {
        suit(SuitTier.MARK_I, ModArmorMaterials.MARK_I, Rarity.UNCOMMON, false);
        suit(SuitTier.MARK_III, ModArmorMaterials.MARK_III, Rarity.RARE, false);
        suit(SuitTier.MARK_85, ModArmorMaterials.MARK_85, Rarity.EPIC, true);
    }

    private static void suit(SuitTier tier, ArmorMaterial material, Rarity rarity, boolean fireproof) {
        Map<EquipmentType, Item> parts = new EnumMap<>(EquipmentType.class);
        for (EquipmentType type : List.of(EquipmentType.HELMET, EquipmentType.CHESTPLATE,
                EquipmentType.LEGGINGS, EquipmentType.BOOTS)) {
            Item.Settings settings = new Item.Settings().armor(material, type).rarity(rarity);
            if (fireproof) {
                settings = settings.fireproof();
            }
            parts.put(type, register(tier.asString() + "_" + type.getName(),
                    s -> new IronManArmorItem(tier, type, s), settings));
        }
        SUITS.put(tier, parts);
    }

    public static Item suitPart(SuitTier tier, EquipmentType type) {
        return SUITS.get(tier).get(type);
    }

    public static Item reactorItem(ReactorTier tier) {
        return switch (tier) {
            case MK_II -> ARC_REACTOR_MK2;
            case STARKIUM -> STARKIUM_ARC_REACTOR;
            default -> ARC_REACTOR_MK1;
        };
    }

    // ---- служебное ------------------------------------------------------

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, IronMan.id(name));
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
