package net.arcanum.block;

import com.mojang.serialization.MapCodec;
import net.arcanum.entity.InfusionAltarBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.ShapeContext;

/**
 * Алтарь превращения — центральный «верстак» мода.
 *
 * <p>Ингредиенты игрок бросает на землю рядом с алтарём (клавиша выброса),
 * после чего щёлкает по алтарю посохом. Такой способ выбран намеренно:
 * выброшенные предметы рисует сама ваниль, поэтому обряд выглядит нарядно,
 * а моду не нужен собственный рендерер блока.
 */
public class InfusionAltarBlock extends Block implements BlockEntityProvider {

    public static final MapCodec<InfusionAltarBlock> CODEC = createCodec(InfusionAltarBlock::new);

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
            Block.createCuboidShape(3.0, 4.0, 3.0, 13.0, 11.0, 13.0),
            Block.createCuboidShape(1.0, 11.0, 1.0, 15.0, 16.0, 15.0));

    public InfusionAltarBlock(Settings settings) {
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
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof InfusionAltarBlockEntity altar) {
            return altar.onUse(player);
        }
        return ActionResult.PASS;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InfusionAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return (tickWorld, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof InfusionAltarBlockEntity altar) {
                altar.serverTick();
            }
        };
    }
}
