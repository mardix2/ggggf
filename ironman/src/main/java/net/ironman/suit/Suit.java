package net.ironman.suit;

import net.ironman.item.IronManArmorItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/** Проверки «что надето на игроке». Работают и на сервере, и на клиенте. */
public final class Suit {
    private Suit() {
    }

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * Модель полного костюма или {@code null}, если костюм неполный.
     * Детали разных моделей не стыкуются между собой — так задумано.
     */
    public static SuitTier tierOf(LivingEntity entity) {
        SuitTier tier = null;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (!(stack.getItem() instanceof IronManArmorItem armor)) {
                return null;
            }
            if (tier == null) {
                tier = armor.tier();
            } else if (tier != armor.tier()) {
                return null;
            }
        }
        return tier;
    }

    /** Модель шлема (HUD работает от одного шлема) или {@code null}. */
    public static SuitTier helmetTier(LivingEntity entity) {
        ItemStack stack = entity.getEquippedStack(EquipmentSlot.HEAD);
        return stack.getItem() instanceof IronManArmorItem armor ? armor.tier() : null;
    }

    /** Сколько частей костюма надето (для подсказки в HUD). */
    public static int pieces(LivingEntity entity) {
        int count = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (entity.getEquippedStack(slot).getItem() instanceof IronManArmorItem) {
                count++;
            }
        }
        return count;
    }
}
