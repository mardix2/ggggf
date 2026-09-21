package net.arcanum.client;

import net.arcanum.registry.ModItems;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellRegistry;
import net.arcanum.spell.SpellMastery;
import net.arcanum.spell.SpellSchool;
import net.arcanum.net.payload.SelectSpellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Книга заклинаний: вкладки школ, список заклинаний и описание выбранного.
 *
 * <p>Нарисована примитивами {@code fill} и {@code drawText}, без единой
 * текстуры интерфейса. Так книга выглядит одинаково при любом ресурс-паке
 * и не ломается при смене формата GUI-текстур в новых версиях игры.
 */
public class SpellbookScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 200;
    private static final int TAB_SIZE = 20;
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_WIDTH = 170;
    private static final int VISIBLE_ROWS = 8;

    private static final int COLOR_PANEL = 0xF01A1226;
    private static final int COLOR_BORDER = 0xFF6B4FA0;
    private static final int COLOR_ROW = 0x40FFFFFF;
    private static final int COLOR_ROW_HOVER = 0x70FFFFFF;
    private static final int COLOR_SELECTED = 0x80FFD966;

    private SpellSchool school = SpellSchool.FIRE;
    private List<Spell> spells = List.of();
    private Spell detail;
    private int scroll;

    private int left;
    private int top;

    public SpellbookScreen() {
        super(Text.translatable("screen.arcanum.spellbook"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        selectSchool(school);
    }

    private void selectSchool(SpellSchool next) {
        school = next;
        spells = new ArrayList<>(SpellRegistry.bySchool(next));
        scroll = 0;
        detail = spells.isEmpty() ? null : spells.get(0);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ------------------------------------------------------------------
    //  Отрисовка
    // ------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + PANEL_HEIGHT + 2, COLOR_BORDER);
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);

        context.drawText(textRenderer, title, left + 8, top + 7, 0xFFE8E4FF, true);
        context.drawText(textRenderer,
                Text.translatable("screen.arcanum.mana", (int) ClientSpellState.mana(),
                        ClientSpellState.maxMana()).formatted(Formatting.AQUA),
                left + PANEL_WIDTH - 110, top + 7, 0xFF7FD8FF, true);

        drawTabs(context, mouseX, mouseY);
        drawList(context, mouseX, mouseY);
        drawDetail(context);
    }

    private void drawTabs(DrawContext context, int mouseX, int mouseY) {
        SpellSchool[] schools = SpellSchool.values();
        for (int i = 0; i < schools.length; i++) {
            SpellSchool candidate = schools[i];
            int x = left + 8 + i * (TAB_SIZE + 4);
            int y = top + 22;
            boolean active = candidate == school;
            boolean hover = inside(mouseX, mouseY, x, y, TAB_SIZE, TAB_SIZE);

            context.fill(x - 1, y - 1, x + TAB_SIZE + 1, y + TAB_SIZE + 1,
                    active ? candidate.argb() : (hover ? COLOR_ROW_HOVER : COLOR_ROW));
            context.fill(x, y, x + TAB_SIZE, y + TAB_SIZE, COLOR_PANEL);
            context.drawItem(new ItemStack(ModItems.RUNES.get(candidate)), x + 2, y + 2);

            if (hover) {
                context.drawTooltip(textRenderer, candidate.displayName(), mouseX, mouseY);
            }
        }
    }

    private void drawList(DrawContext context, int mouseX, int mouseY) {
        int x = left + 8;
        int y = top + 50;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= spells.size()) {
                break;
            }
            Spell spell = spells.get(index);
            int rowY = y + row * ROW_HEIGHT;
            boolean hover = inside(mouseX, mouseY, x, rowY, LIST_WIDTH, ROW_HEIGHT - 2);
            boolean known = ClientSpellState.knows(spell.id());
            boolean selected = ClientSpellState.selectedId().map(id -> id.equals(spell.id())).orElse(false);

            int background = selected ? COLOR_SELECTED : (hover ? COLOR_ROW_HOVER : COLOR_ROW);
            context.fill(x, rowY, x + LIST_WIDTH, rowY + ROW_HEIGHT - 2, background);

            Text name = known
                    ? spell.displayName()
                    : Text.translatable("screen.arcanum.unknown_spell").formatted(Formatting.DARK_GRAY);
            context.drawText(textRenderer, name, x + 4, rowY + 5, known ? spell.school().argb() : 0xFF6A6A6A, false);

            Text cost = Text.literal(String.valueOf((int) spell.manaCost())).formatted(Formatting.GRAY);
            context.drawText(textRenderer, cost,
                    x + LIST_WIDTH - 6 - textRenderer.getWidth(cost), rowY + 5, 0xFFA0A0A0, false);

            if (hover) {
                detail = spell;
            }
        }

        if (spells.size() > VISIBLE_ROWS) {
            Text hint = Text.translatable("screen.arcanum.scroll_hint").formatted(Formatting.DARK_GRAY);
            context.drawText(textRenderer, hint, x, y + VISIBLE_ROWS * ROW_HEIGHT + 2, 0xFF6A6A6A, false);
        }
    }

    private void drawDetail(DrawContext context) {
        int x = left + LIST_WIDTH + 20;
        int y = top + 50;
        int panelWidth = PANEL_WIDTH - LIST_WIDTH - 30;

        if (detail == null) {
            return;
        }

        context.drawText(textRenderer, detail.displayName(), x, y, detail.school().argb(), true);

        int line = y + 14;
        line = drawStat(context, x, line, "screen.arcanum.stat.school", detail.school().displayName());
        line = drawStat(context, x, line, "screen.arcanum.stat.type", detail.castType().displayName());
        line = drawStat(context, x, line, "screen.arcanum.stat.tier",
                Text.literal(String.valueOf(detail.tier())));
        line = drawStat(context, x, line, "screen.arcanum.stat.cost",
                Text.literal(String.valueOf((int) detail.manaCost())));
        line = drawStat(context, x, line, "screen.arcanum.stat.cooldown",
                Text.literal(String.format("%.1f", detail.cooldownTicks() / 20.0f) + " c"));

        int mastery = ClientSpellState.mastery(detail.id());
        line = drawStat(context, x, line, "screen.arcanum.stat.mastery",
                SpellMastery.stars(mastery));
        int next = SpellMastery.nextThreshold(mastery);
        if (next > 0) {
            line = drawStat(context, x, line, "screen.arcanum.stat.progress",
                    Text.literal(ClientSpellState.casts(detail.id()) + " / " + next));
        }

        line += 6;
        for (var text : textRenderer.wrapLines(detail.description(), panelWidth)) {
            context.drawText(textRenderer, text, x, line, 0xFFB9B3CC, false);
            line += 10;
        }

        if (!ClientSpellState.knows(detail.id())) {
            context.drawText(textRenderer,
                    Text.translatable("screen.arcanum.not_learned").formatted(Formatting.RED),
                    x, top + PANEL_HEIGHT - 20, 0xFFFF6B6B, false);
        }
    }

    private int drawStat(DrawContext context, int x, int y, String key, Text value) {
        Text label = Text.translatable(key).formatted(Formatting.DARK_GRAY);
        context.drawText(textRenderer, label, x, y, 0xFF7A7A8A, false);
        context.drawText(textRenderer, value, x + 80, y, 0xFFD8D4E8, false);
        return y + 11;
    }

    // ------------------------------------------------------------------
    //  Ввод
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SpellSchool[] schools = SpellSchool.values();
        for (int i = 0; i < schools.length; i++) {
            int x = left + 8 + i * (TAB_SIZE + 4);
            int y = top + 22;
            if (inside((int) mouseX, (int) mouseY, x, y, TAB_SIZE, TAB_SIZE)) {
                selectSchool(schools[i]);
                click();
                return true;
            }
        }

        int listX = left + 8;
        int listY = top + 50;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= spells.size()) {
                break;
            }
            if (inside((int) mouseX, (int) mouseY, listX, listY + row * ROW_HEIGHT,
                    LIST_WIDTH, ROW_HEIGHT - 2)) {
                Spell spell = spells.get(index);
                detail = spell;
                if (ClientSpellState.knows(spell.id())) {
                    ClientSpellState.setSelectedLocally(spell.id());
                    ClientPlayNetworking.send(new SelectSpellPayload(spell.id()));
                    click();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int max = Math.max(0, spells.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }

    private void click() {
        if (client != null) {
            client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
