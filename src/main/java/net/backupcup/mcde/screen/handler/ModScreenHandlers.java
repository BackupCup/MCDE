package net.backupcup.mcde.screen.handler;

import net.minecraft.screen.ScreenHandlerType;

public class ModScreenHandlers {
    public static ScreenHandlerType<RunicTableScreenHandler> RUNIC_TABLE_SCREEN_HANDLER;
    public static ScreenHandlerType<RollBenchScreenHandler> ROLL_BENCH_SCREEN_HANDLER;
    public static ScreenHandlerType<GildingFoundryScreenHandler> GILDING_FOUNDRY_SCREEN_HANDLER;

    public static void registerAllScreenHandlers() {
        RUNIC_TABLE_SCREEN_HANDLER = new ScreenHandlerType<>(RunicTableScreenHandler::new);
        ROLL_BENCH_SCREEN_HANDLER = new ScreenHandlerType<>(RollBenchScreenHandler::new);
        GILDING_FOUNDRY_SCREEN_HANDLER = new ScreenHandlerType<>(GildingFoundryScreenHandler::new);
    }
}
