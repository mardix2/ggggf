package net.arcanum.item;

import net.arcanum.spell.Augment;
import net.minecraft.item.Item;

/**
 * Руна-модификатор. Сам по себе предмет ничего не делает — его вплавляют
 * в посох на алтаре превращения.
 */
public class AugmentItem extends Item {

    private final Augment augment;

    public AugmentItem(Augment augment, Settings settings) {
        super(settings);
        this.augment = augment;
    }

    public Augment augment() {
        return augment;
    }
}
