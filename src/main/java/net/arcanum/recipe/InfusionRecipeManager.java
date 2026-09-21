package net.arcanum.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.arcanum.Arcanum;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Загружает рецепты алтаря из ресурс-паков (и, значит, из датапаков игроков).
 */
public final class InfusionRecipeManager implements SimpleSynchronousResourceReloadListener {

    private static final String DIRECTORY = "infusion";
    /** Рецепты, отсортированные от самых «сложных» к простым. */
    private static List<InfusionRecipe> sorted = List.of();

    public static void init() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new InfusionRecipeManager());
    }

    @Override
    public Identifier getFabricId() {
        return Arcanum.id("infusion_recipes");
    }

    @Override
    public void reload(ResourceManager manager) {
        List<InfusionRecipe> loaded = new ArrayList<>();
        Map<Identifier, Resource> resources =
                manager.findResources(DIRECTORY, path -> path.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().getReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                InfusionRecipe.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> Arcanum.LOGGER.error(
                                "Не удалось прочитать рецепт алтаря {}: {}", entry.getKey(), error))
                        .ifPresent(loaded::add);
            } catch (IOException | RuntimeException e) {
                Arcanum.LOGGER.error("Ошибка чтения рецепта алтаря {}", entry.getKey(), e);
            }
        }

        // Сначала пробуем сложные рецепты: иначе набор на «посох архимага»
        // сработает как рецепт на обычную пыль, у которой ингредиентов меньше.
        loaded.sort(Comparator.comparingInt(InfusionRecipe::weight).reversed());
        sorted = List.copyOf(loaded);

        Arcanum.LOGGER.info("Загружено рецептов алтаря: {}", sorted.size());
    }

    /** Первый подходящий рецепт для набора предметов у алтаря. */
    public static Optional<InfusionRecipe> find(Map<Item, Integer> available, int pedestals) {
        for (InfusionRecipe recipe : sorted) {
            if (recipe.matches(available, pedestals)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /** Все загруженные обряды — их показывает команда {@code /arcanum infusion}. */
    public static List<InfusionRecipe> all() {
        return sorted;
    }
}
