package net.arcanum.block;

import com.mojang.serialization.MapCodec;
import net.arcanum.mana.ManaAttachments;
import net.arcanum.spell.util.Fx;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;

/**
 * Якорь перемещения: щелчок по нему запоминает точку, куда переносит
 * заклинание «Возврат». Игрок может держать только один якорь одновременно.
 */
public class TeleportAnchorBlock extends Block {

    public static final MapCodec<TeleportAnchorBlock> CODEC = createCodec(TeleportAnchorBlock::new);

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 2.0, 14.0),
            Block.createCuboidShape(5.0, 2.0, 5.0, 11.0, 13.0, 11.0),
            Block.createCuboidShape(3.0, 13.0, 3.0, 13.0, 16.0, 13.0));

    public TeleportAnchorBlock(Settings settings) {
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
        GlobalPos anchor = new GlobalPos(world.getRegistryKey(), pos.up());
        player.setAttached(ManaAttachments.ANCHOR, anchor);
        player.sendMessage(Text.translatable("message.arcanum.anchor_bound",
                pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.LIGHT_PURPLE), true);

        if (world instanceof ServerWorld serverWorld) {
            Vec3d center = Vec3d.ofCenter(pos.up());
            Fx.ring(serverWorld, center, 0.9, ParticleTypes.PORTAL, 24);
            Fx.sound(serverWorld, center, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        double y = pos.getY() + 1.05;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        Fx.clientParticle(world, ParticleTypes.PORTAL, x, y, z, 0.0, 0.01, 0.0);
    }
}
