package net.backupcup.mcde.screen.util;

import java.util.Optional;

import net.backupcup.mcde.util.SlotPosition;
import net.minecraft.client.util.math.MatrixStack;

public interface ScreenWithSlots {
    Optional<SlotPosition> getOpened();
    void setOpened(Optional<SlotPosition> opened);
    void drawTexture(MatrixStack matrices, int x, int y, int u, int v, int width, int height);
}
