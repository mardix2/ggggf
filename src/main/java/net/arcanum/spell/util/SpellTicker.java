package net.arcanum.spell.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Планировщик длительных эффектов.
 *
 * <p>Заклинания вроде «Метели» или «Ауры пламени» не держат игрока за кнопку:
 * они один раз ставят задачу, а та сама отрабатывает нужное число тиков.
 * Задачи живут только в памяти сервера — перезапуск их обрывает, и это
 * осознанный компромисс ради простоты.
 */
public final class SpellTicker {
    private SpellTicker() {
    }

    /** Шаг длительного эффекта. Возврат {@code false} прекращает задачу досрочно. */
    @FunctionalInterface
    public interface Task {
        boolean run(int elapsedTicks);
    }

    private static final class Entry {
        final ServerWorld world;
        final Task task;
        final int interval;
        final int duration;
        int elapsed;

        Entry(ServerWorld world, Task task, int interval, int duration) {
            this.world = world;
            this.task = task;
            this.interval = Math.max(1, interval);
            this.duration = duration;
        }
    }

    private static final List<Entry> TASKS = new ArrayList<>();
    /** Задачи, добавленные во время обхода, — чтобы не ловить ConcurrentModificationException. */
    private static final List<Entry> PENDING = new ArrayList<>();
    private static boolean ticking;

    /**
     * @param durationTicks сколько тиков задача живёт
     * @param interval      через сколько тиков вызывать {@code task}
     */
    public static void schedule(ServerWorld world, int durationTicks, int interval, Task task) {
        Entry entry = new Entry(world, task, interval, durationTicks);
        if (ticking) {
            PENDING.add(entry);
        } else {
            TASKS.add(entry);
        }
    }

    public static void tick(MinecraftServer server) {
        if (TASKS.isEmpty()) {
            return;
        }
        ticking = true;
        try {
            Iterator<Entry> iterator = TASKS.iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                if (server.getWorld(entry.world.getRegistryKey()) == null) {
                    // Мир выгружен — задача больше не имеет смысла.
                    iterator.remove();
                    continue;
                }
                entry.elapsed++;
                if (entry.elapsed % entry.interval == 0 && !entry.task.run(entry.elapsed)) {
                    iterator.remove();
                    continue;
                }
                if (entry.elapsed >= entry.duration) {
                    iterator.remove();
                }
            }
        } finally {
            ticking = false;
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
    }

    /** Сбрасывает задачи при остановке сервера, чтобы они не пережили перезаход в мир. */
    public static void clear() {
        TASKS.clear();
        PENDING.clear();
    }
}
