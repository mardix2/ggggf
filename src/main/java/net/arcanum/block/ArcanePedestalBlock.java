package net.arcanum.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.block.ShapeContext;

/**
 * Аркановый постамент.
 *
 * <p>Не хранит предметы: его задача — усиливать
 * {@link InfusionAltarBlock алтарь превращения}. Рецепты высоких ступеней
 * требуют, чтобы вокруг алтаря стояло нужное число постаментов.
 */
public class ArcanePedestalBlock extends Block {

    public static final MapCodec<ArcanePedestalBlock> CODEC = createCodec(ArcanePedestalBlock::new);

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
            Block.createCuboidShape(5.0, 3.0, 5.0, 11.0, 12.0, 11.0),
            Block.createCuboidShape(3.0, 12.0, 3.0, 13.0, 15.0, 13.0));

    public ArcanePedestalBlock(Settings settings) {
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
}
