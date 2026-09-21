package net.arcanum.entity;

import net.arcanum.Arcanum;
import net.arcanum.ArcanumConfig;
import net.arcanum.mana.ManaManager;
import net.arcanum.recipe.InfusionRecipe;
import net.arcanum.recipe.InfusionRecipeManager;
import net.arcanum.registry.ModBlockEntities;
import net.arcanum.registry.ModBlocks;
import net.arcanum.item.WandItem;
import net.arcanum.registry.ModComponents;
import net.arcanum.spell.Spell;
import net.arcanum.spell.SpellRegistry;
import net.arcanum.spell.Augments;
import net.arcanum.spell.SpellSchool;
import net.arcanum.spell.util.Fx;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Мозги алтаря превращения.
 *
 * <p>Состояние обряда живёт только в памяти: обряд длится три секунды, и
 * сохранять его между загрузками чанка незачем. Ингредиенты при этом
 * остаются лежать на земле до самого конца обряда — если что-то прервётся,
 * игрок ничего не потеряет.
 */
public class InfusionAltarBlockEntity extends BlockEntity {

    private static final int PEDESTAL_SCAN_RADIUS = 4;

    private int ritualTicks = -1;
    private InfusionRecipe activeRecipe;
    private UUID caster;
    private float paidMana;

