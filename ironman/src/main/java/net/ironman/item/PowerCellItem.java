package net.ironman.item;

import net.ironman.suit.ReactorData;
import net.ironman.suit.SuitManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Палладиевая батарея: мгновенно доливает энергию в реактор. */
public class PowerCellItem extends Item {

    private final int energy;

    public PowerCellItem(int energy, Settings settings) {
        super(settings);
        this.energy = energy;
    }

    public int energy() {
        return energy;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        ReactorData data = SuitManager.reactor(player);
        if (!data.installed()) {
            player.sendMessage(Text.translatable("message.ironman.no_reactor")
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        if (data.energy() >= data.capacity()) {
            player.sendMessage(Text.translatable("message.ironman.reactor_full")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        SuitManager.setReactor(player, data.withEnergy(data.energy() + energy));
        ItemStack stack = player.getStackInHand(hand);
        stack.decrementUnlessCreative(1, player);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.4f);
        return ActionResult.SUCCESS;
    }
}
