package net.ironman.client;

import net.ironman.suit.Suit;
import net.ironman.suit.SuitTier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Реактивный рывок.
 *
 * <p>Движение игрока в Minecraft считает клиент, поэтому рывок живёт здесь:
 * в полёте «вперёд + бег» разгоняет игрока вдоль взгляда — хоть вверх,
 * хоть в пике. Сервер видит только результат и списывает энергию по скорости.
 */
public final class FlightController {
    private FlightController() {
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !player.getAbilities().flying || !ClientSuitState.flightArmed()) {
            return;
        }
        SuitTier tier = Suit.tierOf(player);
        if (tier == null || ClientSuitState.energy() <= 0) {
            return;
        }
        boolean boosting = client.options.forwardKey.isPressed() && client.options.sprintKey.isPressed();
        if (!boosting) {
            return;
        }

        Vec3d look = player.getRotationVector();
        Vec3d velocity = player.getVelocity().add(look.multiply(tier.boost()));
        double max = tier.maxSpeed();
        if (velocity.length() > max) {
            velocity = velocity.normalize().multiply(max);
        }
        player.setVelocity(velocity);
    }
}
