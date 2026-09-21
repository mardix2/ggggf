package net.arcanum.block;

import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Рунный камень школы. Сейчас это декоративный блок с «своим» цветом искр;
 * он же служит опознавательным знаком для построек и рецептов.
 */
public class RunestoneBlock extends Block {

    private final SpellSchool school;

    public RunestoneBlock(SpellSchool school, Settings settings) {
        super(settings);
        this.school = school;
    }

    public SpellSchool school() {
        return school;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + 1.02;
        double z = pos.getZ() + random.nextDouble();
        Fx.clientParticle(world, Fx.dust(school.color(), 0.8f), x, y, z, 0.0, 0.01, 0.0);
    }
}
