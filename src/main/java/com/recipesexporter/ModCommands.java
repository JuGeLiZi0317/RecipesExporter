package com.recipesexporter;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ExportRecipeCommand.register(dispatcher);
        RecipesHelperCommand.register(dispatcher);
    }
}
