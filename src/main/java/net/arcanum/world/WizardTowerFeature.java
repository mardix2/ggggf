package net.arcanum.world;

import com.mojang.serialization.Codec;
import net.arcanum.Arcanum;
import net.arcanum.registry.ModBlocks;
import net.arcanum.spell.SpellSchool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Башня павшего мага.
 *
 * <p>Сделана обычной фичей генерации, а не структурой из NBT: так мод не
 * тащит за собой бинарные файлы с версией формата данных, а постройка
 * подстраивается под рельеф прямо при генерации.
 *
 * <p>Внутри — работающий алтарь превращения с постаментами, источник маны
 * наверху, рунные камни всех семи школ в стенах и сундук с наградой.
 */
public class WizardTowerFeature extends Feature<DefaultFeatureConfig> {

    /** Ключ таблицы добычи сундука; сама таблица лежит в датапаке мода. */
    public static final RegistryKey<LootTable> LOOT = RegistryKey.of(RegistryKeys.LOOT_TABLE,
            Arcanum.id("chests/wizard_tower"));

    private static final int RADIUS = 4;
    private static final int HEIGHT = 15;
    /** Насколько неровным может быть пятно под башней. */
    private static final int MAX_SLOPE = 3;

    public WizardTowerFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        BlockPos base = findFootprint(world, origin);
        if (base == null) {
            return false;
        }

