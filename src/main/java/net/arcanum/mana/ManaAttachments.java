package net.arcanum.mana;

import net.arcanum.Arcanum;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.math.GlobalPos;

/** Регистрация attachment-а, в котором живут данные маны игрока. */
public final class ManaAttachments {
    private ManaAttachments() {
    }

    public static final AttachmentType<ManaData> MANA = AttachmentRegistry.<ManaData>builder()
            .persistent(ManaData.CODEC)
            .initializer(() -> ManaData.DEFAULT)
            .copyOnDeath()
            .buildAndRegister(Arcanum.id("mana"));

    /**
     * Привязанный «Якорь перемещения» — цель заклинания «Возврат».
     * Без инициализатора: отсутствие значения означает «якорь не установлен».
     */
    public static final AttachmentType<GlobalPos> ANCHOR = AttachmentRegistry.<GlobalPos>builder()
            .persistent(GlobalPos.CODEC)
            .copyOnDeath()
            .buildAndRegister(Arcanum.id("anchor"));

    public static void init() {
        // Обращение к полю выше вызывает статическую инициализацию класса.
    }
}
