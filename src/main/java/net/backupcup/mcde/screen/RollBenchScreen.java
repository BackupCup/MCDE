package net.backupcup.mcde.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.screen.handler.RollBenchScreenHandler;
import net.backupcup.mcde.screen.util.EnchantmentSlotsRenderer;
import net.backupcup.mcde.screen.util.ScreenWithSlots;
import net.backupcup.mcde.screen.util.TextWrapUtils;
import net.backupcup.mcde.screen.util.TexturePos;
import net.backupcup.mcde.util.Choice;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.SlotPosition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

@Environment(EnvType.CLIENT)
public class RollBenchScreen extends HandledScreen<RollBenchScreenHandler> implements ScreenWithSlots {
    private static enum RerollItemSilouette {
        LAPIS, ECHO_SHARD;
    }
    private static final Identifier TEXTURE = new Identifier(MCDEnchantments.MOD_ID, "textures/gui/roll_bench.png");
    private Inventory inventory;
    private RerollItemSilouette silouette = RerollItemSilouette.LAPIS;
    private float silouetteTimer = 0f;
    private Optional<SlotPosition> opened = Optional.empty();
    private Optional<Pair<SlotPosition, SlotPosition>> selected = Optional.empty();
    private EnchantmentSlotsRenderer slotsRenderer;

    private TexturePos rerollButton;
    private boolean drawRerollButton;
    private TexturePos touchButton;

    public RollBenchScreen(RollBenchScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.inventory = handler.getInventory();
    }

