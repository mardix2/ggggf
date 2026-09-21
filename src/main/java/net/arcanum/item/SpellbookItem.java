package net.arcanum.item;

import net.arcanum.mana.ManaData;
import net.arcanum.mana.ManaManager;
import net.arcanum.net.ModNetworking;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

/**
 * Книга заклинаний: открывает список изученного, а в приседе просто
 * переключает активное заклинание на следующее — удобно, когда не хочется
 * лезть в интерфейс.
 */
public class SpellbookItem extends Item {

    public SpellbookItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        if (player.isSneaking()) {
            return cycleSpell(player);
        }
        ModNetworking.openSpellbook(player);
        return ActionResult.SUCCESS;
    }

    /** Переключает активное заклинание на следующее изученное. */
    private ActionResult cycleSpell(ServerPlayerEntity player) {
        ManaData data = ManaManager.data(player);
        List<Identifier> known = data.known();
        if (known.isEmpty()) {
            player.sendMessage(Text.translatable("message.arcanum.no_spells_known")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        int index = data.selected().map(known::indexOf).orElse(-1);
        Identifier next = known.get((index + 1 + known.size()) % known.size());
        ManaManager.set(player, data.withSelected(next));

        Spell spell = SpellRegistry.get(next);
        player.sendMessage(spell != null
                ? Text.translatable("message.arcanum.spell_selected", spell.displayName())
                : Text.literal(next.toString()), true);
        return ActionResult.SUCCESS;
    }
}
