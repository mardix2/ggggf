package net.arcanum.block;

import com.mojang.serialization.MapCodec;
import net.arcanum.mana.ManaManager;
import net.arcanum.spell.util.Fx;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.ShapeContext;

/**
 * Источник маны: пока игрок стоит рядом, его мана восстанавливается
 * в несколько раз быстрее (см. {@link ManaManager}).
 */
public class ManaFontBlock extends Block {

    public static final MapCodec<ManaFontBlock> CODEC = createCodec(ManaFontBlock::new);

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
            Block.createCuboidShape(4.0, 3.0, 4.0, 12.0, 10.0, 12.0),
            Block.createCuboidShape(1.0, 10.0, 1.0, 15.0, 14.0, 15.0));

    public ManaFontBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient()) {
            player.sendMessage(Text.translatable("message.arcanum.mana_status",
                    (int) ManaManager.current(player), ManaManager.max(player))
                    .formatted(Formatting.AQUA), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Ленивые искры, поднимающиеся из чаши.
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
            double y = pos.getY() + 0.85;
            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
            Fx.clientParticle(world, ParticleTypes.END_ROD, x, y, z,
                    0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
        }
    }
}
