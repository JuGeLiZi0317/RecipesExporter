package com.recipesexporter;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RecipesExporter.MOD_ID)
public class RecipesExporter {
    public static final String MOD_ID = "recipesexporter";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RecipesExporter() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        // Register command event listener
        forgeEventBus.addListener(this::onRegisterCommands);

        LOGGER.info("RecipesExporter initialized.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}