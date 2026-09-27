package net.ironman.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.ironman.IronMan;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/** Горячие клавиши костюма. Переназначаются в «Управлении» → «Железный человек». */
public final class IronManKeys {
    private IronManKeys() {
    }

    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(IronMan.id("suit"));

    /** Включить/выключить полётные системы. */
    public static final KeyBinding FLIGHT = register("key.ironman.flight", GLFW.GLFW_KEY_H);
    /** Репульсор (Mark I — огнемёт). Можно держать. */
    public static final KeyBinding PRIMARY = register("key.ironman.primary", GLFW.GLFW_KEY_R);
    /** Юнибим из реактора в груди. */
    public static final KeyBinding UNIBEAM = register("key.ironman.unibeam", GLFW.GLFW_KEY_G);
    /** Залп микроракет. */
    public static final KeyBinding MISSILES = register("key.ironman.missiles", GLFW.GLFW_KEY_V);
    /** Ночное зрение шлема. */
    public static final KeyBinding NIGHT_VISION = register("key.ironman.night_vision", GLFW.GLFW_KEY_N);
    /** Показать/скрыть HUD J.A.R.V.I.S. */
    public static final KeyBinding HUD = register("key.ironman.hud", GLFW.GLFW_KEY_J);

    private static KeyBinding register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, key, CATEGORY));
    }

    public static void init() {
        // Обращение к классу запускает статическую инициализацию.
    }
}
