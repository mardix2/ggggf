package net.ironman.suit;

/** Команды, которые клиент отправляет костюму по горячим клавишам. */
public enum SuitAction {
    PRIMARY,
    UNIBEAM,
    MISSILES,
    TOGGLE_FLIGHT,
    TOGGLE_NIGHT_VISION;

    public static SuitAction byOrdinal(int ordinal) {
        SuitAction[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
