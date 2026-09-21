package net.arcanum.spell;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Описание одного заклинания: стоимость, перезарядка, школа и сам эффект.
 *
 * <p>Создаётся через {@link #builder}; экземпляры неизменяемы и живут
 * в {@link SpellRegistry} в единственном числе.
 */
public final class Spell {

    /** Эффект мгновенного или зарядного заклинания. Возвращает {@code false}, если применить не удалось. */
    @FunctionalInterface
    public interface Action {
        boolean cast(SpellContext ctx);
    }

    private final Identifier id;
    private final SpellSchool school;
    private final int tier;
    private final float manaCost;
    private final int cooldownTicks;
    private final SpellCastType castType;
    private final Action action;

    private Spell(Builder b) {
        this.id = b.id;
        this.school = b.school;
        this.tier = b.tier;
        this.manaCost = b.manaCost;
        this.cooldownTicks = b.cooldownTicks;
        this.castType = b.castType;
        this.action = b.action;
    }

    public Identifier id() {
        return id;
    }

    public SpellSchool school() {
        return school;
    }

    /** Минимальный уровень посоха, который может применить заклинание. */
    public int tier() {
        return tier;
    }

    public float manaCost() {
        return manaCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public SpellCastType castType() {
        return castType;
    }

    public boolean cast(SpellContext ctx) {
        return action != null && action.cast(ctx);
    }

    public String translationKey() {
        return "spell." + id.getNamespace() + "." + id.getPath();
    }

    public String descriptionKey() {
        return translationKey() + ".desc";
    }

    public Text displayName() {
        return Text.translatable(translationKey()).formatted(school.formatting());
    }

    public Text description() {
        return Text.translatable(descriptionKey());
    }

    @Override
    public String toString() {
        return "Spell[" + id + "]";
    }

    public static Builder builder(String path, SpellSchool school) {
        return new Builder(net.arcanum.Arcanum.id(path), school);
    }

    public static final class Builder {
        private final Identifier id;
        private final SpellSchool school;
        private int tier = 1;
        private float manaCost = 10.0f;
        private int cooldownTicks = 20;
        private SpellCastType castType = SpellCastType.TARGET;
        private Action action;

        private Builder(Identifier id, SpellSchool school) {
            this.id = id;
            this.school = school;
        }

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder cost(float manaCost) {
            this.manaCost = manaCost;
            return this;
        }

        public Builder cooldown(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public Builder type(SpellCastType castType) {
            this.castType = castType;
            return this;
        }

        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        public Spell register() {
            if (action == null) {
                throw new IllegalStateException("У заклинания " + id + " не задан эффект");
            }
            return SpellRegistry.register(new Spell(this));
        }
    }
}
