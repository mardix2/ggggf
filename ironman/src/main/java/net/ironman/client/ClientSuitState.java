package net.ironman.client;

import net.ironman.net.payload.SuitStatePayload;
import net.ironman.suit.ReactorTier;

/** То, что клиент знает о костюме со слов сервера. */
public final class ClientSuitState {
    private ClientSuitState() {
    }

    private static final SuitStatePayload EMPTY =
            new SuitStatePayload(0, 0, 0, false, true, 0, 0, 0);

    private static SuitStatePayload state = EMPTY;
    private static boolean hudEnabled = true;

    public static void update(SuitStatePayload payload) {
        state = payload;
    }

    public static void reset() {
        state = EMPTY;
    }

    public static ReactorTier reactor() {
        return ReactorTier.byOrdinal(state.reactor());
    }

    public static int energy() {
        return state.energy();
    }

    public static int capacity() {
        return state.capacity();
    }

    public static float fraction() {
        return state.capacity() > 0 ? state.energy() / (float) state.capacity() : 0.0f;
    }

    public static boolean flightArmed() {
        return state.flightArmed();
    }

    public static boolean nightVision() {
        return state.nightVision();
    }

    public static int primaryCooldown() {
        return state.primaryCooldown();
    }

    public static int unibeamCooldown() {
        return state.unibeamCooldown();
    }

    public static int missileCooldown() {
        return state.missileCooldown();
    }

    public static boolean hudEnabled() {
        return hudEnabled;
    }

    public static void toggleHud() {
        hudEnabled = !hudEnabled;
    }
}
