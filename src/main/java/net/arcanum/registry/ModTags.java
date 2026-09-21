package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/** Теги мода. Сами файлы лежат в {@code data/arcanum/tags/...}. */
public final class ModTags {
    private ModTags() {
    }

    public static final class Items {
        private Items() {
        }

        /** Чем чинится мантия чародея. */
        public static final TagKey<Item> ROBE_REPAIR = of("robe_repair");
        /** Всё, что считается посохом (для рецептов и подсказок). */
        public static final TagKey<Item> WANDS = of("wands");
        /** Фокусы всех школ. */
        public static final TagKey<Item> FOCUSES = of("focuses");
        /** Руны всех школ. */
        public static final TagKey<Item> RUNES = of("runes");

        private static TagKey<Item> of(String path) {
            return TagKey.of(RegistryKeys.ITEM, Arcanum.id(path));
        }
    }

    public static final class Blocks {
        private Blocks() {
        }

        /** Рунные камни всех школ. */
        public static final TagKey<Block> RUNESTONES = of("runestones");

        private static TagKey<Block> of(String path) {
            return TagKey.of(RegistryKeys.BLOCK, Arcanum.id(path));
        }
    }
}
