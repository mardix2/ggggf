package net.arcanum.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcanum.ArcanumConfig;
import net.arcanum.mana.ManaData;
import net.arcanum.mana.ManaManager;
import net.arcanum.net.ModNetworking;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellRegistry;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.SummonManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Команды мода.
 *
 * <p>{@code /arcanum spells} доступна всем — это справочник. Всё, что меняет
 * состояние игрока, требует прав оператора.
 */
public final class ArcanumCommands {
    private ArcanumCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(build()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("arcanum")
                .then(CommandManager.literal("spells").executes(ArcanumCommands::listSpells))
                .then(CommandManager.literal("known").executes(ArcanumCommands::listKnown))
                .then(CommandManager.literal("dispel").executes(ArcanumCommands::dispel))
                .then(CommandManager.literal("mana")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("fill").executes(ArcanumCommands::fillMana))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ArcanumCommands::setMana))))
                .then(CommandManager.literal("attunement")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("level",
                                        IntegerArgumentType.integer(0, ArcanumConfig.MAX_ATTUNEMENT))
                                .executes(ArcanumCommands::setAttunement)))
                .then(CommandManager.literal("learn")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("all").executes(ArcanumCommands::learnAll))
                        .then(CommandManager.argument("spell", IdentifierArgumentType.identifier())
                                .executes(ArcanumCommands::learnOne)))
                .then(CommandManager.literal("forget")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("all").executes(ArcanumCommands::forgetAll))
                        .then(CommandManager.argument("spell", IdentifierArgumentType.identifier())
                                .executes(ArcanumCommands::forgetOne)));
    }

    // ------------------------------------------------------------------

    private static int listSpells(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        for (SpellSchool school : SpellSchool.values()) {
            source.sendFeedback(() -> school.displayName(), false);
            for (Spell spell : SpellRegistry.bySchool(school)) {
                source.sendFeedback(() -> Text.literal("  ")
                        .append(spell.displayName())
                        .append(Text.literal(" — " + spell.id() + " | "
                                        + "ур." + spell.tier()
                                        + " | " + (int) spell.manaCost() + " маны")
                                .formatted(Formatting.GRAY)), false);
            }
        }
        return SpellRegistry.size();
    }

    private static int listKnown(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ManaData data = ManaManager.data(player);
        if (data.known().isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.translatable("message.arcanum.no_spells_known"), false);
            return 0;
        }
        for (Identifier id : data.known()) {
            Spell spell = SpellRegistry.get(id);
            Text line = spell != null ? spell.displayName() : Text.literal(id.toString());
            context.getSource().sendFeedback(() -> Text.literal(" • ").append(line), false);
        }
        return data.known().size();
    }

    private static int dispel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        int count = SummonManager.dispelAll(context.getSource().getServer(), player);
        context.getSource().sendFeedback(
                () -> Text.translatable("message.arcanum.dispelled", count), false);
        return count;
    }

    private static int fillMana(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ManaManager.fill(player);
        return report(context, player);
    }

    private static int setMana(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        float amount = FloatArgumentType.getFloat(context, "amount");
        ManaManager.set(player, ManaManager.data(player).withMana(Math.min(amount, ManaManager.max(player))));
        return report(context, player);
    }

    private static int setAttunement(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        int level = IntegerArgumentType.getInteger(context, "level");
        ManaManager.set(player, ManaManager.data(player).withAttunement(level));
        ManaManager.fill(player);
        return report(context, player);
    }

    private static int learnAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ManaData data = ManaManager.data(player);
        for (Identifier id : SpellRegistry.ids()) {
            data = data.learn(id);
        }
        ManaManager.set(player, data);
        ModNetworking.syncSpells(player);
        context.getSource().sendFeedback(
                () -> Text.translatable("message.arcanum.learned_all", SpellRegistry.size()), false);
        return SpellRegistry.size();
    }

    private static int learnOne(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "spell");
        Spell spell = SpellRegistry.get(id);
        if (spell == null) {
            context.getSource().sendError(Text.translatable("message.arcanum.unknown_spell", id.toString()));
            return 0;
        }
        ManaManager.set(player, ManaManager.data(player).learn(id));
        ModNetworking.syncSpells(player);
        context.getSource().sendFeedback(
                () -> Text.translatable("message.arcanum.spell_learned", spell.displayName()), false);
        return 1;
    }

    private static int forgetOne(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "spell");
        ManaManager.set(player, ManaManager.data(player).forget(id));
        ModNetworking.syncSpells(player);
        return 1;
    }

    private static int forgetAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ManaData data = ManaManager.data(player);
        int count = data.known().size();
        for (Identifier id : List.copyOf(data.known())) {
            data = data.forget(id);
        }
        ManaManager.set(player, data);
        ModNetworking.syncSpells(player);
        return count;
    }

    private static int report(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        int mana = (int) ManaManager.current(player);
        int max = ManaManager.max(player);
        context.getSource().sendFeedback(
                () -> Text.translatable("message.arcanum.mana_status", mana, max), false);
        return mana;
    }
}
