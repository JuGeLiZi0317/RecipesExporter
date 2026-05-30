package com.recipesexporter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.HashSet;
import java.util.Set;

public class RecipesHelperCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("recipeshelper")
                        .requires(source -> source.hasPermission(2)) // OP level 2
                        .then(Commands.literal("gettypes")
                                .then(Commands.argument("modid", StringArgumentType.word())
                                        .executes(RecipesHelperCommand::executeGetTypes)
                                )
                        )
        );
    }

    private static int executeGetTypes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String modid = StringArgumentType.getString(context, "modid");
        RecipeManager recipeManager = server.getRecipeManager();

        Set<String> recipeTypes = new HashSet<>();

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            ResourceLocation id = recipe.getId();
            if (!id.getNamespace().equals(modid)) {
                continue;
            }
            recipeTypes.add(recipe.getType().toString());
        }

        if (recipeTypes.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "§e[RecipesHelper] §7未找到模组 §e" + modid + "§7 的任何配方。"
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "§a[RecipesHelper] §7模组 §e" + modid + " §7包含以下配方类型:"), false);
            for (String type : recipeTypes) {
                source.sendSuccess(() -> Component.literal("  §8- §b" + type), false);
            }
        }

        return 1;
    }
}