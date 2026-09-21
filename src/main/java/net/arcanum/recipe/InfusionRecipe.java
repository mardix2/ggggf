package net.arcanum.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcanum.item.WandItem;
import net.arcanum.spell.Augment;
import net.arcanum.spell.SpellSchool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.StringIdentifiable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Рецепт алтаря превращения.
 *
 * <p>Это не ванильный {@code Recipe}: своя загрузка из
 * {@code data/arcanum/infusion/*.json} позволяет описать то, чего в ванильной
 * системе нет, — требование к числу постаментов и расход маны заклинателя.
 *
 * @param inputs       что нужно бросить на землю у алтаря
 * @param result        что получится
 * @param pedestals     сколько аркановых постаментов должно стоять рядом
 * @param mana          сколько маны спишется с игрока при запуске обряда
 * @param scrollSchool  если задано, результат превращается в свиток со
 *                      случайным заклинанием этой школы (так записываются свитки)
 * @param requiresWand  обряду нужен брошенный рядом посох; он не тратится,
 *                      а служит основой результата — так сохраняются уже
 *                      впечатанные в него руны
 * @param addAugment    какую руну-модификатор вплавить в посох
 * @param clearAugments вынуть из посоха все руны (сами руны при этом гибнут)
 */
public record InfusionRecipe(List<Entry> inputs, Entry result, int pedestals, int mana,
                             Optional<SpellSchool> scrollSchool, boolean requiresWand,
                             Optional<Augment> addAugment, boolean clearAugments) {

    /** Предмет и количество. */
    public record Entry(Item item, int count) {

        /** Пустой результат: его указывают обряды, которые преобразуют посох. */
        public static final Entry NOTHING = new Entry(Items.AIR, 1);

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Registries.ITEM.getCodec().fieldOf("item").forGetter(Entry::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Entry::count)
        ).apply(instance, Entry::new));

        public ItemStack toStack() {
            return new ItemStack(item, count);
        }
    }

    public static final Codec<InfusionRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf(1, 16).fieldOf("inputs").forGetter(InfusionRecipe::inputs),
            Entry.CODEC.optionalFieldOf("result", Entry.NOTHING).forGetter(InfusionRecipe::result),
            Codec.intRange(0, 8).optionalFieldOf("pedestals", 0).forGetter(InfusionRecipe::pedestals),
            Codec.intRange(0, 10000).optionalFieldOf("mana", 0).forGetter(InfusionRecipe::mana),
            StringIdentifiable.createCodec(SpellSchool::values)
                    .optionalFieldOf("scroll_school").forGetter(InfusionRecipe::scrollSchool),
            Codec.BOOL.optionalFieldOf("requires_wand", false).forGetter(InfusionRecipe::requiresWand),
            Augment.CODEC.optionalFieldOf("add_augment").forGetter(InfusionRecipe::addAugment),
            Codec.BOOL.optionalFieldOf("clear_augments", false).forGetter(InfusionRecipe::clearAugments)
    ).apply(instance, InfusionRecipe::new));

    /**
     * Подходит ли рецепт под то, что лежит у алтаря.
     *
     * @param available сколько каких предметов собрано
     * @param pedestals сколько постаментов стоит вокруг
     */
    public boolean matches(Map<Item, Integer> available, int pedestals) {
        if (pedestals < this.pedestals) {
            return false;
        }
        if (requiresWand && available.keySet().stream().noneMatch(item -> item instanceof WandItem)) {
            return false;
        }
        for (Entry entry : inputs) {
            if (available.getOrDefault(entry.item(), 0) < entry.count()) {
                return false;
            }
        }
        return true;
    }

    /** Сколько всего предметов нужно — по этому числу выбирается более «сложный» рецепт. */
    public int weight() {
        int total = 0;
        for (Entry entry : inputs) {
            total += entry.count();
        }
        // Обряды с посохом должны обходить простые наборы с теми же реагентами.
        return total + pedestals * 2 + (requiresWand ? 6 : 0);
    }
}
