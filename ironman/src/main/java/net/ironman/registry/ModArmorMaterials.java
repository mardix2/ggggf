package net.ironman.registry;

import net.ironman.IronMan;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Материалы брони трёх моделей костюма.
 *
 * <p>Внешний вид каждого костюма описан файлом
 * {@code assets/ironman/equipment/<модель>.json}.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {
    }

    public static final RegistryKey<EquipmentAsset> MARK_I_ASSET = asset("mark_i");
    public static final RegistryKey<EquipmentAsset> MARK_III_ASSET = asset("mark_iii");
    public static final RegistryKey<EquipmentAsset> MARK_85_ASSET = asset("mark_85");

    /** Mark I: толстое железо, заметно крепче железной брони. */
    public static final ArmorMaterial MARK_I = new ArmorMaterial(
            25,
            Map.of(
                    EquipmentType.BOOTS, 2,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.CHESTPLATE, 7,
                    EquipmentType.HELMET, 3
            ),
            9,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            1.0f,
            0.1f,
            ModTags.REPAIRS_MARK_I,
            MARK_I_ASSET
    );

    /** Mark III: на уровне алмаза, но с отличной прочностью. */
    public static final ArmorMaterial MARK_III = new ArmorMaterial(
            35,
            Map.of(
                    EquipmentType.BOOTS, 3,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.HELMET, 3
            ),
            15,
            SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
            2.5f,
            0.1f,
            ModTags.REPAIRS_MARK_III,
            MARK_III_ASSET
    );

    /** Mark 85: лучше незерита. */
    public static final ArmorMaterial MARK_85 = new ArmorMaterial(
            45,
            Map.of(
                    EquipmentType.BOOTS, 4,
                    EquipmentType.LEGGINGS, 7,
                    EquipmentType.CHESTPLATE, 9,
                    EquipmentType.HELMET, 4
            ),
            18,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            4.0f,
            0.2f,
            ModTags.REPAIRS_MARK_85,
            MARK_85_ASSET
    );

    private static RegistryKey<EquipmentAsset> asset(String name) {
        return RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, IronMan.id(name));
    }
}
