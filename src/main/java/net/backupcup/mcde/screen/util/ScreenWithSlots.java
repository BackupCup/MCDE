package net.backupcup.mcde.screen.util;

import java.util.Optional;

import net.backupcup.mcde.util.SlotPosition;

public interface ScreenWithSlots {
    Optional<SlotPosition> getOpened();
    void setOpened(Optional<SlotPosition> opened);
}
