package net.ironman.registry;

import net.ironman.IronMan;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/** Теги мода. Файлы лежат в {@code data/ironman/tags/item}. */
public final class ModTags {
    private ModTags() {
    }

    /** Чем чинится Mark I (железо). */
    public static final TagKey<Item> REPAIRS_MARK_I = of("repairs_mark_i");
    /** Чем чинится Mark III (золото-титановый сплав). */
    public static final TagKey<Item> REPAIRS_MARK_III = of("repairs_mark_iii");
    /** Чем чинится Mark 85 (нанотехнологическое ядро). */
    public static final TagKey<Item> REPAIRS_MARK_85 = of("repairs_mark_85");

    private static TagKey<Item> of(String path) {
        return TagKey.of(RegistryKeys.ITEM, IronMan.id(path));
    }
}