    public InfusionAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSION_ALTAR, pos, state);
    }

    private boolean ritualActive() {
        return ritualTicks >= 0 && activeRecipe != null;
    }

    private Vec3d center() {
        return Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0);
    }

    // ------------------------------------------------------------------
    //  Взаимодействие
    // ------------------------------------------------------------------

    public ActionResult onUse(PlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        if (ritualActive()) {
            player.sendMessage(Text.translatable("message.arcanum.altar.busy")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        Map<Item, Integer> available = gather(serverWorld);
        if (available.isEmpty()) {
            player.sendMessage(Text.translatable("message.arcanum.altar.empty")
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        int pedestals = countPedestals(serverWorld);
        Optional<InfusionRecipe> match = InfusionRecipeManager.find(available, pedestals);
        if (match.isEmpty()) {
            player.sendMessage(Text.translatable("message.arcanum.altar.no_recipe", pedestals)
                    .formatted(Formatting.GRAY), true);
            return ActionResult.FAIL;
        }

        InfusionRecipe recipe = match.get();
        if (recipe.requiresWand() && recipe.addAugment().isPresent()) {
            ItemEntity wand = findWand(serverWorld);
            if (wand == null || !Augments.hasFreeSlot(wand.getStack())) {
                player.sendMessage(Text.translatable("message.arcanum.altar.no_slots")
                        .formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
        }
        if (recipe.mana() > 0 && !ManaManager.consume(player, recipe.mana())) {
            player.sendMessage(Text.translatable("message.arcanum.altar.no_mana", recipe.mana())
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        activeRecipe = recipe;
        ritualTicks = 0;
        caster = player.getUuid();
        paidMana = recipe.mana();

        Fx.sound(serverWorld, center(), SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f);
        return ActionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    //  Обряд
    // ------------------------------------------------------------------

    public void serverTick() {
        if (!ritualActive() || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        ritualTicks++;

        Vec3d center = center();
        pullItems(serverWorld, center);
        showProgress(serverWorld, center);

        if (ritualTicks >= ArcanumConfig.ALTAR_RITUAL_TICKS) {
            finish(serverWorld, center);
        }
    }

    private void pullItems(ServerWorld serverWorld, Vec3d center) {
        for (ItemEntity item : nearbyItems(serverWorld)) {
            Vec3d pull = center.subtract(item.getPos());
            if (pull.lengthSquared() > 0.25) {
                item.setVelocity(pull.normalize().multiply(0.12));
                item.velocityModified = true;
            }
            // Пока идёт обряд, ингредиенты не подобрать.
            item.setPickupDelay(40);
        }
    }

    private void showProgress(ServerWorld serverWorld, Vec3d center) {
        double progress = (double) ritualTicks / ArcanumConfig.ALTAR_RITUAL_TICKS;
        Fx.ring(serverWorld, center.add(0.0, 0.2, 0.0), 1.6 * (1.0 - progress) + 0.4,
                ParticleTypes.ENCHANT, 12);
        if (ritualTicks % 10 == 0) {
            Fx.sound(serverWorld, center, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    0.5f, 0.8f + (float) progress * 0.8f);
        }
    }

    private void finish(ServerWorld serverWorld, Vec3d center) {
        InfusionRecipe recipe = activeRecipe;
        Map<Item, Integer> available = gather(serverWorld);
        int pedestals = countPedestals(serverWorld);

        if (!recipe.matches(available, pedestals)) {
            abort(serverWorld, center);
            return;
        }

        ItemStack result;
        if (recipe.requiresWand()) {
            result = transformWand(serverWorld, recipe);
            if (result == null) {
                abort(serverWorld, center);
                return;
            }
        } else {
            result = recipe.result().toStack();
        }

        if (result.isEmpty()) {
            // Обряд без результата — ошибка в датапаке; ингредиенты не трогаем.
            Arcanum.LOGGER.warn("Обряд алтаря не дал результата, проверьте поле result");
            abort(serverWorld, center);
            return;
        }

        consume(serverWorld, recipe);
        recipe.scrollSchool().ifPresent(school -> inscribe(serverWorld, result, school));
        ItemEntity drop = new ItemEntity(serverWorld, center.x, center.y + 0.6, center.z, result);
        drop.setVelocity(0.0, 0.15, 0.0);
        drop.setPickupDelay(10);
        serverWorld.spawnEntity(drop);

        Fx.sphere(serverWorld, center, 1.0, ParticleTypes.END_ROD, 40);
        Fx.sound(serverWorld, center, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        reset();
    }

    /**
     * Превращает брошенный посох: вплавляет руну или вынимает все разом.
     *
     * <p>Посох не «тратится», а заменяется собственной копией — иначе
     * пропали бы уже впечатанные в него руны и выбранное название.
     */
    private ItemStack transformWand(ServerWorld serverWorld, InfusionRecipe recipe) {
        ItemEntity entity = findWand(serverWorld);
        if (entity == null) {
            return null;
        }
        ItemStack wand = entity.getStack();
        ItemStack result;
        if (recipe.clearAugments()) {
            result = Augments.cleared(wand);
        } else if (recipe.addAugment().isPresent() && Augments.hasFreeSlot(wand)) {
            result = Augments.with(wand, recipe.addAugment().get());
        } else {
            return null;
        }

        wand.decrement(1);
        if (wand.isEmpty()) {
            entity.discard();
        } else {
            entity.setStack(wand);
        }
        return result;
    }

    private ItemEntity findWand(ServerWorld serverWorld) {
        for (ItemEntity item : nearbyItems(serverWorld)) {
            if (item.getStack().getItem() instanceof WandItem) {
                return item;
            }
        }
        return null;
    }

    /**
     * Записывает в свиток случайное заклинание школы.
     *
     * <p>Приоритет у тех, которых заклинатель ещё не знает: иначе игрок,
     * выучивший половину школы, будет получать сплошные дубликаты.
     */
    private void inscribe(ServerWorld serverWorld, ItemStack scroll, SpellSchool school) {
        List<Spell> pool = SpellRegistry.bySchool(school);
        if (pool.isEmpty()) {
            return;
        }
        PlayerEntity player = caster == null ? null : serverWorld.getPlayerByUuid(caster);
        List<Spell> unknown = new ArrayList<>();
        if (player != null) {
            for (Spell spell : pool) {
                if (!ManaManager.data(player).knows(spell.id())) {
                    unknown.add(spell);
                }
            }
        }
        List<Spell> source = unknown.isEmpty() ? pool : unknown;
        Spell chosen = source.get(serverWorld.getRandom().nextInt(source.size()));
        scroll.set(ModComponents.SCROLL_SPELL, chosen.id());
    }

    /** Обряд сорвался: ингредиенты остались на земле, мана возвращается. */
    private void abort(ServerWorld serverWorld, Vec3d center) {
        Fx.burst(serverWorld, center, ParticleTypes.SMOKE, 25, 0.4, 0.02);
        Fx.sound(serverWorld, center, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.8f);

        if (paidMana > 0 && caster != null) {
            PlayerEntity player = serverWorld.getPlayerByUuid(caster);
            if (player != null) {
                ManaManager.give(player, paidMana);
                player.sendMessage(Text.translatable("message.arcanum.altar.interrupted")
                        .formatted(Formatting.RED), true);
            }
        }
        reset();
    }

    private void reset() {
        ritualTicks = -1;
        activeRecipe = null;
        caster = null;
        paidMana = 0.0f;
    }

    // ------------------------------------------------------------------
    //  Работа с предметами вокруг
    // ------------------------------------------------------------------

    private List<ItemEntity> nearbyItems(ServerWorld serverWorld) {
        double radius = ArcanumConfig.ALTAR_RADIUS;
        Vec3d center = center();
        Box box = Box.of(center, radius * 2, radius * 2, radius * 2);
        List<ItemEntity> items = new ArrayList<>();
        for (ItemEntity item : serverWorld.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (item.getPos().squaredDistanceTo(center) <= radius * radius) {
                items.add(item);
            }
        }
        return items;
    }

    private Map<Item, Integer> gather(ServerWorld serverWorld) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemEntity item : nearbyItems(serverWorld)) {
            ItemStack stack = item.getStack();
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private void consume(ServerWorld serverWorld, InfusionRecipe recipe) {
        Map<Item, Integer> needed = new HashMap<>();
        for (InfusionRecipe.Entry entry : recipe.inputs()) {
            needed.merge(entry.item(), entry.count(), Integer::sum);
        }

        for (ItemEntity item : nearbyItems(serverWorld)) {
            ItemStack stack = item.getStack();
            int remaining = needed.getOrDefault(stack.getItem(), 0);
            if (remaining <= 0) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.decrement(taken);
            needed.put(stack.getItem(), remaining - taken);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setStack(stack);
            }
        }
    }

    private int countPedestals(ServerWorld serverWorld) {
        int count = 0;
        for (BlockPos check : BlockPos.iterate(
                pos.add(-PEDESTAL_SCAN_RADIUS, -1, -PEDESTAL_SCAN_RADIUS),
                pos.add(PEDESTAL_SCAN_RADIUS, 1, PEDESTAL_SCAN_RADIUS))) {
            if (serverWorld.getBlockState(check).isOf(ModBlocks.ARCANE_PEDESTAL)) {
                count++;
            }
        }
        return count;
    }
}
