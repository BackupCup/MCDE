package net.backupcup.mcde.screen.handler;

import net.backupcup.mcde.MCDEnchantments;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModScreenHandlers {
    public static ScreenHandlerType<RunicTableScreenHandler> RUNIC_TABLE_SCREEN_HANDLER = new ScreenHandlerType<>(RunicTableScreenHandler::new);
    public static ScreenHandlerType<RollBenchScreenHandler> ROLL_BENCH_SCREEN_HANDLER = new ScreenHandlerType<>(RollBenchScreenHandler::new);
    public static ExtendedScreenHandlerType<GildingFoundryScreenHandler> GILDING_FOUNDRY_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(GildingFoundryScreenHandler::new);

    public static void registerAllScreenHandlers() {
        registerScreenHandler("runic_table", RUNIC_TABLE_SCREEN_HANDLER);
        registerScreenHandler("roll_bench", ROLL_BENCH_SCREEN_HANDLER);
        registerScreenHandler("gilding_foundry", GILDING_FOUNDRY_SCREEN_HANDLER);
    }

    private static void registerScreenHandler(String id, ScreenHandlerType<?> type) {
        Registry.register(Registry.SCREEN_HANDLER, Identifier.of(MCDEnchantments.MOD_ID, id), type);
    }
}
