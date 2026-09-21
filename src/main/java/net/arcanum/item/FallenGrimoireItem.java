package net.arcanum.item;

import net.arcanum.mana.ManaData;
import net.arcanum.mana.ManaManager;
import net.arcanum.net.ModNetworking;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Гримуар павшего мага — награда из башни.
 *
 * <p>Учит одному случайному заклинанию из тех, что игрок ещё не знает.
 * Если знать уже нечего, книга отдаёт остатки своей силы маной.
 */
public class FallenGrimoireItem extends Item {

    private static final float LEFTOVER_MANA = 200.0f;

    public FallenGrimoireItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        ManaData data = ManaManager.data(player);
        List<Spell> unknown = new ArrayList<>();
        for (Spell spell : SpellRegistry.all()) {
            if (!data.knows(spell.id())) {
                unknown.add(spell);
            }
        }

        if (unknown.isEmpty()) {
            ManaManager.give(player, LEFTOVER_MANA);
            player.sendMessage(Text.translatable("message.arcanum.grimoire_spent")
                    .formatted(Formatting.GRAY), true);
        } else {
            Spell learned = unknown.get(world.getRandom().nextInt(unknown.size()));
            ManaManager.set(player, data.learn(learned.id()));
            ModNetworking.syncSpells(player);
            player.sendMessage(Text.translatable("message.arcanum.spell_learned",
                    learned.displayName()).formatted(Formatting.LIGHT_PURPLE), false);
        }

        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getEyeY(), player.getZ(), 40, 0.6, 0.6, 0.6, 0.5);
        }
        stack.decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS;
    }
}
