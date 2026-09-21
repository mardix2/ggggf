package net.arcanum.item;

import net.arcanum.mana.ManaData;
import net.arcanum.mana.ManaManager;
import net.arcanum.registry.ModComponents;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellCasting;
import net.arcanum.spell.SpellRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Свиток заклинания.
 *
 * <p>Обычный щелчок — разовое применение за половину стоимости и без
 * требований к ступени посоха. Щелчок в приседе — изучение заклинания
 * навсегда; свиток при этом расходуется.
 */
public class ScrollItem extends Item {

    public ScrollItem(Settings settings) {
        super(settings);
    }

    /** Заклинание, записанное в свиток, либо {@code null} для пустого свитка. */
    public static Spell spellOf(ItemStack stack) {
        Identifier id = stack.get(ModComponents.SCROLL_SPELL);
        return id == null ? null : SpellRegistry.get(id);
    }

    public static ItemStack of(Item scrollItem, Spell spell) {
        ItemStack stack = new ItemStack(scrollItem);
        stack.set(ModComponents.SCROLL_SPELL, spell.id());
        return stack;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        Spell spell = spellOf(stack);
        if (spell == null) {
            player.sendMessage(Text.translatable("message.arcanum.scroll_blank")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        if (player.isSneaking()) {
            return learn(player, stack, spell);
        }

        SpellCasting.Result result = SpellCasting.castFromScroll(player, stack, spell);
        if (!result.ok()) {
            player.sendMessage(SpellCasting.failureMessage(player, stack, spell, result), true);
            return ActionResult.FAIL;
        }
        stack.decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS;
    }

    private ActionResult learn(ServerPlayerEntity player, ItemStack stack, Spell spell) {
        ManaData data = ManaManager.data(player);
        if (data.knows(spell.id())) {
            player.sendMessage(Text.translatable("message.arcanum.already_known", spell.displayName())
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }
        ManaManager.set(player, data.learn(spell.id()));
        player.sendMessage(Text.translatable("message.arcanum.spell_learned", spell.displayName())
                .formatted(Formatting.LIGHT_PURPLE), false);
        stack.decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS;
    }
}
