package net.arcanum.client;

import net.arcanum.Arcanum;
import net.arcanum.ArcanumConfig;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellMastery;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Полоса маны и выбранное заклинание в левом верхнем углу.
 *
 * <p>Угол выбран намеренно: там не мешают ни ванильные полосы здоровья,
 * ни эффекты, ни панель быстрого доступа.
 */
public final class ArcanumHud implements HudElement {

    private static final int WIDTH = 110;
    private static final int HEIGHT = 12;

    private static final int COLOR_FRAME = 0xFF2B1B44;
    private static final int COLOR_BACK = 0xC0100A18;
    private static final int COLOR_MANA = 0xFF4FA8FF;
    private static final int COLOR_MANA_TOP = 0xFF9BD6FF;
    private static final int COLOR_TEXT = 0xFFE8E4FF;

    /**
     * Регистрация HUD. Вынесена в отдельный метод, потому что имя этого
     * API в Fabric менялось: до 1.21.6 использовался {@code HudRenderCallback}.
     */
    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Arcanum.id("mana_bar"), new ArcanumHud());
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.player.isSpectator()) {
            return;
        }

        drawManaBar(context, client);
        drawSelectedSpell(context, client);
    }

    private void drawManaBar(DrawContext context, MinecraftClient client) {
        int x = ArcanumConfig.HUD_X;
        int y = ArcanumConfig.HUD_Y;

        context.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, COLOR_FRAME);
        context.fill(x, y, x + WIDTH, y + HEIGHT, COLOR_BACK);

        int filled = Math.round(WIDTH * ClientSpellState.manaFraction());
        if (filled > 0) {
            context.fill(x, y, x + filled, y + HEIGHT, COLOR_MANA);
            // Светлая полоска сверху даёт объём без единой текстуры.
            context.fill(x, y, x + filled, y + 3, COLOR_MANA_TOP);
        }

        Text label = Text.literal((int) ClientSpellState.mana() + " / " + ClientSpellState.maxMana());
        int textX = x + (WIDTH - client.textRenderer.getWidth(label)) / 2;
        context.drawText(client.textRenderer, label, textX, y + 2, COLOR_TEXT, true);
    }

    private void drawSelectedSpell(DrawContext context, MinecraftClient client) {
        Spell spell = ClientSpellState.selectedSpell();
        int x = ArcanumConfig.HUD_X;
        int y = ArcanumConfig.HUD_Y + HEIGHT + 4;

        if (spell == null) {
            context.drawText(client.textRenderer,
                    Text.translatable("hud.arcanum.no_spell").formatted(Formatting.DARK_GRAY),
                    x, y, 0xFF8A8A8A, true);
            return;
        }

        context.drawText(client.textRenderer, spell.displayName(), x, y, spell.school().argb(), true);

        int mastery = ClientSpellState.mastery(spell.id());
        if (mastery > 0) {
            int offset = client.textRenderer.getWidth(spell.displayName()) + 4;
            context.drawText(client.textRenderer, SpellMastery.stars(mastery),
                    x + offset, y, 0xFFFFD966, true);
        }

        int cooldown = ClientSpellState.cooldown(spell.id());
        if (cooldown > 0) {
            Text text = Text.translatable("hud.arcanum.cooldown",
                    String.format("%.1f", cooldown / 20.0f)).formatted(Formatting.GRAY);
            context.drawText(client.textRenderer, text, x, y + 10, 0xFFB0B0B0, true);
            return;
        }

        int cost = (int) spell.manaCost();
        boolean affordable = ClientSpellState.mana() >= cost;
        Text text = Text.translatable("hud.arcanum.cost", cost);
        context.drawText(client.textRenderer, text, x, y + 10,
                affordable ? 0xFF7FD8FF : 0xFFFF6B6B, true);
    }
}
