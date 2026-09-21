package net.arcanum.item;

import net.arcanum.ArcanumConfig;
import net.arcanum.mana.ManaData;
import net.arcanum.mana.ManaManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Печать разума: навсегда поднимает максимум маны.
 * Больше {@link ArcanumConfig#MAX_ATTUNEMENT} печатей разум не выдерживает.
 */
public class ManaGemItem extends Item {

    public ManaGemItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        ManaData data = ManaManager.data(user);
        if (data.attunement() >= ArcanumConfig.MAX_ATTUNEMENT) {
            user.sendMessage(Text.translatable("message.arcanum.attunement_max")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        ManaManager.set(user, data.withAttunement(data.attunement() + 1));
        ManaManager.fill(user);
        user.sendMessage(Text.translatable("message.arcanum.attunement_up",
                ManaManager.max(user)).formatted(Formatting.AQUA), false);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS, 0.8f, 1.2f);

        stack.decrementUnlessCreative(1, user);
        return ActionResult.SUCCESS;
    }
}
