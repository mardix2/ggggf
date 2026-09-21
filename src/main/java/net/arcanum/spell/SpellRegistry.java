package net.arcanum.spell;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр заклинаний.
 *
 * <p>Намеренно не ванильный {@code Registry}: заклинания полностью описаны
 * кодом и одинаковы на клиенте и сервере, поэтому синхронизировать реестр
 * не нужно — по сети ходят только идентификаторы.
 */
public final class SpellRegistry {
    private SpellRegistry() {
    }

    private static final Map<Identifier, Spell> SPELLS = new LinkedHashMap<>();

    static Spell register(Spell spell) {
        if (SPELLS.putIfAbsent(spell.id(), spell) != null) {
            throw new IllegalStateException("Заклинание уже зарегистрировано: " + spell.id());
        }
        return spell;
    }

    public static Spell get(Identifier id) {
        return id == null ? null : SPELLS.get(id);
    }

    public static Collection<Spell> all() {
        return Collections.unmodifiableCollection(SPELLS.values());
    }

    public static List<Identifier> ids() {
        return List.copyOf(SPELLS.keySet());
    }

    public static List<Spell> bySchool(SpellSchool school) {
        List<Spell> out = new ArrayList<>();
        for (Spell spell : SPELLS.values()) {
            if (spell.school() == school) {
                out.add(spell);
            }
        }
        return out;
    }

    public static int size() {
        return SPELLS.size();
    }
}
