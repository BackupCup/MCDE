package net.backupcup.mcde.screen;

import java.util.stream.IntStream;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class GildingFoundryScreen extends HandledScreen<GildingFoundryScreenHandler> {
    private Inventory inventory;
    private PlayerEntity playerEntity;

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
        this.playerEntity = inventory.player;
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title) + 4) / 2;
        titleY = -3;
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
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /*Gilding Foundry UI*/
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        ctx.drawTexture(TEXTURE, posX, posY, 0, 0, backgroundWidth + 10, backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        int width = textRenderer.getWidth(title);
        int height = textRenderer.fontHeight;
        int outlineX = titleX - 4;
        int outlineY = titleY - 4;

        ctx.drawTexture(TEXTURE,  outlineX, outlineY, 0, 204, 3, height + 6);
        IntStream.range(-1, width + 1).forEach(i ->
                ctx.drawTexture(TEXTURE, titleX + i, outlineY, 2, 204, 1, height + 6));
        ctx.drawTexture(TEXTURE, titleX + width + 1, outlineY, 3, 204, 2, height + 6);
        super.drawForeground(ctx, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInBounds(buttonX, buttonY, (int)mouseX, (int)mouseY, 0, 76, 0, 12) && isGildingButtonClickable()) {
            client.interactionManager.clickButton(handler.syncId, 0);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        if (isGildingButtonClickable()) {
            ctx.drawTexture(TEXTURE, buttonX, buttonY, 0, 180, 76, 12);
            if (isInBounds(buttonX, buttonY, mouseX, mouseY, 0, 76, 0, 12)) {
                ctx.drawTexture(TEXTURE, buttonX, buttonY, 0, 192, 76, 12);
            }
        }

        drawProgress(ctx, handler.getProgress());

        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    private static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX &&
               mouseX <= posX + endX &&
               mouseY >= posY + startY &&
               mouseY <= posY + endY;
    }

    private boolean isGildingButtonClickable() {
        var weapon = inventory.getStack(0);
        var slotsOptional = EnchantmentSlots.fromItemStack(weapon);
        return !weapon.isEmpty() &&
            (inventory.getStack(1).getCount() >= MCDEnchantments.getConfig().getGildingCost() || playerEntity.isCreative()) &&
            !handler.hasProgress() &&
            slotsOptional.filter(EnchantmentSlots::hasGilding).isEmpty() &&
            handler.hasEnchantmentForGilding();
    }

    private void drawProgress(DrawContext ctx, int progress) {
        if (progress <= 1) {
            return;
        }
        if (progress >= 34) {
            ctx.drawTexture(TEXTURE, progressX, progressY, 77, 168, 64, 38);
            return;
        }
        int fullLowerTextureX = 84;
        // lower bars
        if (2 <= progress && progress <= 13) {
            ctx.drawTexture(TEXTURE, progressX + 7 + (13 - progress), progressY + 36, fullLowerTextureX + (13 - progress), 204, 28 + (progress - 2) * 2, 2);
            return;
        }
        
        if (progress > 13) {
            int step = progress == 14 ? 1 : 0;
            ctx.drawTexture(TEXTURE, progressX + step, progressY + 36, 77 + step, 204, 64 - step * 2, 2);
        }

        if (progress > 15) {
            int step = progress == 16 ? 1 : 0;
            ctx.drawTexture(TEXTURE, progressX, progressY + 29 + step, 77, 197 + step, 64, 2 - step);
        }

        if (progress > 17) {
            int step = progress == 18 ? 1 : 0;
            ctx.drawTexture(TEXTURE, progressX, progressY + 7 + step, 77, 175 + step, 64, 2 - step);
        }

        if (progress > 19) {
            int step = progress == 20 ? 1 : 0;
            ctx.drawTexture(TEXTURE, progressX, progressY + step, 77, 168 + step, 2, 2 - step);
            ctx.drawTexture(TEXTURE, progressX + 62, progressY + step, 77 + 62, 168 + step, 2, 2 - step);
        }

        if (21 < progress && progress < 34) {
            ctx.drawTexture(TEXTURE, progressX + 7, progressY, 84, 168, progress - 21, 2);
            ctx.drawTexture(TEXTURE, progressX + 45 + (33 - progress), progressY, 122 + (33 - progress), 168, progress - 21, 2);
        }
    }
}
