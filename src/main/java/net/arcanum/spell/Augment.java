package net.arcanum.spell;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;

/**
 * Руна-модификатор, впечатываемая в посох.
 *
 * <p>Каждая что-то даёт и что-то забирает: чистых улучшений здесь нет,
 * иначе выбор был бы не выбором, а списком обязательных деталей.
 */
public enum Augment implements StringIdentifiable {
    /** Сила за счёт расхода маны. */
    POWER("power", 0.20f, 0.25f, 0.0f, 0.0f, 0),
    /** Дешевле, но слабее. */
    EFFICIENCY("efficiency", -0.10f, -0.20f, 0.0f, 0.0f, 0),
    /** Быстрая перезарядка за счёт маны. */
    HASTE("haste", 0.0f, 0.15f, -0.25f, 0.0f, 0),
    /** Дальность прицеливания и площадных эффектов. */
    REACH("reach", 0.0f, 0.10f, 0.0f, 0.5f, 0),
    /** Больше снарядов, но каждый слабее. */
    SPLIT("split", -0.45f, 0.20f, 0.10f, 0.0f, 2),
    /** Шанс применить заклинание дважды бесплатно. */
    ECHO("echo", 0.0f, 0.30f, 0.20f, 0.0f, 0);

    /** Шанс повторного применения у руны эха. */
    public static final float ECHO_CHANCE = 0.25f;

    public static final Codec<Augment> CODEC = StringIdentifiable.createCodec(Augment::values);

    public static final PacketCodec<ByteBuf, Augment> PACKET_CODEC =
            PacketCodecs.STRING.xmap(Augment::byName, Augment::asString);

    /** Разбор по строковому имени; неизвестное имя даёт {@link #POWER}. */
    public static Augment byName(String name) {
        for (Augment augment : values()) {
            if (augment.name.equals(name)) {
                return augment;
            }
        }
        return POWER;
    }

    private final String name;
    private final float power;
    private final float cost;
    private final float cooldown;
    private final float range;
    private final int extraShots;

    Augment(String name, float power, float cost, float cooldown, float range, int extraShots) {
        this.name = name;
        this.power = power;
        this.cost = cost;
        this.cooldown = cooldown;
        this.range = range;
        this.extraShots = extraShots;
    }

    public float power() {
        return power;
    }

    public float cost() {
        return cost;
    }

    public float cooldown() {
        return cooldown;
    }

    public float range() {
        return range;
    }

    public int extraShots() {
        return extraShots;
    }

    public String itemName() {
        return "augment_" + name;
    }

    public Text displayName() {
        return Text.translatable("item.arcanum." + itemName()).formatted(Formatting.LIGHT_PURPLE);
    }

    @Override
    public String asString() {
        return name;
    }
}
