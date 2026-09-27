package net.ironman.item;

import net.ironman.registry.ModItems;
import net.ironman.suit.ReactorData;
import net.ironman.suit.ReactorTier;
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

/**
 * Дуговой реактор. ПКМ — установить его себе в грудь.
 *
 * <p>Старый реактор при замене возвращается в инвентарь, а заряд переносится
 * (в пределах ёмкости нового).
 */
public class ArcReactorItem extends Item {

    private final ReactorTier tier;

    public ArcReactorItem(ReactorTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
    }

    public ReactorTier tier() {
        return tier;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        ReactorData data = SuitManager.reactor(player);
        if (data.reactor() == tier) {
            player.sendMessage(Text.translatable("message.ironman.reactor_same")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        ItemStack stack = player.getStackInHand(hand);
        ReactorTier old = data.reactor();
        // Новый реактор приходит заряженным хотя бы наполовину — иначе
        // первые минуты после установки костюм был бы бесполезен.
        float energy = Math.max(data.energy(), tier.capacity() * 0.5f);
        SuitManager.setReactor(player, new ReactorData(tier, 0.0f).withEnergy(energy));
        stack.decrementUnlessCreative(1, player);

        if (old != ReactorTier.NONE && !player.isCreative()) {
            ItemStack back = new ItemStack(ModItems.reactorItem(old));
            if (!player.giveItemStack(back)) {
                player.dropItem(back, false);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.6f);
        player.sendMessage(Text.translatable("message.ironman.reactor_installed",
                Text.translatable(tier.translationKey()).formatted(tier.color())), true);
        return ActionResult.SUCCESS;
    }
}
