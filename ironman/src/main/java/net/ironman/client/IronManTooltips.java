package net.ironman.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.ironman.item.ArcReactorItem;
import net.ironman.item.IronManArmorItem;
import net.ironman.item.PowerCellItem;
import net.ironman.suit.ReactorTier;
import net.ironman.suit.SuitTier;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Подсказки предметов мода — в одном месте, чтобы формат был единым. */
public final class IronManTooltips {
    private IronManTooltips() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.getItem() instanceof IronManArmorItem armor) {
                armorTooltip(armor, lines);
            } else if (stack.getItem() instanceof ArcReactorItem reactor) {
                reactorTooltip(reactor.tier(), lines);
            } else if (stack.getItem() instanceof PowerCellItem cell) {
                lines.add(Text.translatable("tooltip.ironman.power_cell", cell.energy())
                        .formatted(Formatting.AQUA));
            }
        });
    }

    private static void armorTooltip(IronManArmorItem armor, List<Text> lines) {
        SuitTier tier = armor.tier();
        lines.add(Text.translatable("tooltip.ironman.suit_part", "Mark " + tier.mark())
                .formatted(Formatting.GOLD));
        if (armor.type() == EquipmentType.HELMET) {
            lines.add(Text.translatable("tooltip.ironman.helmet").formatted(Formatting.AQUA));
        }
        if (armor.type() != EquipmentType.CHESTPLATE) {
            return;
        }
        // Полный список систем — на нагруднике, чтобы не дублировать его четырежды.
        lines.add(Text.translatable("tooltip.ironman.full_set").formatted(Formatting.GRAY));
        lines.add(line(tier.primary() == SuitTier.PrimaryWeapon.FLAMETHROWER
                ? Text.translatable("tooltip.ironman.flamethrower", (int) tier.primaryDamage())
                : Text.translatable("tooltip.ironman.repulsor", (int) tier.primaryDamage())));
        lines.add(line(Text.translatable("tooltip.ironman.flight",
                Math.round(tier.maxSpeed() * 20.0f))));
        if (tier.hasUnibeam()) {
            lines.add(line(Text.translatable("tooltip.ironman.unibeam", (int) tier.unibeamDamage())));
        }
        if (tier.missiles() > 0) {
            lines.add(line(Text.translatable("tooltip.ironman.missiles", tier.missiles())));
        }
        if (tier.sealed()) {
            lines.add(line(Text.translatable("tooltip.ironman.sealed")));
        }
        if (tier.slowness() > 0.0f) {
            lines.add(Text.translatable("tooltip.ironman.heavy").formatted(Formatting.RED));
        }
        lines.add(Text.translatable("tooltip.ironman.efficiency",
                Math.round(100.0f / tier.costMultiplier())).formatted(Formatting.DARK_AQUA));
    }

    private static void reactorTooltip(ReactorTier tier, List<Text> lines) {
        lines.add(Text.translatable("tooltip.ironman.reactor_capacity", tier.capacity())
                .formatted(Formatting.AQUA));
        lines.add(Text.translatable("tooltip.ironman.reactor_regen",
                String.format("%.0f", tier.regen() * 20.0f)).formatted(Formatting.AQUA));
        lines.add(Text.translatable("tooltip.ironman.reactor_use").formatted(Formatting.GRAY));
    }

    private static Text line(Text body) {
        return Text.literal(" • ").formatted(Formatting.DARK_GRAY).append(body.copy().formatted(Formatting.AQUA));
    }
}