    @Override
    protected void init() {
        super.init();
        titleX = 127;
        titleY = 10;
        playerInventoryTitleX = -200;
        playerInventoryTitleY = -200;

        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        slotsRenderer = EnchantmentSlotsRenderer.builder()
            .withScreen(this)
            .withDefaultGuiTexture(TEXTURE)
            .withDefaultSlotPositions(posX, posY)
            .withDimPredicate(choice -> {
                var slots = EnchantmentSlots.fromItemStack(inventory.getStack(0));
                return !handler.canReroll(client.player, choice.getEnchantmentId(), slots) ||
                    handler.isSlotLocked(choice.getEnchantmentSlot().getSlotPosition()).orElse(true);
            })
            .withClient(client)
            .build();
        drawRerollButton = client.player.isCreative();
        rerollButton = TexturePos.of(posX + 168, posY + 34);
        touchButton = TexturePos.of(posX + 8, posY + 57);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /* Reroll Station UI */
        RenderSystem.setShaderTexture(0, TEXTURE);
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        drawTexture(matrices, posX, posY, 0, 0, backgroundWidth + 10, backgroundHeight);

        drawTexture(matrices, posX + 146, posY + 51, switch (silouette) {
            case LAPIS -> 0;
            case ECHO_SHARD -> 18;
        }, 215, 18, 18);

        silouetteTimer += delta;
        if (inventory.getStack(1).isEmpty() && silouetteTimer > 20f) {
            silouette = switch (silouette) {
                case LAPIS -> RerollItemSilouette.ECHO_SHARD;
                case ECHO_SHARD -> RerollItemSilouette.LAPIS;
            };
        } else if (inventory.getStack(1).isOf(Items.LAPIS_LAZULI)) {
            silouette = RerollItemSilouette.LAPIS;
        } else if (inventory.getStack(1).isOf(Items.ECHO_SHARD)) {
            silouette = RerollItemSilouette.ECHO_SHARD;
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

        drawTexture(matrices,  outlineX, outlineY, 0, 172, 3, height + 6);
        IntStream.range(-1, width + 1).forEach(i ->
                drawTexture(matrices, titleX + i, outlineY, 2, 172, 1, height + 6));
        drawTexture(matrices, titleX + width + 1, outlineY, 3, 172, 2, height + 6);
        super.drawForeground(matrices, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack stack = inventory.getStack(0);

        if (stack.isEmpty())
            return super.mouseClicked(mouseX, mouseY, button);
        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(stack);

        if (isTouchscreen() && selected.isPresent() && isInTouchButton((int)mouseX, (int)mouseY)) {
            var slotPos = selected.get().getLeft();
            var choicePos = selected.get().getRight();
            if (slots.getEnchantmentSlot(slotPos).map(slot -> slotsRenderer.getDimPredicate().test(new Choice(slot, choicePos))).orElse(false)) {
                return false;
            }
            if (slots.getEnchantmentSlot(slotPos).filter(slot -> slot.getChosen().isPresent()).isPresent()) {
                selected = Optional.empty();
                opened = Optional.of(slotPos);
            }
            client.interactionManager.clickButton(handler.syncId, SlotPosition.values().length * slotPos.ordinal() + choicePos.ordinal());
            return false;
        }

        for (var slot : slots) {
            if (slotsRenderer.isInSlotBounds(slot.getSlotPosition(), (int) mouseX, (int) mouseY)) {
                if (slot.getChosen().isPresent()) {
                    var chosen = slot.getChosen().get();
                    if (isTouchscreen()) {
                        selected = Optional.of(new Pair<>(slot.getSlotPosition(), chosen.getChoicePosition()));
                        opened = Optional.empty();
                    } else if (!slotsRenderer.getDimPredicate().test(chosen)) {
                        client.interactionManager.clickButton(handler.syncId, SlotPosition.values().length * slot.ordinal());
                    }
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                if (opened.isEmpty()) {
                    opened = Optional.of(slot.getSlotPosition());
                } else if (opened.get() == slot.getSlotPosition()) {
                    opened = Optional.empty();
                    selected = Optional.empty();
                } else {
                    opened = Optional.of(slot.getSlotPosition());
                    selected = Optional.empty();
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            if (opened.isPresent() && opened.get() == slot.getSlotPosition()) {
                for (var choice : slot.choices()) {
                    if (slotsRenderer.isInChoiceBounds(slot.getSlotPosition(), choice.getChoicePosition(), (int) mouseX, (int) mouseY)) {
                        if (isTouchscreen()) {
                            selected = Optional.of(new Pair<>(slot.getSlotPosition(), choice.getChoicePosition()));
                        } else if (!slotsRenderer.getDimPredicate().test(choice)) {
                            client.interactionManager.clickButton(handler.syncId, SlotPosition.values().length * slot.ordinal() + choice.ordinal());
                        }
                        return super.mouseClicked(mouseX, mouseY, button);
                    }
                }
            }
        }
        opened = Optional.empty();
        selected = Optional.empty();
        if (drawRerollButton && !inventory.getStack(0).isEmpty() && isInRerollButton((int)mouseX, (int)mouseY)) {
            client.interactionManager.clickButton(handler.syncId, RollBenchScreenHandler.REROLL_BUTTON_ID);
            return false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        RenderSystem.setShaderTexture(0, TEXTURE);

        renderRerollButton(matrices, mouseX, mouseY);

        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        if (isTouchscreen()) {
            drawTexture(matrices, touchButton.x(), touchButton.y(), 36, 215, 13, 13);
        }

        ItemStack itemStack = inventory.getStack(0);
        var slots = EnchantmentSlots.fromItemStack(itemStack);
        if (itemStack.isEmpty() || slots == null) {
            drawMouseoverTooltip(matrices, mouseX, mouseY);
            return;
        }

        if (!isTouchscreen()) {
            for (var slot : slots) {
                var pos = slot.getSlotPosition();
                if (!slotsRenderer.isInChoicesBounds(pos, mouseX, mouseY) && opened.map(slotPos -> slotPos.equals(pos)).orElse(false)) {
                    opened = Optional.empty();
                }
                if (slotsRenderer.isInSlotBounds(pos, mouseX, mouseY)) {
                    opened = Optional.of(slot.getSlotPosition());
                }
            }
        }

        Optional<Choice> hoveredChoice = slotsRenderer.render(matrices, itemStack, mouseX, mouseY);
        if (isTouchscreen()) {
            selected.flatMap(pair -> slots.getEnchantmentSlot(pair.getLeft())
                    .map(slot -> new Choice(slot, pair.getRight()))).ifPresent(choice -> {
                renderTooltip(matrices, choice, slots, posX, posY + 88);
                if (choice.isChosen()) {
                    slotsRenderer.drawHoverOutline(matrices, choice.getEnchantmentSlot().getSlotPosition());
                } else {
                    slotsRenderer.drawIconHoverOutline(matrices, choice.getEnchantmentSlot().getSlotPosition(), choice);
                }
                if (!slotsRenderer.getDimPredicate().test(choice)) {
                    int buttonX = 49;
                    if (isInTouchButton(mouseX, mouseY)) {
                        buttonX = 62;
                    }
                    drawTexture(matrices, touchButton.x(), touchButton.y(), buttonX, 215, 13, 13);
                }
            });
        } else {
            hoveredChoice.ifPresent(choice -> renderTooltip(matrices, choice, slots, mouseX, mouseY));
        }
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    public Optional<SlotPosition> getOpened() {
        return opened;
    }

    @Override
    public void setOpened(Optional<SlotPosition> opened) {
        this.opened = opened;
    }

    protected void renderRerollButton(MatrixStack matrices, int mouseX, int mouseY) {
        drawRerollButton = inventory.getStack(1).isOf(Items.ECHO_SHARD) || client.player.isCreative();
        if (drawRerollButton) {
            int textureButtonX;
            if (inventory.getStack(0).isEmpty()) {
                textureButtonX = 0;
            } else if (isInRerollButton(mouseX, mouseY)) {
                textureButtonX = 50;
            } else {
                textureButtonX = 25;
            }
            drawTexture(matrices, rerollButton.x(), rerollButton.y(), textureButtonX, 187, 25, 28);
        }
    }

    protected void renderTooltip(MatrixStack matrices, Choice hovered, EnchantmentSlots slots, int x, int y) {
        Identifier enchantment = hovered.getEnchantmentId();
        String translationKey = enchantment.toTranslationKey("enchantment");
        List<Text> tooltipLines = new ArrayList<>();
        boolean canReroll = handler.canReroll(client.player, enchantment, slots);
        MutableText enchantmentName = Text.translatable(translationKey)
                .formatted(EnchantmentUtils.formatEnchantment(enchantment));
        if (hovered.isChosen() && hovered.getEnchantment().getMaxLevel() > 1) {
            enchantmentName.append(" ")
                .append(Text.translatable("enchantment.level." + hovered.getLevel()));
        }
        tooltipLines.add(enchantmentName);

        tooltipLines.addAll(TextWrapUtils.wrapText(width, translationKey + ".desc", Formatting.GRAY));
        if (!client.player.isCreative()) {
            tooltipLines.addAll(TextWrapUtils.wrapText(width, Text.translatable(
                        "message.mcde.lapis_required",
                        slots.getNextRerollCost(enchantment)), Formatting.ITALIC, Formatting.DARK_GRAY));
        }
        if (!canReroll) {
            tooltipLines.addAll(TextWrapUtils.wrapText(width, Text.translatable("message.mcde.not_enough_lapis"),
                    Formatting.DARK_RED, Formatting.ITALIC));
        }
        if (handler.isSlotLocked(hovered.getEnchantmentSlot().getSlotPosition()).orElse(true)) {
            tooltipLines.addAll(TextWrapUtils.wrapText(width, Text.translatable("message.mcde.cant_generate"), Formatting.DARK_RED, Formatting.ITALIC));
        }
        renderTooltip(matrices, tooltipLines, x, y);
    }

    protected static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX &&
               mouseX <= posX + endX &&
               mouseY >= posY + startY &&
               mouseY <= posY + endY;
    }

    protected boolean isInRerollButton(int mouseX, int mouseY) {
        return isInBounds(rerollButton.x(), rerollButton.y(), mouseX, mouseY, 0, 25, 0, 28);
    }

    protected boolean isInTouchButton(int mouseX, int mouseY) {
        return isInBounds(touchButton.x(), touchButton.y(), mouseX, mouseY, 0, 13, 0, 13);
    }

    private boolean isTouchscreen() {
        return client.options.getTouchscreen().getValue();
    }
}
