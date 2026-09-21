package net.arcanum.client;

import net.arcanum.net.payload.SelectSpellPayload;
import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellMastery;
import net.arcanum.spell.SpellRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Колесо быстрого выбора: держите клавишу, ведите мышь к нужному
 * заклинанию, отпускайте.
 *
 * <p>Как и книга, нарисовано примитивами — ни одной текстуры интерфейса.
 * Секторы не рисуются заливкой по дуге (её в {@code DrawContext} нет),
 * вместо этого значки расставлены по окружности, а выбранный подсвечен.
 */
public class SpellWheelScreen extends Screen {

    private static final int RADIUS = 62;
    private static final int TILE = 22;
    private static final int DEAD_ZONE = 26;

    private static final int COLOR_DIM = 0x70000000;
    private static final int COLOR_TILE = 0xB01A1226;
    private static final int COLOR_HOVER = 0xF0000000;

    private final List<Identifier> entries;
    private int hovered = -1;

    public SpellWheelScreen() {
        super(Text.translatable("screen.arcanum.wheel"));
        this.entries = ClientSpellState.wheel();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, COLOR_DIM);

        int centerX = width / 2;
        int centerY = height / 2;
        hovered = pick(mouseX - centerX, mouseY - centerY);

        for (int index = 0; index < entries.size(); index++) {
            Spell spell = SpellRegistry.get(entries.get(index));
            if (spell == null) {
                continue;
            }
            double angle = angleOf(index);
            int x = centerX + (int) Math.round(Math.cos(angle) * RADIUS) - TILE / 2;
            int y = centerY + (int) Math.round(Math.sin(angle) * RADIUS) - TILE / 2;

            boolean active = index == hovered;
            context.fill(x - 2, y - 2, x + TILE + 2, y + TILE + 2,
                    active ? spell.school().argb() : COLOR_TILE);
            context.fill(x, y, x + TILE, y + TILE, active ? COLOR_HOVER : COLOR_TILE);
            context.drawItem(new ItemStack(ModItems.RUNES.get(spell.school())), x + 3, y + 3);

            int cooldown = ClientSpellState.cooldown(spell.id());
            if (cooldown > 0) {
                // Заклинание на откате — гасим значок.
                context.fill(x, y, x + TILE, y + TILE, 0xA0000000);
            }
        }

        drawCenterLabel(context, centerX, centerY);
    }

    private void drawCenterLabel(DrawContext context, int centerX, int centerY) {
        if (hovered < 0 || hovered >= entries.size()) {
            Text hint = Text.translatable(entries.isEmpty()
                    ? "screen.arcanum.wheel_empty" : "screen.arcanum.wheel_hint")
                    .formatted(Formatting.GRAY);
            context.drawCenteredTextWithShadow(textRenderer, hint, centerX, centerY - 4, 0xFFAAAAAA);
            return;
        }

        Spell spell = SpellRegistry.get(entries.get(hovered));
        if (spell == null) {
            return;
        }
        context.drawCenteredTextWithShadow(textRenderer, spell.displayName(),
                centerX, centerY - 12, spell.school().argb());

        Text cost = Text.translatable("hud.arcanum.cost", (int) spell.manaCost());
        context.drawCenteredTextWithShadow(textRenderer, cost, centerX, centerY, 0xFF7FD8FF);

        int mastery = ClientSpellState.mastery(spell.id());
        if (mastery > 0) {
            context.drawCenteredTextWithShadow(textRenderer, SpellMastery.stars(mastery),
                    centerX, centerY + 11, 0xFFFFD966);
        }
    }

    private double angleOf(int index) {
        return -Math.PI / 2 + index * 2 * Math.PI / Math.max(1, entries.size());
    }

    /** По смещению курсора от центра определяет сектор. */
    private int pick(int dx, int dy) {
        if (entries.isEmpty() || Math.hypot(dx, dy) < DEAD_ZONE) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        if (angle < 0) {
            angle += Math.PI * 2;
        }
        double step = 2 * Math.PI / entries.size();
        return (int) Math.round(angle / step) % entries.size();
    }

    /** Вызывается, когда игрок отпустил клавишу колеса. */
    public void confirmAndClose() {
        if (hovered >= 0 && hovered < entries.size()) {
            Identifier spell = entries.get(hovered);
            ClientSpellState.setSelectedLocally(spell);
            ClientPlayNetworking.send(new SelectSpellPayload(spell));
            if (client != null) {
                client.getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));
            }
        }
        close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        confirmAndClose();
        return true;
    }
}
