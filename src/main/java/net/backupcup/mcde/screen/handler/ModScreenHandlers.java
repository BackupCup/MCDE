package net.backupcup.mcde.screen.handler;

import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<RunicTableScreenHandler> RUNIC_TABLE_SCREEN_HANDLER = new ScreenHandlerType<>(RunicTableScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
    public static ScreenHandlerType<RollBenchScreenHandler> ROLL_BENCH_SCREEN_HANDLER = new ScreenHandlerType<>(RollBenchScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
    public static ScreenHandlerType<GildingFoundryScreenHandler> GILDING_FOUNDRY_SCREEN_HANDLER = new ScreenHandlerType<>(GildingFoundryScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

    public static void registerAllScreenHandlers() {
        registerScreenHandler("runic_table", RUNIC_TABLE_SCREEN_HANDLER);
        registerScreenHandler("roll_bench", ROLL_BENCH_SCREEN_HANDLER);
        registerScreenHandler("gilding_foundry", GILDING_FOUNDRY_SCREEN_HANDLER);
    }

    private static void registerScreenHandler(String id, ScreenHandlerType<?> type) {
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MCDEnchantments.MOD_ID, id), type);
    }
}
