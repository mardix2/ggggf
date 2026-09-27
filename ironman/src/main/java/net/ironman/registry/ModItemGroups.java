package net.ironman.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.ironman.IronMan;
import net.ironman.suit.SuitTier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/** Творческая вкладка «Stark Industries». */
public final class ModItemGroups {
    private ModItemGroups() {
    }

    public static final RegistryKey<ItemGroup> STARK =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, IronMan.id("stark_industries"));

    public static void init() {
        Registry.register(Registries.ITEM_GROUP, STARK, FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.suitPart(SuitTier.MARK_III, EquipmentType.HELMET)))
                .displayName(Text.translatable("itemgroup.ironman.stark_industries"))
                .build());

        ItemGroupEvents.modifyEntriesEvent(STARK).register(entries -> {
            ModItems.ordered().forEach(entries::add);
            ModBlocks.ordered().forEach(entries::add);
        });
    }
}
