package net.arcanum.mana;

import net.arcanum.spell.SpellSchool;
import net.minecraft.item.ItemStack;

/**
 * Реализуется предметами, которые влияют на магию игрока, пока надеты
 * или находятся в руках: мантии, фокусы, посохи.
 */
public interface ArcaneGear {

    /** Прибавка к максимуму маны. */
    default int manaCapacityBonus(ItemStack stack) {
        return 0;
    }

    /** Прибавка к регенерации маны, единиц за тик. */
    default float manaRegenBonus(ItemStack stack) {
        return 0.0f;
    }

    /** Множитель стоимости заклинаний; 1.0 — без изменений, 0.8 — на 20% дешевле. */
    default float manaCostMultiplier(ItemStack stack) {
        return 1.0f;
    }

    /** Прибавка к силе заклинаний выбранной школы (0.15 = +15% урона/эффекта). */
    default float powerBonus(ItemStack stack, SpellSchool school) {
        return 0.0f;
    }

    /** Множитель перезарядки; 1.0 — без изменений, 0.75 — на четверть быстрее. */
    default float cooldownMultiplier(ItemStack stack) {
        return 1.0f;
    }
}
