package net.arcanum.item;

import net.arcanum.mana.ArcaneGear;
import net.arcanum.spell.SpellSchool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Часть мантии чародея: прибавка к запасу маны, восстановлению
 * и небольшая скидка на заклинания. Полный комплект даёт заметный эффект.
 */
public class ArcaneRobeItem extends Item implements ArcaneGear {

    private final int manaBonus;
    private final float regenBonus;
    private final float costMultiplier;

    public ArcaneRobeItem(int manaBonus, float regenBonus, float costMultiplier, Settings settings) {
        super(settings);
        this.manaBonus = manaBonus;
        this.regenBonus = regenBonus;
        this.costMultiplier = costMultiplier;
    }

    @Override
    public int manaCapacityBonus(ItemStack stack) {
        return manaBonus;
    }

    @Override
    public float manaRegenBonus(ItemStack stack) {
        return regenBonus;
    }

    @Override
    public float manaCostMultiplier(ItemStack stack) {
        return costMultiplier;
    }

    @Override
    public float powerBonus(ItemStack stack, SpellSchool school) {
        return 0.0f;
    }
}
