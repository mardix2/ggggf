package net.ironman.item;

import net.ironman.suit.SuitTier;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.EquipmentType;

/**
 * Часть костюма. Сама по себе — просто броня; способности включаются,
 * только когда надеты все четыре части одной модели (см. {@link net.ironman.suit.Suit}).
 */
public class IronManArmorItem extends Item {

    private final SuitTier tier;
    private final EquipmentType type;

    public IronManArmorItem(SuitTier tier, EquipmentType type, Settings settings) {
        super(settings);
        this.tier = tier;
        this.type = type;
    }

    public SuitTier tier() {
        return tier;
    }

    public EquipmentType type() {
        return type;
    }
}
