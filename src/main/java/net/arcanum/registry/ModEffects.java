package net.arcanum.registry;

import net.arcanum.Arcanum;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Эффекты мода.
 *
 * <p>Сами по себе они ничего не делают каждый тик — вся логика собрана
 * в {@link ModEvents} и {@link net.arcanum.mana.ManaManager}. Так эффекты
 * остаются простыми объектами и не зависят от версии сигнатур ванили.
 */
public final class ModEffects {
    private ModEffects() {
    }

    /** Ускоренное восстановление маны. */
    public static final RegistryEntry<StatusEffect> MANA_SURGE =
            register("mana_surge", StatusEffectCategory.BENEFICIAL, 0x55CCFF);

    /** Мана утекает вместо того, чтобы восстанавливаться. */
    public static final RegistryEntry<StatusEffect> MANA_BURN =
            register("mana_burn", StatusEffectCategory.HARMFUL, 0x8A2BE2);

    /** Часть входящего урона списывается с маны, а не со здоровья. */
    public static final RegistryEntry<StatusEffect> ARCANE_SHIELD =
            register("arcane_shield", StatusEffectCategory.BENEFICIAL, 0xB388FF);

    /** Промёрзшая цель получает больше урона от холода и медленнее двигается. */
    public static final RegistryEntry<StatusEffect> CHILLED =
            register("chilled", StatusEffectCategory.HARMFUL, 0x9FE3FF);

    /** Отметка пироманта: при завершении эффекта цель вспыхивает. */
    public static final RegistryEntry<StatusEffect> COMBUSTION =
            register("combustion", StatusEffectCategory.HARMFUL, 0xFF6A00);

    /** Благословение: прибавка к урону и сопротивление магии. */
    public static final RegistryEntry<StatusEffect> BLESSED =
            register("blessed", StatusEffectCategory.BENEFICIAL, 0xFFF0A0);

    /** Взор мага: подсвечивает существ вокруг. */
    public static final RegistryEntry<StatusEffect> ARCANE_SIGHT =
            register("arcane_sight", StatusEffectCategory.BENEFICIAL, 0xD0A0FF);

    /** Статический заряд от магии бури — топливо для реакции с арканой. */
    public static final RegistryEntry<StatusEffect> CHARGED =
            register("charged", StatusEffectCategory.HARMFUL, 0xF2EE8A);

    /** Нестабильность от чистой магии — топливо для реакций света и природы. */
    public static final RegistryEntry<StatusEffect> DESTABILIZED =
            register("destabilized", StatusEffectCategory.HARMFUL, 0xC77DFF);

    /** Печать безмолвия: цель не может колдовать. */
    public static final RegistryEntry<StatusEffect> SILENCE =
            register("silence", StatusEffectCategory.HARMFUL, 0x4A4A55);

    private static RegistryEntry<StatusEffect> register(String name, StatusEffectCategory category, int color) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Arcanum.id(name),
                new SimpleEffect(category, color));
    }

    /** Минимальная реализация: {@link StatusEffect} абстрактен и требует подкласса. */
    private static final class SimpleEffect extends StatusEffect {
        private SimpleEffect(StatusEffectCategory category, int color) {
            super(category, color);
        }
    }

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
