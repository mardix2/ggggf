package net.arcanum.item;

import net.arcanum.mana.ArcaneGear;
import net.arcanum.spell.SpellSchool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Фокус школы — носится во второй руке и усиливает заклинания своей школы,
 * слегка удешевляя их.
 */
public class FocusItem extends Item implements ArcaneGear {

    private final SpellSchool school;
    private final float bonus;

    public FocusItem(SpellSchool school, float bonus, Settings settings) {
        super(settings);
        this.school = school;
        this.bonus = bonus;
    }

    public SpellSchool school() {
        return school;
    }

    public float bonus() {
        return bonus;
    }

    @Override
    public float powerBonus(ItemStack stack, SpellSchool target) {
        return target == school ? bonus : 0.0f;
    }

    @Override
    public float manaCostMultiplier(ItemStack stack) {
        return 0.95f;
    }
}
