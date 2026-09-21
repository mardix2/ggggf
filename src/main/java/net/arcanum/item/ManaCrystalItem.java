package net.arcanum.item;

import net.arcanum.mana.ManaManager;
import net.arcanum.registry.ModComponents;
import net.arcanum.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Заряженный кристалл маны — переносной запас.
 *
 * <p>Отдаёт ровно столько, сколько не хватает игроку, поэтому им нельзя
 * «перелить» сверх максимума. Опустевший кристалл превращается в тусклый
 * и заряжается обратно на алтаре превращения.
 */
public class ManaCrystalItem extends Item {

    private final int capacity;

    public ManaCrystalItem(int capacity, Settings settings) {
        super(settings);
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public static int stored(ItemStack stack) {
        Integer value = stack.get(ModComponents.STORED_MANA);
        return value == null ? 0 : value;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        int stored = stored(stack);
        int missing = ManaManager.max(user) - (int) ManaManager.current(user);
        if (stored <= 0 || missing <= 0) {
            return ActionResult.FAIL;
        }

        int transferred = Math.min(stored, missing);
        ManaManager.give(user, transferred);
        int left = stored - transferred;

        if (left > 0) {
            stack.set(ModComponents.STORED_MANA, left);
        } else if (!user.isCreative()) {
            // Кристалл выгорел — отдаём тусклый.
            stack.decrement(1);
            user.giveItemStack(new ItemStack(ModItems.DIM_MANA_CRYSTAL));
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 0.8f, 1.4f);
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return stored(stack) < capacity;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0f * stored(stack) / capacity);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x7FD8FF;
    }
}
