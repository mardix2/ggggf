package net.arcanum.item;

import net.arcanum.mana.ManaManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Зелье маны. Действует мгновенно, без анимации питья — так его можно
 * использовать в бою, не теряя контроль над персонажем.
 */
public class ManaPotionItem extends Item {

    private final int restore;

    public ManaPotionItem(int restore, Settings settings) {
        super(settings);
        this.restore = restore;
    }

    public int restore() {
        return restore;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (ManaManager.current(user) >= ManaManager.max(user)) {
            return ActionResult.FAIL;
        }

        ManaManager.give(user, restore);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK,
                SoundCategory.PLAYERS, 0.8f, 1.3f);
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.INSTANT_EFFECT,
                    user.getX(), user.getY() + 1.0, user.getZ(), 12, 0.3, 0.5, 0.3, 0.0);
        }

        if (!user.isCreative()) {
            stack.decrement(1);
            user.giveItemStack(new ItemStack(Items.GLASS_BOTTLE));
        }
        return ActionResult.SUCCESS;
    }
}
