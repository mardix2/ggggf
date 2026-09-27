package net.ironman.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.ironman.IronMan;
import net.ironman.registry.ModItems;
import net.ironman.suit.ReactorTier;
import net.ironman.suit.Suit;
import net.ironman.suit.SuitTier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * HUD шлема — интерфейс J.A.R.V.I.S.
 *
 * <p>Рисуется только прямоугольниками и текстом, без единой текстуры:
 * рамка визора по углам, реактор, полёт, оружие, анализ цели и прицел.
 * Появляется, когда на игроке надет шлем любой модели.
 */
public final class JarvisHud implements HudElement {

    private static final int CYAN = 0xFF5FE8FF;
    private static final int CYAN_DIM = 0x995FE8FF;
    private static final int CYAN_FAINT = 0x405FE8FF;
    private static final int PANEL = 0x70001018;
    private static final int WHITE = 0xFFE8FDFF;
    private static final int AMBER = 0xFFFFB84D;
    private static final int RED = 0xFFFF5252;
    private static final int GREEN = 0xFF7CFF9E;

    private static final int BAR_WIDTH = 104;
    private static final double SCAN_RANGE = 64.0;

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                IronMan.id("jarvis"), new JarvisHud());
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.options.hudHidden || player.isSpectator() || !ClientSuitState.hudEnabled()) {
            return;
        }
        SuitTier helmet = Suit.helmetTier(player);
        if (helmet == null) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        SuitTier suit = Suit.tierOf(player);
        int accent = suit != null ? suit.accent() : CYAN;
        TextRenderer text = client.textRenderer;

        drawVisorFrame(context, width, height, accent);
        drawReticle(context, width, height, accent);

        int y = drawStatus(context, text, player, suit);
        y = drawReactor(context, text, y);
        y = drawFlight(context, text, player, y);
        drawWeapons(context, text, player, suit, y);
        drawTarget(context, text, client, player, tickCounter, width);
        drawWarnings(context, text, player, width, height);
    }

    // ---- рамка и прицел ---------------------------------------------------------

    private void drawVisorFrame(DrawContext context, int width, int height, int accent) {
        int inset = 4;
        int length = 22;
        int color = (accent & 0x00FFFFFF) | 0xB0000000;
        // Четыре уголка визора.
        corner(context, inset, inset, length, 1, 1, color);
        corner(context, width - inset - 1, inset, length, -1, 1, color);
        corner(context, inset, height - inset - 1, length, 1, -1, color);
        corner(context, width - inset - 1, height - inset - 1, length, -1, -1, color);
    }

    private void corner(DrawContext context, int x, int y, int length, int dx, int dy, int color) {
        int x2 = x + dx * length;
        int y2 = y + dy * length;
        context.fill(Math.min(x, x2), y, Math.max(x, x2) + 1, y + 1, color);
        context.fill(x, Math.min(y, y2), x + 1, Math.max(y, y2) + 1, color);
    }

    private void drawReticle(DrawContext context, int width, int height, int accent) {
        int cx = width / 2;
        int cy = height / 2;
        int r = 9;
        int arm = 3;
        int color = (accent & 0x00FFFFFF) | 0x90000000;
        corner(context, cx - r, cy - r, arm, 1, 1, color);
        corner(context, cx + r, cy - r, arm, -1, 1, color);
        corner(context, cx - r, cy + r, arm, 1, -1, color);
        corner(context, cx + r, cy + r, arm, -1, -1, color);
    }

    // ---- левая колонка ----------------------------------------------------------

    private int drawStatus(DrawContext context, TextRenderer text, ClientPlayerEntity player, SuitTier suit) {
        int x = 10;
        int y = 10;
        context.fill(x - 3, y - 3, x + BAR_WIDTH + 3, y + 21, PANEL);
        context.drawText(text, Text.literal("J.A.R.V.I.S."), x, y, CYAN, true);

        Text status;
        int color;
        if (suit == null) {
            status = Text.translatable("hud.ironman.incomplete", Suit.pieces(player));
            color = AMBER;
        } else if (ClientSuitState.reactor() == ReactorTier.NONE) {
            status = Text.translatable("hud.ironman.no_reactor", "Mark " + suit.mark());
            color = RED;
        } else if (ClientSuitState.energy() <= 0 && !player.isCreative()) {
            status = Text.translatable("hud.ironman.offline", "Mark " + suit.mark());
            color = RED;
        } else {
            status = Text.translatable("hud.ironman.online", "Mark " + suit.mark());
            color = GREEN;
        }
        context.drawText(text, status, x, y + 10, color, true);
        return y + 26;
    }

    private int drawReactor(DrawContext context, TextRenderer text, int y) {
        int x = 10;
        ReactorTier reactor = ClientSuitState.reactor();
        context.fill(x - 3, y - 3, x + BAR_WIDTH + 3, y + 22, PANEL);

        Text label = Text.translatable("hud.ironman.reactor", Text.translatable(reactor.translationKey()));
        context.drawText(text, label, x, y, CYAN_DIM, true);

        float fraction = ClientSuitState.fraction();
        int barY = y + 10;
        int barColor = fraction > 0.5f ? CYAN : fraction > 0.2f ? AMBER : RED;
        context.fill(x, barY, x + BAR_WIDTH, barY + 6, CYAN_FAINT);
        int filled = Math.round(BAR_WIDTH * fraction);
        if (filled > 0) {
            context.fill(x, barY, x + filled, barY + 6, barColor);
        }
        // Деления по 10 %: «сегментированная» полоса смотрится технологичнее.
        for (int i = 1; i < 10; i++) {
            int sx = x + BAR_WIDTH * i / 10;
            context.fill(sx, barY, sx + 1, barY + 6, PANEL);
        }
        String percent = Math.round(fraction * 100) + "%";
        context.drawText(text, percent, x + BAR_WIDTH - text.getWidth(percent), y, WHITE, true);
        return y + 27;
    }

    private int drawFlight(DrawContext context, TextRenderer text, ClientPlayerEntity player, int y) {
        int x = 10;
        context.fill(x - 3, y - 3, x + BAR_WIDTH + 3, y + 21, PANEL);

        Text state;
        int color;
        if (!ClientSuitState.flightArmed()) {
            state = Text.translatable("hud.ironman.flight_off");
            color = CYAN_DIM;
        } else if (player.getAbilities().flying) {
            state = Text.translatable("hud.ironman.flight_active");
            color = GREEN;
        } else {
            state = Text.translatable("hud.ironman.flight_armed");
            color = CYAN;
        }
        context.drawText(text, state, x, y, color, true);

        double speed = player.getVelocity().length() * 20.0;
        int heading = Math.floorMod(Math.round(player.getYaw()) + 180, 360);
        String telemetry = String.format("ALT %d  SPD %.0f  HDG %03d", (int) Math.floor(player.getY()),
                speed, heading);
        context.drawText(text, telemetry, x, y + 10, WHITE, true);
        return y + 26;
    }

    private void drawWeapons(DrawContext context, TextRenderer text, ClientPlayerEntity player,
                             SuitTier suit, int y) {
        if (suit == null) {
            return;
        }
        int x = 10;
        int rows = 1 + (suit.hasUnibeam() ? 1 : 0) + (suit.missiles() > 0 ? 1 : 0);
        context.fill(x - 3, y - 3, x + BAR_WIDTH + 3, y + rows * 10 + 1, PANEL);

        Text primary = Text.translatable(suit.primary() == SuitTier.PrimaryWeapon.FLAMETHROWER
                ? "hud.ironman.flamethrower" : "hud.ironman.repulsor");
        weaponRow(context, text, x, y, IronManKeys.PRIMARY, primary, ClientSuitState.primaryCooldown());
        y += 10;
        if (suit.hasUnibeam()) {
            weaponRow(context, text, x, y, IronManKeys.UNIBEAM, Text.translatable("hud.ironman.unibeam"),
                    ClientSuitState.unibeamCooldown());
            y += 10;
        }
        if (suit.missiles() > 0) {
            int ammo = player.isCreative() ? -1 : countMissiles(player);
            Text name = ammo < 0 ? Text.translatable("hud.ironman.missiles_inf")
                    : Text.translatable("hud.ironman.missiles", ammo);
            weaponRow(context, text, x, y, IronManKeys.MISSILES, name, ClientSuitState.missileCooldown());
        }
    }

    private void weaponRow(DrawContext context, TextRenderer text, int x, int y, KeyBinding key,
                           Text name, int cooldown) {
        Text keyLabel = Text.literal("[").append(key.getBoundKeyLocalizedText()).append("] ");
        context.drawText(text, keyLabel, x, y, CYAN_DIM, true);
        int nameX = x + text.getWidth(keyLabel);
        context.drawText(text, name, nameX, y, WHITE, true);

        String state = cooldown > 0 ? String.format("%.1fs", cooldown / 20.0f) : "OK";
        context.drawText(text, state, x + BAR_WIDTH - text.getWidth(state), y,
                cooldown > 0 ? AMBER : GREEN, true);
    }

    private static int countMissiles(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(ModItems.MICRO_MISSILE)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // ---- анализ цели (справа) ------------------------------------------------------

    private void drawTarget(DrawContext context, TextRenderer text, MinecraftClient client,
                            ClientPlayerEntity player, RenderTickCounter tickCounter, int width) {
        LivingEntity target = scan(player, tickCounter.getTickProgress(true));
        if (target == null) {
            return;
        }
        int panelWidth = 120;
        int x = width - panelWidth - 10;
        int y = 10;
        context.fill(x - 3, y - 3, x + panelWidth + 3, y + 31, PANEL);

        boolean hostile = target instanceof Monster;
        Text header = Text.translatable(hostile ? "hud.ironman.target_hostile" : "hud.ironman.target");
        context.drawText(text, header, x, y, hostile ? RED : CYAN_DIM, true);
        String distance = String.format("%.1fm", player.distanceTo(target));
        context.drawText(text, distance, x + panelWidth - text.getWidth(distance), y, WHITE, true);

        context.drawText(text, target.getDisplayName(), x, y + 10, WHITE, true);

        float health = MathHelper.clamp(target.getHealth() / target.getMaxHealth(), 0.0f, 1.0f);
        int barY = y + 21;
        context.fill(x, barY, x + panelWidth, barY + 4, CYAN_FAINT);
        context.fill(x, barY, x + Math.round(panelWidth * health), barY + 4, hostile ? RED : GREEN);
    }

    /** Луч из глаз на 64 блока: первое живое существо до ближайшего блока. */
    private static LivingEntity scan(ClientPlayerEntity player, float tickProgress) {
        Vec3d eye = player.getCameraPosVec(tickProgress);
        Vec3d look = player.getRotationVec(tickProgress);
        Vec3d end = eye.add(look.multiply(SCAN_RANGE));

        HitResult block = player.raycast(SCAN_RANGE, tickProgress, false);
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getPos();
        }
        Box box = player.getBoundingBox().stretch(look.multiply(SCAN_RANGE)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box,
                entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isAlive(),
                eye.squaredDistanceTo(end));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    // ---- предупреждения ------------------------------------------------------------

    private void drawWarnings(DrawContext context, TextRenderer text, ClientPlayerEntity player,
                              int width, int height) {
        if (ClientSuitState.reactor() == ReactorTier.NONE || player.isCreative()) {
            return;
        }
        float fraction = ClientSuitState.fraction();
        if (fraction >= 0.15f) {
            return;
        }
        // Мигание раз в полсекунды.
        if ((player.age / 10) % 2 == 0) {
            Text warning = Text.translatable("hud.ironman.low_power");
            int w = text.getWidth(warning);
            int x = (width - w) / 2;
            int y = height / 2 - 40;
            context.fill(x - 4, y - 3, x + w + 4, y + 11, 0x90300000);
            context.drawText(text, warning, x, y, RED, true);
        }
    }
}
