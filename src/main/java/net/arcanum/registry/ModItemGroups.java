package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.arcanum.item.ScrollItem;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/** Творческая вкладка мода. */
public final class ModItemGroups {
    private ModItemGroups() {
    }

    public static final RegistryKey<ItemGroup> ARCANUM =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Arcanum.id("general"));

    public static void init() {
        Registry.register(Registries.ITEM_GROUP, ARCANUM, FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.ARCHMAGE_STAFF))
                .displayName(Text.translatable("itemgroup.arcanum.general"))
                .build());

        ItemGroupEvents.modifyEntriesEvent(ARCANUM).register(entries -> {
            ModItems.ordered().forEach(entries::add);
            ModBlocks.ordered().forEach(entries::add);
            // Готовый свиток на каждое заклинание — удобно и для теста, и в творческом.
            for (Spell spell : SpellRegistry.all()) {
                entries.add(ScrollItem.of(ModItems.SPELL_SCROLL, spell));
            }
        });
    }
}
