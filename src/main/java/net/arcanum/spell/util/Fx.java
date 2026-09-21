package net.arcanum.spell.util;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Обёртка над частицами и звуками.
 *
 * <p>Все вызовы «капризных» рендер-API собраны здесь специально: если
 * сигнатуры частиц в новой версии Minecraft поменяются, править придётся
 * только этот файл, а не полсотни заклинаний.
 */
public final class Fx {
    private Fx() {
    }

    /** Цветная пылинка заданного цвета 0xRRGGBB. */
    public static ParticleEffect dust(int color, float scale) {
        return new DustParticleEffect(color, scale);
    }

    /** Облако частиц в точке. */
    public static void burst(ServerWorld world, Vec3d pos, ParticleEffect particle,
                             int count, double spread, double speed) {
        world.spawnParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    /** Горизонтальное кольцо — базовый «след» площадных заклинаний. */
    public static void ring(ServerWorld world, Vec3d center, double radius,
                            ParticleEffect particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(particle, x, center.y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Кольцо, расходящееся по земле: рисуется по высоте ближайшей поверхности. */
    public static void groundRing(ServerWorld world, Vec3d center, double radius,
                                  ParticleEffect particle, int points) {
        ring(world, center.add(0.0, 0.1, 0.0), radius, particle, points);
    }

    /** Сфера точек — для взрывов и щитов. */
    public static void sphere(ServerWorld world, Vec3d center, double radius,
                              ParticleEffect particle, int points) {
        for (int i = 0; i < points; i++) {
            double theta = world.getRandom().nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * world.getRandom().nextDouble() - 1);
            double x = center.x + radius * Math.sin(phi) * Math.cos(theta);
            double y = center.y + radius * Math.cos(phi);
            double z = center.z + radius * Math.sin(phi) * Math.sin(theta);
            world.spawnParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Луч из точки в точку. */
    public static void beam(ServerWorld world, Vec3d from, Vec3d to, ParticleEffect particle, double step) {
        Vec3d delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3d dir = delta.multiply(1.0 / length);
        for (double d = 0.0; d < length; d += step) {
            Vec3d p = from.add(dir.multiply(d));
            world.spawnParticles(particle, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Спираль вокруг вертикальной оси — «каст» и ауры. */
    public static void helix(ServerWorld world, Vec3d base, double radius, double height,
                             ParticleEffect particle, int points, double turns) {
        for (int i = 0; i < points; i++) {
            double t = (double) i / points;
            double angle = t * Math.PI * 2 * turns;
            double x = base.x + Math.cos(angle) * radius;
            double z = base.z + Math.sin(angle) * radius;
            double y = base.y + t * height;
            world.spawnParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Конус в направлении взгляда — для «волн» и «дыханий». */
    public static void cone(ServerWorld world, Vec3d origin, Vec3d direction, double length,
                           double spreadDegrees, ParticleEffect particle, int points) {
        double spread = Math.toRadians(spreadDegrees);
        for (int i = 0; i < points; i++) {
            double d = world.getRandom().nextDouble() * length;
            double yaw = (world.getRandom().nextDouble() - 0.5) * spread;
            double pitch = (world.getRandom().nextDouble() - 0.5) * spread;
            Vec3d dir = direction.rotateY((float) yaw).add(0.0, Math.sin(pitch), 0.0).normalize();
            Vec3d p = origin.add(dir.multiply(d));
            world.spawnParticles(particle, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Частица, добавляемая на стороне клиента (из {@code randomDisplayTick}).
     *
     * <p>Вынесено сюда по той же причине, что и остальной файл: имя этого
     * метода в ванили менялось между версиями, и правится оно в одном месте.
     */
    public static void clientParticle(World world, ParticleEffect particle,
                                      double x, double y, double z,
                                      double vx, double vy, double vz) {
        world.addParticleClient(particle, x, y, z, vx, vy, vz);
    }

    public static void sound(ServerWorld world, Vec3d pos, SoundEvent event, float volume, float pitch) {
        world.playSound(null, pos.x, pos.y, pos.z, event, SoundCategory.PLAYERS, volume, pitch);
    }

    /** Звук со случайным разбросом высоты — чтобы серия кастов не звучала механически. */
    public static void soundVaried(ServerWorld world, Vec3d pos, SoundEvent event, float volume, float basePitch) {
        float pitch = MathHelper.clamp(basePitch + (world.getRandom().nextFloat() - 0.5f) * 0.2f, 0.5f, 2.0f);
        sound(world, pos, event, volume, pitch);
    }
}
