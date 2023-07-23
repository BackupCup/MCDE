package net.backupcup.mcde.screen.util;

import java.util.Optional;

import net.backupcup.mcde.util.Slots;
import net.minecraft.client.util.math.MatrixStack;

public interface ScreenWithSlots {
    Optional<Slots> getOpened();
    void setOpened(Optional<Slots> opened);
    void drawTexture(MatrixStack matrices, int x, int y, int u, int v, int width, int height);
}
