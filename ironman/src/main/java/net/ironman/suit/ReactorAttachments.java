package net.ironman.suit;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.ironman.IronMan;

/** Attachment, в котором живёт дуговой реактор игрока. */
public final class ReactorAttachments {
    private ReactorAttachments() {
    }

    /** Реактор вживлён в грудь — поэтому переживает смерть. */
    public static final AttachmentType<ReactorData> REACTOR = AttachmentRegistry.create(IronMan.id("reactor"),
            builder -> builder
                    .persistent(ReactorData.CODEC)
                    .initializer(() -> ReactorData.EMPTY)
                    .copyOnDeath());

    /**
     * Метка «право полёта выдано костюмом». Сохраняется вместе с игроком,
     * чтобы после аварийного выключения сервера не остался бесплатный полёт.
     */
    public static final AttachmentType<Boolean> FLIGHT_GRANTED = AttachmentRegistry.create(
            IronMan.id("flight_granted"), builder -> builder.persistent(Codec.BOOL));

    public static void init() {
        // Обращение к полю выше запускает статическую инициализацию класса.
    }
}
