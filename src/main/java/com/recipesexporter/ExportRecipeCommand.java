package com.recipesexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExportRecipeCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("recipesexporter")
                        .requires(source -> source.hasPermission(2)) // OP level 2
                        .then(Commands.literal("export")
                                .then(Commands.argument("modid", StringArgumentType.word())
                                        .executes(ExportRecipeCommand::executeExport) // default: json_single
                                        .then(Commands.argument("export_type", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("json_single");
                                                    builder.suggest("json_split");
                                                    builder.suggest("kubejs");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ExportRecipeCommand::executeExport)
                                                .then(Commands.argument("recipe_type", StringArgumentType.string())
                                                        .executes(ExportRecipeCommand::executeExport)
                                                )
                                        )
                                )
                                .executes(context -> {
                                    context.getSource().sendFailure(
                                            Component.literal("§c[RecipesExporter] §7Usage: /recipesexporter export <modid> [export_type] [recipe_type]")
                                    );
                                    return 0;
                                })
                        )
        );
    }

    private static int executeExport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ExportMode mode;
        try {
            String typeStr = context.getArgument("export_type", String.class);
            mode = ExportMode.fromString(typeStr);
        } catch (IllegalArgumentException e) {
            // 'export_type' argument not provided, fall back to default
            mode = ExportMode.JSON_SINGLE;
        }

        // Try to get the optional 'recipe_type' argument (global filter)
        String recipeTypeFilter = null;
        try {
            recipeTypeFilter = context.getArgument("recipe_type", String.class);
        } catch (IllegalArgumentException e) {
            // 'recipe_type' argument not provided
        }

        return switch (mode) {
            case JSON_SINGLE -> exportAsSingleJson(context, recipeTypeFilter);
            case JSON_SPLIT -> exportAsSplitJson(context, recipeTypeFilter);
            case KUBEJS -> exportAsKubeJs(context, recipeTypeFilter);
        };
    }

    private static int exportAsSingleJson(CommandContext<CommandSourceStack> context, String recipeTypeFilter) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String modid = StringArgumentType.getString(context, "modid");
        RecipeManager recipeManager = server.getRecipeManager();

        // Build world-specific export path: <world_dir>/RecipesExporter/<modid>/ or <modid>_<filter>/
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        String folderName = (recipeTypeFilter != null) ? modid + "_" + recipeTypeFilter.replace(":", "_").replace("/", "_") : modid;
        Path exportDir = worldDir.resolve("RecipesExporter").resolve(folderName);

        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c[RecipesExporter] Failed to create export directory: " + e.getMessage()));
            return 0;
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failedRecipeIds = new ArrayList<>();

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            ResourceLocation id = recipe.getId();
            if (!id.getNamespace().equals(modid)) {
                continue; // Skip recipes not matching the given modid
            }
            if (recipeTypeFilter != null && !recipe.getType().toString().endsWith(recipeTypeFilter)) {
                continue; // Skip recipes not matching the type filter
            }
            JsonObject originalJson = getOriginalRecipeJson(recipe, server);
            if (originalJson == null) {
                failedRecipeIds.add(recipe.getId().toString());
                continue;
            }
            boolean success = exportRecipeToJson(originalJson, exportDir, id);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }

        failCount = failedRecipeIds.size() + failCount;
        int total = successCount + failCount;
        int finalSuccessCount = successCount;
        int finalTotal = total;
        String filterSuffix = (recipeTypeFilter != null) ? " (filtered by type: " + recipeTypeFilter + ")" : "";
        source.sendSuccess(() -> Component.literal(
                "§a[RecipesExporter] §7Exported §e" + finalSuccessCount + "§7/§e" + finalTotal + "§7 recipes from mod §e" + modid + "§7 to §e" + exportDir.toAbsolutePath() + filterSuffix
        ), true);

        if (failCount > 0) {
            String logFilename = writeFailedLogFile(exportDir, modid, recipeTypeFilter, failedRecipeIds);
            source.sendFailure(Component.literal("§c[RecipesExporter] §7Failed to export §e" + failCount + "§7 recipes. Details saved to §e" + logFilename + "§7"));
        }

        return 1;
    }

    private static int exportAsSplitJson(CommandContext<CommandSourceStack> context, String recipeTypeFilter) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String modid = StringArgumentType.getString(context, "modid");
        RecipeManager recipeManager = server.getRecipeManager();

        // Build world-specific base path: <world_dir>/RecipesExporter/<modid>/ or <modid>_<filter>/
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        String folderName = (recipeTypeFilter != null) ? modid + "_" + recipeTypeFilter.replace(":", "_").replace("/", "_") : modid;
        Path baseDir = worldDir.resolve("RecipesExporter").resolve(folderName);

        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c[RecipesExporter] Failed to create base export directory: " + e.getMessage()));
            return 0;
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failedRecipeIds = new ArrayList<>();

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            ResourceLocation id = recipe.getId();
            if (!id.getNamespace().equals(modid)) {
                continue;
            }
            if (recipeTypeFilter != null && !recipe.getType().toString().endsWith(recipeTypeFilter)) {
                continue;
            }

            // Build a subfolder name from the recipe type, replacing ':' with '_' to avoid filesystem issues
            String typeSubfolder = recipe.getType().toString().replace(":", "_");
            Path typeDir = baseDir.resolve(typeSubfolder);

            try {
                Files.createDirectories(typeDir);
            } catch (IOException e) {
                source.sendFailure(Component.literal("§c[RecipesExporter] Failed to create type subfolder '" + typeSubfolder + "': " + e.getMessage()));
                failCount++;
                continue;
            }

            JsonObject originalJson = getOriginalRecipeJson(recipe, server);
            if (originalJson == null) {
                failedRecipeIds.add(recipe.getId().toString());
                continue;
            }
            boolean success = exportRecipeToJson(originalJson, typeDir, id);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }

        failCount = failedRecipeIds.size() + failCount;
        int total = successCount + failCount;
        int finalSuccessCount = successCount;
        int finalTotal = total;
        String filterSuffix = (recipeTypeFilter != null) ? " (filtered by type: " + recipeTypeFilter + ")" : "";
        source.sendSuccess(() -> Component.literal(
                "§a[RecipesExporter] §7Exported §e" + finalSuccessCount + "§7/§e" + finalTotal + "§7 recipes from mod §e" + modid + "§7 to §e" + baseDir.toAbsolutePath() + "§7 (split by type)" + filterSuffix
        ), true);

        if (failCount > 0) {
            String logFilename = writeFailedLogFile(baseDir, modid, recipeTypeFilter, failedRecipeIds);
            source.sendFailure(Component.literal("§c[RecipesExporter] §7Failed to export §e" + failCount + "§7 recipes. Details saved to §e" + logFilename + "§7"));
        }

        return 1;
    }

    private static int exportAsKubeJs(CommandContext<CommandSourceStack> context, String recipeTypeFilter) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String modid = StringArgumentType.getString(context, "modid");
        RecipeManager recipeManager = server.getRecipeManager();

        // Output: <world_dir>/RecipesExporter/<modid>_recipes.js or <modid>_recipes_<filter>.js
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path exportDir = worldDir.resolve("RecipesExporter");
        String filename = (recipeTypeFilter != null)
                ? modid + "_recipes_" + recipeTypeFilter.replace(":", "_").replace("/", "_") + ".js"
                : modid + "_recipes.js";
        Path outputFile = exportDir.resolve(filename);

        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c[RecipesExporter] Failed to create export directory: " + e.getMessage()));
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ServerEvents.recipes(event => {\n");

        int successCount = 0;
        int failCount = 0;
        List<String> failedRecipeIds = new ArrayList<>();

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            ResourceLocation id = recipe.getId();
            if (!id.getNamespace().equals(modid)) {
                continue;
            }
            if (recipeTypeFilter != null && !recipe.getType().toString().endsWith(recipeTypeFilter)) {
                continue;
            }

            JsonObject originalJson = getOriginalRecipeJson(recipe, server);
            if (originalJson == null) {
                failedRecipeIds.add(recipe.getId().toString());
                continue;
            }

            try {
                String jsonStr = GSON.toJson(originalJson);

                // Indent: 2 spaces for the body lines
                sb.append("  // ").append(id).append("\n");
                sb.append("  event.custom(\n");
                // Indent each line of the pretty-printed JSON with an extra 4 spaces
                String indentedJson = jsonStr.replace("\n", "\n    ");
                sb.append("    ").append(indentedJson).append("\n");
                sb.append("  );\n\n");

                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }

        sb.append("});\n");

        // Write the entire script to file
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c[RecipesExporter] Failed to write KubeJS script: " + e.getMessage()));
            return 0;
        }

        failCount = failedRecipeIds.size() + failCount;
        int total = successCount + failCount;
        int finalSuccessCount = successCount;
        int finalTotal = total;
        String filterSuffix = (recipeTypeFilter != null) ? " (filtered by type: " + recipeTypeFilter + ")" : "";
        source.sendSuccess(() -> Component.literal(
                "§a[RecipesExporter] §7Exported §e" + finalSuccessCount + "§7/§e" + finalTotal + "§7 recipes from mod §e" + modid + "§7 to §e" + outputFile.toAbsolutePath() + filterSuffix
        ), true);

        if (failCount > 0) {
            String logFilename = writeFailedLogFile(exportDir, modid, recipeTypeFilter, failedRecipeIds);
            source.sendFailure(Component.literal("§c[RecipesExporter] §7Failed to export §e" + failCount + "§7 recipes. Details saved to §e" + logFilename + "§7"));
        }

        return 1;
    }

    /**
     * Reads the original recipe JSON file from the mod's resource pack via ResourceManager.
     * Falls back gracefully by returning null if the resource is not found or cannot be read.
     */
    private static JsonObject getOriginalRecipeJson(Recipe<?> recipe, MinecraftServer server) {
        ResourceLocation recipeId = recipe.getId();
        ResourceLocation fileLocation = new ResourceLocation(
                recipeId.getNamespace(),
                "recipes/" + recipeId.getPath() + ".json"
        );
        ResourceManager resourceManager = server.getResourceManager();

        try (java.io.BufferedReader reader = resourceManager.getResource(fileLocation)
                .orElseThrow(() -> new java.io.IOException("Recipe resource not found: " + fileLocation))
                .openAsReader()) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * Serializes a single recipe JsonObject to a JSON file under exportDir, using the recipe's path as the filename.
     * Returns true on success, false on failure.
     */
    private static boolean exportRecipeToJson(JsonObject json, Path exportDir, ResourceLocation id) {

        // Build output filename: path.json (namespace is already in the folder name)
        String filename = id.getPath().replace("/", "_") + ".json";
        Path outputPath = exportDir.resolve(filename);

        try {
            Files.writeString(outputPath, GSON.toJson(json));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Writes a log file listing all failed recipe IDs that were not found as physical JSON files.
     * The log file is placed in the same export directory.
     *
     * @return the log filename (without directory), for display in chat feedback
     */
    private static String writeFailedLogFile(Path exportDir, String modid, String recipeTypeFilter, List<String> failedRecipeIds) {
        String logFilename = (recipeTypeFilter != null)
                ? modid + "_" + recipeTypeFilter.replace(":", "_").replace("/", "_") + "_recipes_failed_log.txt"
                : modid + "_recipes_failed_log.txt";
        Path logFile = exportDir.resolve(logFilename);

        try (BufferedWriter writer = Files.newBufferedWriter(logFile)) {
            writer.write("Failed Recipes Count: " + failedRecipeIds.size());
            writer.newLine();
            writer.write("---");
            writer.newLine();
            for (String id : failedRecipeIds) {
                writer.write(id);
                writer.newLine();
            }
        } catch (IOException e) {
            // Swallow; the chat message already told the user we attempted to write the log
        }

        return logFilename;
    }

    private enum ExportMode {
        JSON_SINGLE("json_single"),
        JSON_SPLIT("json_split"),
        KUBEJS("kubejs");

        private final String id;

        ExportMode(String id) {
            this.id = id;
        }

        static ExportMode fromString(String id) {
            for (ExportMode mode : values()) {
                if (mode.id.equals(id)) {
                    return mode;
                }
            }
            return JSON_SINGLE;
        }
    }
}