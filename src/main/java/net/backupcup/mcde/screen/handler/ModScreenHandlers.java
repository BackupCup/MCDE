package net.backupcup.mcde.screen.handler;

import java.util.Optional;

import net.backupcup.mcde.MCDE;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public class ModScreenHandlers {
    public static ScreenHandlerType<RunicTableScreenHandler> RUNIC_TABLE_SCREEN_HANDLER = new ScreenHandlerType<>(RunicTableScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
    public static ScreenHandlerType<RollBenchScreenHandler> ROLL_BENCH_SCREEN_HANDLER = new ScreenHandlerType<>(RollBenchScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
    public static ExtendedScreenHandlerType<GildingFoundryScreenHandler, Optional<RegistryEntry<Enchantment>>> GILDING_FOUNDRY_SCREEN_HANDLER =
        new ExtendedScreenHandlerType<>(GildingFoundryScreenHandler::new, GildingFoundryScreenHandler.GENERATED_ENCHANTMENT_CODEC);

    public static void registerAllScreenHandlers() {
        registerScreenHandler("runic_table", RUNIC_TABLE_SCREEN_HANDLER);
        registerScreenHandler("roll_bench", ROLL_BENCH_SCREEN_HANDLER);
        registerScreenHandler("gilding_foundry", GILDING_FOUNDRY_SCREEN_HANDLER);
    }

    private static void registerScreenHandler(String id, ScreenHandlerType<?> type) {
        Registry.register(Registries.SCREEN_HANDLER, MCDE.id(id), type);
    }
}
