package net.ironman.suit;

import net.minecraft.util.StringIdentifiable;

/**
 * Модели костюма.
 *
 * <p>Все игровые числа костюма собраны здесь, чтобы баланс правился в одном
 * месте. Энергия — в условных единицах реактора, скорость — в блоках за тик.
 */
public enum SuitTier implements StringIdentifiable {
    /**
     * Mark I: собран в пещере из ящика обрезков. Крепкий, но медленный,
     * прожорливый и вместо репульсоров — огнемёт.
     */
    MARK_I("mark_i", "I", 0xFFB0B4B8,
            PrimaryWeapon.FLAMETHROWER, 4.0f, 0.0f, 0,
            0.035f, 0.035f, 0.8f, 1.6f, 0.30f),

    /** Mark III: классический красно-золотой костюм. */
    MARK_III("mark_iii", "III", 0xFFE0B040,
            PrimaryWeapon.REPULSOR, 8.0f, 20.0f, 4,
            0.08f, 0.10f, 1.6f, 1.0f, 0.0f),

    /** Mark 85: нанотехнологии, самый быстрый и экономичный. */
    MARK_85("mark_85", "85", 0xFF5FE8FF,
            PrimaryWeapon.REPULSOR, 12.0f, 32.0f, 8,
            0.12f, 0.16f, 2.2f, 0.6f, 0.0f);

    public enum PrimaryWeapon {
        FLAMETHROWER,
        REPULSOR
    }

    private final String id;
    private final String mark;
    private final int accent;
    private final PrimaryWeapon primary;
    private final float primaryDamage;
    private final float unibeamDamage;
    private final int missiles;
    private final float flySpeed;
    private final float boost;
    private final float maxSpeed;
    private final float costMultiplier;
    private final float slowness;

    SuitTier(String id, String mark, int accent, PrimaryWeapon primary, float primaryDamage,
             float unibeamDamage, int missiles, float flySpeed, float boost, float maxSpeed,
             float costMultiplier, float slowness) {
        this.id = id;
        this.mark = mark;
        this.accent = accent;
        this.primary = primary;
        this.primaryDamage = primaryDamage;
        this.unibeamDamage = unibeamDamage;
        this.missiles = missiles;
        this.flySpeed = flySpeed;
        this.boost = boost;
        this.maxSpeed = maxSpeed;
        this.costMultiplier = costMultiplier;
        this.slowness = slowness;
    }

    @Override
    public String asString() {
        return id;
    }

    /** Номер модели для HUD: «I», «III», «85». */
    public String mark() {
        return mark;
    }

    /** Цвет рамок HUD (ARGB). */
    public int accent() {
        return accent;
    }

    public PrimaryWeapon primary() {
        return primary;
    }

    public float primaryDamage() {
        return primaryDamage;
    }

    public boolean hasUnibeam() {
        return unibeamDamage > 0.0f;
    }

    public float unibeamDamage() {
        return unibeamDamage;
    }

    public int missiles() {
        return missiles;
    }

    /** Скорость «зависания» — это ванильная скорость полёта (по умолчанию 0.05). */
    public float flySpeed() {
        return flySpeed;
    }

    /** Ускорение реактивного рывка за тик. */
    public float boost() {
        return boost;
    }

    /** Предельная скорость рывка, блоков за тик. */
    public float maxSpeed() {
        return maxSpeed;
    }

    /** Множитель расхода энергии: у Mark I всё обходится дороже. */
    public float costMultiplier() {
        return costMultiplier;
    }

    /** Насколько костюм замедляет на земле (0 — не замедляет). */
    public float slowness() {
        return slowness;
    }

    public boolean sealed() {
        return this == MARK_85;
    }

    public static SuitTier byId(String id) {
        for (SuitTier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        return null;
    }
}
