package net.arcanum.client;

import net.arcanum.Arcanum;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Горячие клавиши.
 *
 * <p>Категории клавиш в Minecraft 1.21.6+ описываются объектом
 * {@link KeyBinding.Category}. Если вы собираете мод под более раннюю
 * версию, замените категорию строкой {@code "key.categories.arcanum"}.
 */
public final class ArcanumKeys {
    private ArcanumKeys() {
    }

    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Arcanum.id("main"));

    /** Открыть книгу заклинаний. */
    public static final KeyBinding OPEN_SPELLBOOK = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.arcanum.spellbook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));

    /** Следующее изученное заклинание. */
    public static final KeyBinding NEXT_SPELL = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.arcanum.next_spell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));

    /** Предыдущее изученное заклинание. */
    public static final KeyBinding PREV_SPELL = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.arcanum.prev_spell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY));

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
