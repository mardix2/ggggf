package net.ironman.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.ironman.registry.ModItems;
import net.ironman.suit.ReactorData;
import net.ironman.suit.ReactorTier;
import net.ironman.suit.SuitManager;
import net.ironman.suit.SuitTier;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Команды для проверки и творческого режима:
 * <ul>
 *     <li>{@code /ironman suit <модель>} — надеть костюм и поставить реактор;</li>
 *     <li>{@code /ironman reactor <реактор>} — установить реактор;</li>
 *     <li>{@code /ironman energy <число>} — задать заряд;</li>
 *     <li>{@code /ironman status} — показать состояние.</li>
 * </ul>
 */
public final class IronManCommands {
    private IronManCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("ironman")
                    .then(CommandManager.literal("status").executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        ReactorData data = SuitManager.reactor(player);
                        SuitTier tier = SuitManager.onlineTier(player);
                        context.getSource().sendFeedback(() -> Text.translatable("command.ironman.status",
                                Text.translatable(data.reactor().translationKey()),
                                (int) data.energy(), data.capacity(),
                                tier == null ? "—" : "Mark " + tier.mark()), false);
                        return 1;
                    }));

            LiteralArgumentBuilder<ServerCommandSource> suit = CommandManager.literal("suit")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK));
            for (SuitTier tier : SuitTier.values()) {
                suit.then(CommandManager.literal(tier.asString()).executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    equip(player, tier);
                    context.getSource().sendFeedback(() -> Text.translatable("command.ironman.suit",
                            "Mark " + tier.mark()), true);
                    return 1;
                }));
            }

            LiteralArgumentBuilder<ServerCommandSource> reactor = CommandManager.literal("reactor")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK));
            for (ReactorTier tier : ReactorTier.values()) {
                reactor.then(CommandManager.literal(tier.id()).executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    SuitManager.setReactor(player, new ReactorData(tier, tier.capacity()));
                    context.getSource().sendFeedback(() -> Text.translatable("command.ironman.reactor",
                            Text.translatable(tier.translationKey())), true);
                    return 1;
                }));
            }

            root.then(suit).then(reactor).then(CommandManager.literal("energy")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                ReactorData data = SuitManager.reactor(player);
                                SuitManager.setReactor(player, data.withEnergy(amount));
                                context.getSource().sendFeedback(() -> Text.translatable(
                                        "command.ironman.energy", (int) SuitManager.reactor(player).energy()),
                                        true);
                                return 1;
                            })));

            dispatcher.register(root);
        });
    }

    /** Надевает полный костюм (старая броня уходит в инвентарь) и ставит реактор. */
    private static void equip(ServerPlayerEntity player, SuitTier tier) {
        for (EquipmentType type : List.of(EquipmentType.HELMET, EquipmentType.CHESTPLATE,
                EquipmentType.LEGGINGS, EquipmentType.BOOTS)) {
            EquipmentSlot slot = type.getEquipmentSlot();
            ItemStack old = player.getEquippedStack(slot);
            if (!old.isEmpty() && !player.giveItemStack(old.copy())) {
                player.dropItem(old.copy(), false);
            }
            player.equipStack(slot, new ItemStack(ModItems.suitPart(tier, type)));
        }
        ReactorData data = SuitManager.reactor(player);
        if (!data.installed()) {
            ReactorTier reactor = switch (tier) {
                case MARK_I -> ReactorTier.MK_I;
                case MARK_III -> ReactorTier.MK_II;
                case MARK_85 -> ReactorTier.STARKIUM;
            };
            SuitManager.setReactor(player, new ReactorData(reactor, reactor.capacity()));
        }
    }
}
