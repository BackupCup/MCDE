package net.backupcup.mcde.screen;

import net.minecraft.screen.ScreenHandlerType;

public class ModScreenHandlers {
    public static ScreenHandlerType<RunicTableScreenHandler> RUNIC_TABLE_SCREEN_HANDLER;
    public static ScreenHandlerType<RerollStationScreenHandler> REROLL_STATION_SCREEN_HANDLER;
    public static ScreenHandlerType<GildingFoundryScreenHandler> GILDING_FOUNDRY_SCREEN_HANDLER;

    public static void registerAllScreenHandlers() {
        RUNIC_TABLE_SCREEN_HANDLER = new ScreenHandlerType<>(RunicTableScreenHandler::new);
        REROLL_STATION_SCREEN_HANDLER = new ScreenHandlerType<>(RerollStationScreenHandler::new);
        GILDING_FOUNDRY_SCREEN_HANDLER = new ScreenHandlerType<>(GildingFoundryScreenHandler::new);
    }
}
