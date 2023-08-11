package net.backupcup.mcde.screen.util;

import java.util.Optional;

import net.backupcup.mcde.util.Slots;

public interface ScreenWithSlots {
    Optional<Slots> getOpened();
    void setOpened(Optional<Slots> opened);
}
