package net.arcanum.client;

import net.arcanum.item.ArcaneRobeItem;
import net.arcanum.item.FocusItem;
import net.arcanum.item.ManaCrystalItem;
import net.arcanum.item.ManaPotionItem;
import net.arcanum.item.ScrollItem;
import net.arcanum.item.WandItem;
import net.arcanum.mana.ArcaneGear;
import net.arcanum.registry.ModComponents;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellSchool;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Подсказки предметов.
 *
 * <p>Собраны в одном месте вместо переопределения {@code appendTooltip}
 * в каждом классе: так проще держать формат единым, и меньше кода зависит
 * от сигнатур ванильных методов.
 */
public final class ArcanumTooltips {
    private ArcanumTooltips() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.getItem() instanceof WandItem wand) {
                wandTooltip(stack, wand, lines);
            } else if (stack.getItem() instanceof FocusItem focus) {
                focusTooltip(focus, lines);
            } else if (stack.getItem() instanceof ScrollItem) {
                scrollTooltip(stack, lines);
            } else if (stack.getItem() instanceof ManaCrystalItem crystal) {
                crystalTooltip(stack, crystal, lines);
            } else if (stack.getItem() instanceof ManaPotionItem potion) {
                lines.add(bonus("tooltip.arcanum.restores", potion.restore()));
            } else if (stack.getItem() instanceof ArcaneRobeItem robe) {
                robeTooltip(stack, robe, lines);
            }

            appendBoundSpells(stack, lines);
        });
    }

    private static void wandTooltip(ItemStack stack, WandItem wand, List<Text> lines) {
        lines.add(Text.translatable("tooltip.arcanum.tier", wand.tier()).formatted(Formatting.GOLD));
        int mana = wand.manaCapacityBonus(stack);
        if (mana > 0) {
            lines.add(bonus("tooltip.arcanum.max_mana", mana));
        }
        lines.add(percent("tooltip.arcanum.cost", wand.manaCostMultiplier(stack) - 1.0f, false));
        lines.add(percent("tooltip.arcanum.cooldown", wand.cooldownMultiplier(stack) - 1.0f, false));
        float power = wand.powerBonus(stack, SpellSchool.ARCANE);
        if (power > 0.0f) {
            lines.add(percent("tooltip.arcanum.power", power, true));
        }
    }

    private static void focusTooltip(FocusItem focus, List<Text> lines) {
        lines.add(Text.translatable("tooltip.arcanum.focus_school", focus.school().displayName())
                .formatted(Formatting.GRAY));
        lines.add(percent("tooltip.arcanum.power", focus.bonus(), true));
        lines.add(Text.translatable("tooltip.arcanum.offhand").formatted(Formatting.DARK_GRAY));
    }

    private static void scrollTooltip(ItemStack stack, List<Text> lines) {
        Spell spell = ScrollItem.spellOf(stack);
        if (spell == null) {
            lines.add(Text.translatable("tooltip.arcanum.blank_scroll").formatted(Formatting.DARK_GRAY));
            return;
        }
        lines.add(spell.displayName());
        lines.add(Text.translatable("tooltip.arcanum.scroll_cost", (int) (spell.manaCost() * 0.5f))
                .formatted(Formatting.AQUA));
        lines.add(Text.translatable("tooltip.arcanum.scroll_learn").formatted(Formatting.DARK_GRAY));
    }

    private static void crystalTooltip(ItemStack stack, ManaCrystalItem crystal, List<Text> lines) {
        lines.add(Text.translatable("tooltip.arcanum.stored",
                ManaCrystalItem.stored(stack), crystal.capacity()).formatted(Formatting.AQUA));
    }

    private static void robeTooltip(ItemStack stack, ArcaneGear gear, List<Text> lines) {
        lines.add(bonus("tooltip.arcanum.max_mana", gear.manaCapacityBonus(stack)));
        lines.add(Text.translatable("tooltip.arcanum.regen",
                        String.format("%.1f", gear.manaRegenBonus(stack) * 20.0f))
                .formatted(Formatting.AQUA));
        lines.add(percent("tooltip.arcanum.cost", gear.manaCostMultiplier(stack) - 1.0f, false));
    }

    /** Заклинания, впечатанные в предмет, показываются для любого предмета. */
    private static void appendBoundSpells(ItemStack stack, List<Text> lines) {
        List<Identifier> bound = stack.get(ModComponents.BOUND_SPELLS);
        if (bound == null || bound.isEmpty()) {
            return;
        }
        lines.add(Text.translatable("tooltip.arcanum.bound").formatted(Formatting.DARK_PURPLE));
        for (Identifier id : bound) {
            Spell spell = net.arcanum.spell.SpellRegistry.get(id);
            lines.add(Text.literal("  ").append(spell != null
                    ? spell.displayName()
                    : Text.literal(id.toString()).formatted(Formatting.DARK_GRAY)));
        }
    }

    private static Text bonus(String key, int value) {
        return Text.translatable(key, "+" + value).formatted(Formatting.AQUA);
    }

    /**
     * Форматирует долю как «-16 %» / «+30 %».
     *
     * @param higherIsBetter true для силы заклинаний, false для стоимости
     *                       и перезарядки — от этого зависит цвет строки
     */
    private static Text percent(String key, float fraction, boolean higherIsBetter) {
        int value = Math.round(fraction * 100.0f);
        String text = (value > 0 ? "+" : "") + value + " %";
        Formatting color;
        if (value == 0) {
            color = Formatting.GRAY;
        } else {
            color = (value > 0) == higherIsBetter ? Formatting.GREEN : Formatting.RED;
        }
        return Text.translatable(key, text).formatted(color);
    }
}
