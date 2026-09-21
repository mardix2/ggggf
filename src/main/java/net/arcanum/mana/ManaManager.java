package net.arcanum.mana;

import net.arcanum.ArcanumConfig;
import net.arcanum.net.ModNetworking;
import net.arcanum.registry.ModBlocks;
import net.arcanum.registry.ModEffects;
import net.arcanum.spell.SpellSchool;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Всё, что связано с запасом маны: чтение, трата, восстановление.
 *
 * <p>Единственная точка, через которую меняются данные маны — так синхронизация
 * с клиентом гарантированно не забывается.
 */
public final class ManaManager {
    private ManaManager() {
    }

    /** Слоты снаряжения, которые учитываются при подсчёте бонусов. */
    private static final EquipmentSlot[] GEAR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    /** Задержка регенерации после траты маны; не сохраняется между сессиями. */
    private static final Map<UUID, Integer> REGEN_DELAY = new HashMap<>();

    /** Кэш проверки «рядом ли Источник маны»: {@code [тик проверки, результат]}. */
    private static final Map<UUID, long[]> FONT_CACHE = new HashMap<>();
    private static final int FONT_CHECK_INTERVAL = 20;

    public static ManaData data(PlayerEntity player) {
        return player.getAttachedOrCreate(ManaAttachments.MANA);
    }

    public static void set(PlayerEntity player, ManaData data) {
        player.setAttached(ManaAttachments.MANA, data);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ModNetworking.syncMana(serverPlayer);
        }
    }

    public static float current(PlayerEntity player) {
        return Math.min(data(player).mana(), max(player));
    }

    public static int max(PlayerEntity player) {
        int total = ArcanumConfig.BASE_MANA
                + data(player).attunement() * ArcanumConfig.MANA_PER_ATTUNEMENT;
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ArcaneGear gear) {
                total += gear.manaCapacityBonus(stack);
            }
        }
        return Math.max(1, total);
    }

    public static boolean has(PlayerEntity player, float amount) {
        return player.isCreative() || current(player) >= amount;
    }

    /** Тратит ману, если её хватает. Возвращает {@code false}, если не хватило. */
    public static boolean consume(PlayerEntity player, float amount) {
        if (player.isCreative()) {
            return true;
        }
        float cur = current(player);
        if (cur < amount) {
            return false;
        }
        set(player, data(player).withMana(cur - amount));
        REGEN_DELAY.put(player.getUuid(), ArcanumConfig.REGEN_DELAY_TICKS);
        return true;
    }

    public static void give(PlayerEntity player, float amount) {
        float next = Math.min(max(player), current(player) + amount);
        set(player, data(player).withMana(next));
    }

    /** Полное восстановление — используется командами и кроватью-алтарём. */
    public static void fill(PlayerEntity player) {
        set(player, data(player).withMana(max(player)));
    }

    /**
     * Суммарный множитель стоимости от всего снаряжения.
     * Перемножается, а не складывается, чтобы комплект не уводил цену в ноль.
     */
    public static float costMultiplier(PlayerEntity player) {
        float mult = 1.0f;
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ArcaneGear gear) {
                mult *= gear.manaCostMultiplier(stack);
            }
        }
        return Math.max(0.2f, mult);
    }

    public static float cooldownMultiplier(PlayerEntity player) {
        float mult = 1.0f;
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ArcaneGear gear) {
                mult *= gear.cooldownMultiplier(stack);
            }
        }
        return Math.max(0.25f, mult);
    }

    /** Суммарная прибавка силы для школы: 0.0 — без бонусов, 0.5 — +50%. */
    public static float powerBonus(PlayerEntity player, SpellSchool school) {
        float bonus = 0.0f;
        if (player.hasStatusEffect(ModEffects.BLESSED)) {
            bonus += 0.15f * (player.getStatusEffect(ModEffects.BLESSED).getAmplifier() + 1);
        }
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ArcaneGear gear) {
                bonus += gear.powerBonus(stack, school);
            }
        }
        return bonus;
    }

    /** Вызывается каждый тик для каждого игрока на сервере. */
    public static void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int delay = REGEN_DELAY.getOrDefault(uuid, 0);
        if (delay > 0) {
            REGEN_DELAY.put(uuid, delay - 1);
            return;
        }

        float cur = current(player);
        int max = max(player);
        if (cur >= max) {
            // Значение могло «зависнуть» выше максимума после снятия мантии.
            if (data(player).mana() > max) {
                set(player, data(player).withMana(max));
            }
            return;
        }

        float regen = ArcanumConfig.BASE_REGEN_PER_TICK;
        for (EquipmentSlot slot : GEAR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ArcaneGear gear) {
                regen += gear.manaRegenBonus(stack);
            }
        }
        if (player.hasStatusEffect(ModEffects.MANA_SURGE)) {
            int amp = player.getStatusEffect(ModEffects.MANA_SURGE).getAmplifier();
            regen *= 2.0f + amp;
        }
        if (player.hasStatusEffect(ModEffects.MANA_BURN)) {
            // Выжигание маны не просто блокирует регенерацию, а откатывает запас.
            int amp = player.getStatusEffect(ModEffects.MANA_BURN).getAmplifier();
            set(player, data(player).withMana(Math.max(0.0f, cur - 0.5f * (amp + 1))));
            return;
        }
        if (nearManaFont(player)) {
            regen *= ArcanumConfig.FONT_REGEN_MULTIPLIER;
        }

        set(player, data(player).withMana(Math.min(max, cur + regen)));
    }

    /**
     * Проверка «рядом ли Источник маны».
     *
     * <p>Скан куба радиусом 8 стоит почти пять тысяч обращений к миру, поэтому
     * результат кэшируется на секунду — за это время игрок всё равно не успеет
     * уйти далеко от источника.
     */
    private static boolean nearManaFont(PlayerEntity player) {
        World world = player.getWorld();
        long now = world.getTime();
        long[] cached = FONT_CACHE.get(player.getUuid());
        if (cached != null && now - cached[0] < FONT_CHECK_INTERVAL) {
            return cached[1] != 0L;
        }

        int r = (int) ArcanumConfig.FONT_RADIUS;
        BlockPos origin = player.getBlockPos();
        boolean found = false;
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        outer:
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -r; dy <= r; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!world.isChunkLoaded(cursor)) {
                        continue;
                    }
                    if (world.getBlockState(cursor).isOf(ModBlocks.MANA_FONT)) {
                        found = true;
                        break outer;
                    }
                }
            }
        }
        FONT_CACHE.put(player.getUuid(), new long[]{now, found ? 1L : 0L});
        return found;
    }

    public static void onDisconnect(PlayerEntity player) {
        REGEN_DELAY.remove(player.getUuid());
        FONT_CACHE.remove(player.getUuid());
        SpellCooldowns.clear(player.getUuid());
    }
}
