package net.backupcup.mcd_enchantments.screen;

import net.minecraft.screen.ScreenHandlerType;

public class ModScreenHandlers {
    public static ScreenHandlerType<RunicTableScreenHandler> RUNIC_TABLE_SCREEN_HANDLER;

    public static void registerAllScreenHandlers() {
        RUNIC_TABLE_SCREEN_HANDLER = new ScreenHandlerType<>(RunicTableScreenHandler::new);
    }
}