        buildFoundation(world, random, base);
        buildWalls(world, random, base);
        buildStairs(world, base);
        buildInterior(world, random, base);
        buildRoof(world, random, base);
        return true;
    }

    /** Проверяет, что пятно под башню достаточно ровное и не в воде. */
    private BlockPos findFootprint(StructureWorldAccess world, BlockPos origin) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int dx = -RADIUS; dx <= RADIUS; dx += RADIUS) {
            for (int dz = -RADIUS; dz <= RADIUS; dz += RADIUS) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z);
                lowest = Math.min(lowest, y);
                highest = Math.max(highest, y);

                BlockState ground = world.getBlockState(new BlockPos(x, y - 1, z));
                if (ground.isLiquid() || ground.isAir()) {
                    return null;
                }
            }
        }
        if (highest - lowest > MAX_SLOPE) {
            return null;
        }
        return new BlockPos(origin.getX(), lowest, origin.getZ());
    }

    // ------------------------------------------------------------------
    //  Части постройки
    // ------------------------------------------------------------------

    private BlockState stone(Random random) {
        int roll = random.nextInt(10);
        if (roll < 5) {
            return Blocks.STONE_BRICKS.getDefaultState();
        }
        if (roll < 8) {
            return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
        }
        return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
    }

    private void place(StructureWorldAccess world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    private void buildFoundation(StructureWorldAccess world, Random random, BlockPos base) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }
                // Пол и «ножка» вниз, чтобы башня не висела над склоном.
                place(world, base.add(dx, 0, dz), stone(random));
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos below = base.add(dx, -dy, dz);
                    if (world.getBlockState(below).isAir() || world.getBlockState(below).isLiquid()) {
                        place(world, below, stone(random));
                    }
                }
            }
        }
    }

    private void buildWalls(StructureWorldAccess world, Random random, BlockPos base) {
        for (int dy = 1; dy <= HEIGHT; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    int distance = dx * dx + dz * dz;
                    BlockPos pos = base.add(dx, dy, dz);
                    if (distance > RADIUS * RADIUS) {
                        continue;
                    }
                    boolean wall = distance > (RADIUS - 1) * (RADIUS - 1);
                    if (!wall) {
                        place(world, pos, Blocks.AIR.getDefaultState());
                        continue;
                    }
                    // Окна: узкие бойницы на трёх уровнях.
                    boolean window = (dy == 5 || dy == 9 || dy == 13)
                            && (dx == 0 || dz == 0);
                    place(world, pos, window ? Blocks.AIR.getDefaultState() : stone(random));
                }
            }
        }
        // Дверной проём.
        place(world, base.add(0, 1, RADIUS), Blocks.AIR.getDefaultState());
        place(world, base.add(0, 2, RADIUS), Blocks.AIR.getDefaultState());
    }

    private void buildStairs(StructureWorldAccess world, BlockPos base) {
        // Винтовая лестница вдоль внутренней стены: по четверти оборота на шаг.
        BlockPos[] ring = {
                new BlockPos(RADIUS - 1, 0, 0), new BlockPos(0, 0, RADIUS - 1),
                new BlockPos(-(RADIUS - 1), 0, 0), new BlockPos(0, 0, -(RADIUS - 1)),
        };
        Direction[] facing = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};

        for (int step = 0; step < HEIGHT - 2; step++) {
            BlockPos offset = ring[step % ring.length];
            BlockPos pos = base.add(offset.getX(), step + 1, offset.getZ());
            place(world, pos, Blocks.STONE_BRICK_STAIRS.getDefaultState()
                    .with(StairsBlock.FACING, facing[step % facing.length]));
        }
    }

    private void buildInterior(StructureWorldAccess world, Random random, BlockPos base) {
        // Алтарь с постаментами — башню можно использовать как готовую мастерскую.
        place(world, base.add(0, 1, 0), ModBlocks.INFUSION_ALTAR.getDefaultState());
        place(world, base.add(2, 1, 0), ModBlocks.ARCANE_PEDESTAL.getDefaultState());
        place(world, base.add(-2, 1, 0), ModBlocks.ARCANE_PEDESTAL.getDefaultState());
        place(world, base.add(0, 1, 2), ModBlocks.ARCANE_PEDESTAL.getDefaultState());
        place(world, base.add(0, 1, -2), ModBlocks.ARCANE_PEDESTAL.getDefaultState());

        // Рунные камни всех школ вживлены в стену на уровне пола.
        SpellSchool[] schools = SpellSchool.values();
        for (int index = 0; index < schools.length; index++) {
            double angle = index * Math.PI * 2 / schools.length;
            int dx = (int) Math.round(Math.cos(angle) * RADIUS);
            int dz = (int) Math.round(Math.sin(angle) * RADIUS);
            place(world, base.add(dx, 2, dz),
                    ModBlocks.RUNESTONES.get(schools[index]).getDefaultState());
        }

        place(world, base.add(RADIUS - 1, 1, RADIUS - 1), ModBlocks.ARCANE_LAMP.getDefaultState());
        place(world, base.add(-(RADIUS - 1), 1, -(RADIUS - 1)), ModBlocks.ARCANE_LAMP.getDefaultState());

        // Заброшенность: паутина по углам.
        for (int i = 0; i < 6; i++) {
            BlockPos pos = base.add(random.nextInt(RADIUS * 2 - 1) - RADIUS + 1,
                    1 + random.nextInt(HEIGHT - 3),
                    random.nextInt(RADIUS * 2 - 1) - RADIUS + 1);
            if (world.getBlockState(pos).isAir()) {
                place(world, pos, Blocks.COBWEB.getDefaultState());
            }
        }

        placeChest(world, random, base.add(2, 1, 2));
    }

    private void placeChest(StructureWorldAccess world, Random random, BlockPos pos) {
        place(world, pos, Blocks.CHEST.getDefaultState());
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(LOOT, random.nextLong());
        }
    }

    private void buildRoof(StructureWorldAccess world, Random random, BlockPos base) {
        int top = HEIGHT + 1;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }
                place(world, base.add(dx, top, dz), stone(random));
            }
        }

        // Зубцы по краю и рабочая площадка мага наверху.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int distance = dx * dx + dz * dz;
                if (distance <= (RADIUS - 1) * (RADIUS - 1) || distance > RADIUS * RADIUS) {
                    continue;
                }
                if ((dx + dz) % 2 == 0) {
                    place(world, base.add(dx, top + 1, dz), stone(random));
                }
            }
        }

        place(world, base.add(0, top + 1, 0), ModBlocks.MANA_FONT.getDefaultState());
        place(world, base.add(2, top + 1, 0), ModBlocks.TELEPORT_ANCHOR.getDefaultState());
    }
}
