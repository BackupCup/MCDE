package net.backupcup.mcde.screen;

import java.util.stream.IntStream;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler;
import net.backupcup.mcde.screen.util.TexturePos;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class GildingFoundryScreen extends HandledScreen<GildingFoundryScreenHandler> {
    private static enum GildingItemSilouette {
        GOLD, EMERALD
    }
    private GildingItemSilouette silouette = GildingItemSilouette.GOLD;
    private float silouetteTimer = 0f;
    private static final Identifier TEXTURE =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/gilding_foundry.png");
    private static final TexturePos GOLD_BUTTON_OFFSET = TexturePos.of(0, 168);
    private static final TexturePos EMERALD_BUTTON_OFFSET = TexturePos.of(0, 211);

    private Inventory inventory;
    private PlayerEntity playerEntity;


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
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /*Gilding Foundry UI*/
        RenderSystem.setShaderTexture(0, TEXTURE);
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        drawTexture(matrices, posX, posY, 0, 0, backgroundWidth + 10, backgroundHeight);
        drawTexture(matrices, posX + 84, posY + 53, switch (silouette) {
            case GOLD -> 151;
            case EMERALD -> 167;
        }, 166, 16, 16);

        silouetteTimer += delta;
        
        var ingridient = inventory.getStack(1);
        if (ingridient.isEmpty() && silouetteTimer > 20f) {
            silouette = switch (silouette) {
                case GOLD -> GildingItemSilouette.EMERALD;
                case EMERALD -> GildingItemSilouette.GOLD;
            };
        } else if (ingridient.isOf(Items.GOLD_INGOT)) {
            silouette = GildingItemSilouette.GOLD;
        } else if (ingridient.isOf(Items.EMERALD)) {
            silouette = GildingItemSilouette.EMERALD;
        }

        if (silouetteTimer > 20f) {
            silouetteTimer = 0;
        }
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        int width = textRenderer.getWidth(title);
        int height = textRenderer.fontHeight;
        int outlineX = titleX - 4;
        int outlineY = titleY - 4;
        RenderSystem.setShaderTexture(0, TEXTURE);
        var outlinePos = TexturePos.of(145, 168);
        drawTexture(matrices,  outlineX, outlineY, outlinePos.x(), outlinePos.y(), 3, height + 6);
        IntStream.range(-1, width + 1).forEach(i ->
                drawTexture(matrices, titleX + i, outlineY, outlinePos.x() + 2, outlinePos.y(), 1, height + 6));
        drawTexture(matrices, titleX + width + 1, outlineY, outlinePos.x() + 3, outlinePos.y(), 2, height + 6);
        super.drawForeground(matrices, mouseX, mouseY);
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

        drawProgress(matrices, handler.getProgress());

        if (!inventory.getStack(1).isEmpty() || playerEntity.isCreative() && !inventory.getStack(0).isEmpty()) {
            var buttonOffset = getButtonTextureOffset();
            if (isGildingButtonClickable()) {
                drawTexture(matrices, buttonX, buttonY, buttonOffset.x(), buttonOffset.y() + 12, 76, 12);
                if (isInBounds(buttonX, buttonY, mouseX, mouseY, 0, 76, 0, 12)) {
                    drawTexture(matrices, buttonX, buttonY, buttonOffset.x(), buttonOffset.y() + 24, 76, 12);
                }
            }
            else {
                drawTexture(matrices, buttonX, buttonY, buttonOffset.x(), buttonOffset.y(), 76, 12);
            }

            int color = hexToColor("#F6F6F6");
            int shadow = hexToColor("#6e2727");
            var text = Text.translatable("ui.mcde.gilding_button");
            if (buttonOffset.equals(EMERALD_BUTTON_OFFSET)) {
                shadow = hexToColor("#165a4c");
                text = Text.translatable("ui.mcde.regilding_button");
            }
            textRenderer.draw(matrices, text, buttonX + (76 - textRenderer.getWidth(text)) / 2 + 1, buttonY + 2 + 1, shadow);
            textRenderer.draw(matrices, text, buttonX + (76 - textRenderer.getWidth(text)) / 2, buttonY + 2, color);
        }

        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    private static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX &&
               mouseX <= posX + endX &&
               mouseY >= posY + startY &&
               mouseY <= posY + endY;
    }

    private boolean isGildingButtonClickable() {
        var weapon = inventory.getStack(0);
        var ingridient = inventory.getStack(1);
        var slotsOptional = EnchantmentSlots.fromItemStack(weapon);
        if (playerEntity.isCreative()) {
            return !weapon.isEmpty() &&
                !handler.hasProgress() &&
                handler.hasEnchantmentForGilding();

        }
        return !weapon.isEmpty() &&
            !ingridient.isEmpty() &&
            ingridient.getCount() >= MCDEnchantments.getConfig().getGildingCost() &&
            !handler.hasProgress() &&
            slotsOptional.filter(slots -> ingridient.isOf(Items.GOLD_INGOT) ^ slots.hasGilding()).isPresent() &&
            handler.hasEnchantmentForGilding();
    }

    private void drawProgress(MatrixStack matrices, int progress) {
        var progressOffset = getButtonTextureOffset().add(77, 0);
        if (progress <= 1) {
            return;
        }
        if (progress >= 34) {
            drawTexture(matrices, progressX, progressY, progressOffset.x(), progressOffset.y(), 64, 38);
            return;
        }
        int fullLowerTextureX = progressOffset.x() + 7;
        // lower bars
        if (2 <= progress && progress <= 13) {
            drawTexture(matrices, progressX + 7 + (13 - progress), progressY + 36, fullLowerTextureX + (13 - progress), progressOffset.y() + 36, 28 + (progress - 2) * 2, 2);
            return;
        }
        
        if (progress > 13) {
            int step = progress == 14 ? 1 : 0;
            drawTexture(matrices, progressX + step, progressY + 36, progressOffset.x() + step, progressOffset.y() + 36, 64 - step * 2, 2);
        }

        if (progress > 15) {
            int step = progress == 16 ? 1 : 0;
            drawTexture(matrices, progressX, progressY + 29 + step, progressOffset.x(), progressOffset.y() + 29 + step, 64, 2 - step);
        }

        if (progress > 17) {
            int step = progress == 18 ? 1 : 0;
            drawTexture(matrices, progressX, progressY + 7 + step, progressOffset.x(), progressOffset.y() + 7 + step, 64, 2 - step);
        }

        if (progress > 19) {
            int step = progress == 20 ? 1 : 0;
            drawTexture(matrices, progressX, progressY + step, progressOffset.x(), progressOffset.y() + step, 2, 2 - step);
            drawTexture(matrices, progressX + 62, progressY + step, progressOffset.x() + 62, progressOffset.y() + step, 2, 2 - step);
        }

        if (21 < progress && progress < 34) {
            drawTexture(matrices, progressX + 7, progressY, progressOffset.x() + 7, progressOffset.y(), progress - 21, 2);
            drawTexture(matrices, progressX + 45 + (33 - progress), progressY, progressOffset.x() + 45 + (33 - progress), progressOffset.y(), progress - 21, 2);
        }
    }

    private TexturePos getButtonTextureOffset() {
        if (playerEntity.isCreative()) {
            return EnchantmentSlots.fromItemStack(inventory.getStack(0))
                .map(slots -> slots.hasGilding()).orElse(false) ?
                EMERALD_BUTTON_OFFSET : GOLD_BUTTON_OFFSET;
        }
        return inventory.getStack(1).isOf(Items.EMERALD) ?
            EMERALD_BUTTON_OFFSET : GOLD_BUTTON_OFFSET;
    }

    private static int hexToColor(String hex) {
        return MathHelper.packRgb(
            Integer.parseInt(hex.substring(1, 3), 16) / 256f,
            Integer.parseInt(hex.substring(3, 5), 16) / 256f,
            Integer.parseInt(hex.substring(5, 7), 16) / 256f
        );
    }
}
