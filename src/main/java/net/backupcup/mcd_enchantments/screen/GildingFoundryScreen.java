package net.backupcup.mcd_enchantments.screen;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.util.EnchantmentUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GildingFoundryScreen extends HandledScreen<GildingFoundryScreenHandler> {
    private Inventory inventory;

    private static final Identifier TEXTURE =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/gilding_foundry.png");

    private int backgroundX;
    private int backgroundY;
    private int buttonX;
    private int buttonY;

    private int progressX;
    private int progressY;

    public GildingFoundryScreen(GildingFoundryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.inventory = handler.getInventory();
    }

    @Override
    protected void init() {
        super.init();
        titleX = 52;
        titleY = 4;
        playerInventoryTitleX = -200;
        playerInventoryTitleY = -200;
        backgroundX = ((width - backgroundWidth) / 2) - 2;
        backgroundY = (height - backgroundHeight) / 2;
        buttonX = backgroundX + 54;
        buttonY = backgroundY + 37;
        progressX = backgroundX + 60;
        progressY = backgroundY + 24;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /*Gilding Foundry UI*/
        RenderSystem.setShaderTexture(0, TEXTURE);
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        drawTexture(matrices, posX, posY, 0, 0, backgroundWidth + 10, backgroundHeight);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInBounds(buttonX, buttonY, (int)mouseX, (int)mouseY, 0, 76, 0, 12) && isGildingButtonClickable()) {
            client.interactionManager.clickButton(handler.syncId, 0);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        RenderSystem.setShaderTexture(0, TEXTURE);

        if (isGildingButtonClickable()) {
            drawTexture(matrices, buttonX, buttonY, 0, 180, 76, 12);
            if (isInBounds(buttonX, buttonY, mouseX, mouseY, 0, 76, 0, 12)) {
                drawTexture(matrices, buttonX, buttonY, 0, 192, 76, 12);
            }
        }

        drawProgress(matrices, handler.getProgress());

        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    private static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX &&
               mouseX <= posX + endX &&
               mouseY >= posY + startY &&
               mouseY <= posY + endY;
    }

    private boolean isGildingButtonClickable() {
        return !inventory.getStack(0).isEmpty() &&
            !inventory.getStack(1).isEmpty() &&
            !handler.hasProgress() &&
            !inventory.getStack(0).getNbt().contains("Gilding") &&
            EnchantmentUtils.canGenerateEnchantment(inventory.getStack(0));
    }

    private void drawProgress(MatrixStack matrices, int progress) {
        if (progress <= 1) {
            return;
        }
        if (progress >= 34) {
            drawTexture(matrices, progressX, progressY, 77, 168, 64, 38);
            return;
        }
        int fullLowerTextureX = 84;
        // lower bars
        if (2 <= progress && progress <= 13) {
            drawTexture(matrices, progressX + 7 + (13 - progress), progressY + 36, fullLowerTextureX + (13 - progress), 204, 28 + (progress - 2) * 2, 2);
            return;
        }
        
        if (progress > 13) {
            int step = progress == 14 ? 1 : 0;
            drawTexture(matrices, progressX + step, progressY + 36, 77 + step, 204, 64 - step * 2, 2);
        }

        if (progress > 15) {
            int step = progress == 16 ? 1 : 0;
            drawTexture(matrices, progressX, progressY + 29 + step, 77, 197 + step, 64, 2 - step);
        }

        if (progress > 17) {
            int step = progress == 18 ? 1 : 0;
            drawTexture(matrices, progressX, progressY + 7 + step, 77, 175 + step, 64, 2 - step);
        }

        if (progress > 19) {
            int step = progress == 20 ? 1 : 0;
            drawTexture(matrices, progressX, progressY + step, 77, 168 + step, 2, 2 - step);
            drawTexture(matrices, progressX + 62, progressY + step, 77 + 62, 168 + step, 2, 2 - step);
        }

        if (21 < progress && progress < 34) {
            drawTexture(matrices, progressX + 7, progressY, 84, 168, progress - 21, 2);
            drawTexture(matrices, progressX + 45 + (33 - progress), progressY, 122 + (33 - progress), 168, progress - 21, 2);
        }
    }
}
