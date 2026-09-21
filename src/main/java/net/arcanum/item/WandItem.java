package net.arcanum.item;

import net.arcanum.mana.ArcaneGear;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCasting;
import net.arcanum.spell.SpellSchool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Посох — основной инструмент заклинателя.
 *
 * <p>Прочности у посохов нет намеренно: платой за магию служит мана,
 * а не расходник. Ступень посоха ограничивает доступные заклинания
 * и задаёт бонусы к стоимости, силе и перезарядке.
 */
public class WandItem extends Item implements ArcaneGear {

    private final int tier;
    private final int manaBonus;
    private final float costMultiplier;
    private final float cooldownMultiplier;
    private final float powerBonus;

    public WandItem(int tier, int manaBonus, float costMultiplier, float cooldownMultiplier,
                    float powerBonus, Settings settings) {
        super(settings);
        this.tier = tier;
        this.manaBonus = manaBonus;
        this.costMultiplier = costMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
        this.powerBonus = powerBonus;
    }

    public int tier() {
        return tier;
    }

    @Override
    public int manaCapacityBonus(ItemStack stack) {
        return manaBonus;
    }

    @Override
    public float manaCostMultiplier(ItemStack stack) {
        return costMultiplier;
    }

    @Override
    public float cooldownMultiplier(ItemStack stack) {
        return cooldownMultiplier;
    }

    @Override
    public float powerBonus(ItemStack stack, SpellSchool school) {
        return powerBonus;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        // Вся логика — на сервере; клиент лишь не мешает анимации руки.
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        Spell spell = SpellCasting.selected(player);
        SpellCasting.Result result = SpellCasting.cast(player, stack, spell, tier);
        if (result.ok()) {
            return ActionResult.SUCCESS;
        }
        player.sendMessage(SpellCasting.failureMessage(player, spell, result), true);
        return ActionResult.FAIL;
    }
}
