package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Материалы брони.
 *
 * <p>Единственное место, где мод касается системы снаряжения ванили.
 * Текстура слоя описывается файлом {@code assets/arcanum/equipment/arcane_robe.json}.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {
    }

    public static final RegistryKey<EquipmentAsset> ARCANE_ROBE_ASSET =
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Arcanum.id("arcane_robe"));

    /**
     * Мантия чародея: защита чуть выше кожи, зато огромный запас маны.
     * Броня мага — не про то, чтобы держать удар.
     */
    public static final ArmorMaterial ARCANE_ROBE = new ArmorMaterial(
            14,
            Map.of(
                    EquipmentType.BOOTS, 1,
                    EquipmentType.LEGGINGS, 3,
                    EquipmentType.CHESTPLATE, 4,
                    EquipmentType.HELMET, 2
            ),
            22,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            0.0f,
            0.0f,
            ModTags.Items.ROBE_REPAIR,
            ARCANE_ROBE_ASSET
    );
}
