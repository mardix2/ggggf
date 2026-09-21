package net.arcanum.spell;

import net.arcanum.item.WandItem;
import net.arcanum.registry.ModComponents;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Работа с рунами-модификаторами, впечатанными в посох. */
public final class Augments {
    private Augments() {
    }

    public static List<Augment> of(ItemStack stack) {
        List<Augment> list = stack.get(ModComponents.AUGMENTS);
        return list == null ? List.of() : list;
    }

    /** Сколько рун вмещает посох: по одной на ступень. */
    public static int slots(ItemStack stack) {
        return stack.getItem() instanceof WandItem wand ? wand.tier() : 0;
    }

    public static boolean hasFreeSlot(ItemStack stack) {
        return of(stack).size() < slots(stack);
    }

    /** Копия посоха с добавленной руной. */
    public static ItemStack with(ItemStack wand, Augment augment) {
        ItemStack copy = wand.copy();
        copy.setCount(1);
        List<Augment> next = new ArrayList<>(of(copy));
        next.add(augment);
        copy.set(ModComponents.AUGMENTS, List.copyOf(next));
        return copy;
    }

    /** Копия посоха без рун: сами руны при этом теряются. */
    public static ItemStack cleared(ItemStack wand) {
        ItemStack copy = wand.copy();
        copy.setCount(1);
        copy.remove(ModComponents.AUGMENTS);
        return copy;
    }

    // Бонусы складываются, штрафы тоже — две руны силы дают +40 % и +50 % к цене.

    public static float powerBonus(List<Augment> augments) {
        float total = 0.0f;
        for (Augment augment : augments) {
            total += augment.power();
        }
        return total;
    }

    public static float costMultiplier(List<Augment> augments) {
        float total = 1.0f;
        for (Augment augment : augments) {
            total += augment.cost();
        }
        return Math.max(0.25f, total);
    }

    public static float cooldownMultiplier(List<Augment> augments) {
        float total = 1.0f;
        for (Augment augment : augments) {
            total += augment.cooldown();
        }
        return Math.max(0.2f, total);
    }

    public static float rangeMultiplier(List<Augment> augments) {
        float total = 1.0f;
        for (Augment augment : augments) {
            total += augment.range();
        }
        return total;
    }

    public static int extraShots(List<Augment> augments) {
        int total = 0;
        for (Augment augment : augments) {
            total += augment.extraShots();
        }
        return total;
    }

    public static boolean hasEcho(List<Augment> augments) {
        return augments.contains(Augment.ECHO);
    }
}
