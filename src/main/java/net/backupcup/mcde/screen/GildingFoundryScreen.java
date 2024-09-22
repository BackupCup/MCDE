package net.backupcup.mcde.screen;

import java.util.Optional;
import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler;
import net.backupcup.mcde.screen.util.TexturePos;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
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
    private static final Identifier TEXTURE = MCDE.id("textures/gui/gilding_foundry.png");
    private static final TexturePos GOLD_BUTTON_OFFSET = TexturePos.of(2, 223);
    private static final TexturePos EMERALD_BUTTON_OFFSET = TexturePos.of(2, 239);

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
        buttonX = backgroundX + 46;
        buttonY = backgroundY + 59;
        progressX = backgroundX + 63;
        progressY = backgroundY + 3;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        Slot slot = this.handler.getSlot(1);

        ctx.drawTexture(TEXTURE, posX, posY, 2, 20, 168, 167);
        ctx.drawTexture(TEXTURE, posX + slot.x + 2, posY + slot.y, 239, switch (silouette) {
            case GOLD -> 166;
            case EMERALD -> 185;
        }, 16, 16);

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
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
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

        if (!handler.hasProgress()) {
            ctx.drawTexture(TEXTURE, buttonX, buttonY, 2, 207, 76, 15);
        }
        else {
            var buttonOffset = getButtonTextureOffset().add(156, 2);
            ctx.drawTexture(TEXTURE, buttonX, buttonY, buttonOffset.x(), buttonOffset.y(), 76, 13);
        }

        drawProgress(ctx, handler.getProgress());

        if (!inventory.getStack(1).isEmpty() || playerEntity.isCreative() && !inventory.getStack(0).isEmpty()) {
            var buttonOffset = getButtonTextureOffset();
            if (isGildingButtonClickable()) {
                ctx.drawTexture(TEXTURE, buttonX, buttonY, buttonOffset.x(), buttonOffset.y(), 76, 15);
                if (isInBounds(buttonX, buttonY, mouseX, mouseY, 0, 76, 0, 15)) {
                    ctx.drawTexture(TEXTURE, buttonX, buttonY, buttonOffset.x() + 78, buttonOffset.y(), 76, 15);
                }
            }

            int color = hexToColor("#F6F6F6");
            int shadow = hexToColor("#6e2727");
            var text = Text.translatable("ui.mcde.gilding_button");
            if (buttonOffset.equals(EMERALD_BUTTON_OFFSET)) {
                shadow = hexToColor("#165a4c");
                text = Text.translatable("ui.mcde.regilding_button");
            }
            ctx.drawText(textRenderer, text, buttonX + (76 - textRenderer.getWidth(text)) / 2 + 1, buttonY + 2 + 1, shadow, false);
            ctx.drawText(textRenderer, text, buttonX + (76 - textRenderer.getWidth(text)) / 2, buttonY + 2, color, false);
        }
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
        var ingridient = inventory.getStack(1);
        var slotsOptional = EnchantmentSlots.fromItemStack(weapon);
        if (playerEntity.isCreative()) {
            return !weapon.isEmpty() &&
                !handler.hasProgress() &&
                handler.hasEnchantmentForGilding();

        }
        return !weapon.isEmpty() &&
            !ingridient.isEmpty() &&
            ingridient.getCount() >= MCDE.getConfig().getGildingCost() &&
            !handler.hasProgress() &&
            slotsOptional.filter(slots -> ingridient.isOf(Items.GOLD_INGOT) ^ slots.hasGilding()).isPresent() &&
            handler.hasEnchantmentForGilding();
    }

    private void drawProgress(DrawContext ctx, int progress) {
        progress = (int)((float)progress / MCDE.getConfig().getGildingDuration() * 25f);
        var progressOffset = getButtonTextureOffset(TexturePos.of(247, 204), TexturePos.of(247, 230));
        if (progress < 1) {
            return;
        }
        if (progress > 25) {
            ctx.drawTexture(TEXTURE, progressX, progressY, progressOffset.x(), progressOffset.y(), 8, 25);
            ctx.drawTexture(TEXTURE, progressX + 34, progressY, progressOffset.x(), progressOffset.y(), 8, 25);
            return;
        }
        ctx.drawTexture(TEXTURE, progressX, progressY + 25 - progress, progressOffset.x(), progressOffset.y() + 25 - progress, 8, progress);
        ctx.drawTexture(TEXTURE, progressX + 34, progressY + 25 - progress, progressOffset.x(), progressOffset.y() + 25 - progress, 8, progress);
    }

    private TexturePos getButtonTextureOffset(TexturePos gold, TexturePos emerald) {
        if (playerEntity.isCreative()) {
            return EnchantmentSlots.fromItemStack(inventory.getStack(0))
                .map(slots -> slots.hasGilding()).orElse(false) ?
                emerald : gold;
        }
        return inventory.getStack(1).isOf(Items.EMERALD) ?
            emerald : gold;
    }

    private TexturePos getButtonTextureOffset() {
        return getButtonTextureOffset(GOLD_BUTTON_OFFSET, EMERALD_BUTTON_OFFSET);
    }

    private static int hexToColor(String hex) {
        return MathHelper.packRgb(
            Integer.parseInt(hex.substring(1, 3), 16) / 256f,
            Integer.parseInt(hex.substring(3, 5), 16) / 256f,
            Integer.parseInt(hex.substring(5, 7), 16) / 256f
        );
    }
}
