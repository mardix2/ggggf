package net.arcanum.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Аркановый светильник: щелчок гасит и зажигает его, редстоун не нужен. */
public class ArcaneLampBlock extends Block {

    public static final MapCodec<ArcaneLampBlock> CODEC = createCodec(ArcaneLampBlock::new);
    public static final BooleanProperty LIT = Properties.LIT;

    public ArcaneLampBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(LIT, true));
    }

    /** Используется в {@code Settings.luminance(...)} при регистрации блока. */
    public static int luminance(BlockState state) {
        return state.getOrEmpty(LIT).orElse(false) ? 15 : 0;
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient()) {
            boolean lit = !state.get(LIT);
            world.setBlockState(pos, state.with(LIT, lit), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS,
                    0.6f, lit ? 1.4f : 0.8f);
        }
        return ActionResult.SUCCESS;
    }
}
