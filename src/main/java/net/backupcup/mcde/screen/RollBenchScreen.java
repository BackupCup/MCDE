package net.backupcup.mcde.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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
    private static enum RerollButtonState {
        HIDDEN, EXTENDING, SHUTTING, SHOWED
    }
    private static final Identifier TEXTURE = new Identifier(MCDEnchantments.MOD_ID, "textures/gui/roll_bench.png");
    private Inventory inventory;
    private RerollItemSilouette silouette = RerollItemSilouette.LAPIS;
    private float silouetteTimer = 0f;
    private Optional<SlotPosition> opened = Optional.empty();
    private Optional<Pair<SlotPosition, SlotPosition>> selected = Optional.empty();
    private EnchantmentSlotsRenderer slotsRenderer;

    private TexturePos background;

    private TexturePos rerollButton;
    private boolean drawRerollButton;
    private RerollButtonState rerollButtonState;
    private float rerollButtonAnimationProgress;
    private static final float rerollButtonAnimationDuration = 20.0f;

    private TexturePos touchButton;

    public RollBenchScreen(RollBenchScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.inventory = handler.getInventory();
    }

    @Override
    protected void init() {
        super.init();

        background = TexturePos.of(((width - backgroundWidth) / 2) - 2, (height - backgroundHeight) / 2 + 25);
        slotsRenderer = EnchantmentSlotsRenderer.builder()
            .withScreen(this)
            .withDefaultGuiTexture(TEXTURE)
            .withDefaultSlotPositions(background)
            .withDimPredicate(
                choice -> EnchantmentSlots.fromItemStack(inventory.getStack(0))
                .map(
                    slots -> !handler.canReroll(client.player, choice.getEnchantmentId(), slots) ||
                        handler.isSlotLocked(choice.getEnchantmentSlot().getSlotPosition()).orElse(true)
                ).orElse(true)
            )
            .withClient(client)
            .build();
        drawRerollButton = client.player.isCreative();
        rerollButton = background.add(155, 12);
        rerollButtonState = RerollButtonState.HIDDEN;
        rerollButtonAnimationProgress = 0f;
        touchButton = background.add(-2, 38);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.drawTexture(TEXTURE, background.x(), background.y(), 1, 109, 168, 149);

        ctx.drawTexture(TEXTURE, background.x() + 134, background.y() + 27, 195, switch (silouette) {
            case LAPIS -> 175;
            case ECHO_SHARD -> 197;
        }, 18, 18);

        silouetteTimer += delta;

        ItemStack ingridient = inventory.getStack(1);
        if (ingridient.isEmpty() && silouetteTimer > 20f) {
            silouette = switch (silouette) {
                case LAPIS -> RerollItemSilouette.ECHO_SHARD;
                case ECHO_SHARD -> RerollItemSilouette.LAPIS;
            };
        } else if (ingridient.isOf(Items.LAPIS_LAZULI)) {
            silouette = RerollItemSilouette.LAPIS;
        } else if (ingridient.isOf(Items.ECHO_SHARD)) {
            silouette = RerollItemSilouette.ECHO_SHARD;
        }

        if (silouetteTimer > 20f) {
            silouetteTimer = 0;
        }

    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack stack = inventory.getStack(0);

        if (stack.isEmpty())
            return super.mouseClicked(mouseX, mouseY, button);
        var slotsOptional = EnchantmentSlots.fromItemStack(stack);
        if (slotsOptional.isEmpty()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        var slots = slotsOptional.get();
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        renderRerollButton(ctx, mouseX, mouseY, delta);

        if (isTouchscreen()) {
            ctx.drawTexture(TEXTURE, touchButton.x(), touchButton.y(), 164, 0, 13, 15);
        }

        ItemStack itemStack = inventory.getStack(0);
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (itemStack.isEmpty() || slotsOptional.isEmpty()) {
            drawMouseoverTooltip(ctx, mouseX, mouseY);
            return;
        }
        var slots = slotsOptional.get();
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

        Optional<Choice> hoveredChoice = slotsRenderer.render(ctx, itemStack, mouseX, mouseY);

        if (isTouchscreen()) {
            selected.flatMap(pair -> slots.getEnchantmentSlot(pair.getLeft())
                    .map(slot -> new Choice(slot, pair.getRight()))).ifPresent(choice -> {
                renderTooltip(ctx, choice, slots, background.x(), background.y() + 88);
                if (choice.isChosen()) {
                    slotsRenderer.drawHoverOutline(ctx, choice.getEnchantmentSlot().getSlotPosition());
                } else {
                    slotsRenderer.drawIconHoverOutline(ctx, choice.getEnchantmentSlot().getSlotPosition(), choice);
                }
                if (!slotsRenderer.getDimPredicate().test(choice)) {
                    int buttonX = 177;
                    if (isInTouchButton(mouseX, mouseY)) {
                        buttonX = 190;
                    }
                    ctx.drawTexture(TEXTURE, touchButton.x(), touchButton.y(), buttonX, 0, 13, 15);
                }
            });
        } else {
            hoveredChoice.ifPresent(choice -> renderTooltip(ctx, choice, slots, mouseX, mouseY));
        }

        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    public Optional<SlotPosition> getOpened() {
        return opened;
    }

    @Override
    public void setOpened(Optional<SlotPosition> opened) {
        this.opened = opened;
    }

    private static Function<Float, Integer> frameEasing(Function<Float, Float> f) {
        // 20 is the number of frames
        return x -> (int)(f.apply(x) * 20f);
    }

    private static float easeOut(float x) {
        return (float)(1 - Math.pow(1 - x, 3));
    }

    private static float easeIn(float x) {
        return x * x * x;
    }

    protected void renderRerollButton(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawRerollButton = !inventory.getStack(0).isEmpty() && (inventory.getStack(1).isOf(Items.ECHO_SHARD) || client.player.isCreative());
        if (drawRerollButton && (rerollButtonState == RerollButtonState.HIDDEN || rerollButtonState == RerollButtonState.SHUTTING)) {
            rerollButtonState = RerollButtonState.EXTENDING;
            rerollButtonAnimationProgress = 0f;
        }
        else if (!drawRerollButton && (rerollButtonState == RerollButtonState.SHOWED || rerollButtonState == RerollButtonState.EXTENDING)) {
            rerollButtonState = RerollButtonState.SHUTTING;
            rerollButtonAnimationProgress = 0f;
        }
        else if (rerollButtonState == RerollButtonState.EXTENDING && rerollButtonAnimationProgress > 1.0f) {
            rerollButtonState = RerollButtonState.SHOWED;
            rerollButtonAnimationProgress = 0f;
        }
        else if (rerollButtonState == RerollButtonState.SHUTTING && rerollButtonAnimationProgress > 1.0f) {
            rerollButtonState = RerollButtonState.HIDDEN;
            rerollButtonAnimationProgress = 0f;
        }

        if (rerollButtonState == RerollButtonState.SHOWED) {
            ctx.drawTexture(TEXTURE, rerollButton.x(), rerollButton.y(), 232, 0, 24, 34);
        }
        else if (rerollButtonState == RerollButtonState.EXTENDING || rerollButtonState == RerollButtonState.SHUTTING) {
            Function<Float, Float> easing = switch (rerollButtonState) {
                case EXTENDING -> RollBenchScreen::easeOut;
                case SHUTTING -> RollBenchScreen::easeIn;
                default -> null;
            };
            float progress = switch (rerollButtonState) {
                case EXTENDING -> rerollButtonAnimationProgress;
                case SHUTTING -> 1 - rerollButtonAnimationProgress;
                default -> 0f;
            };
            drawAnimationRerollButtonFrame(ctx, frameEasing(easing).apply(progress), isInRerollButton(mouseX, mouseY));
            MCDEnchantments.LOGGER.info(String.format("progress: %.2f", rerollButtonAnimationProgress));
            rerollButtonAnimationProgress += delta / rerollButtonAnimationDuration;
        }

        if (rerollButtonState == RerollButtonState.SHOWED && isInRerollButton(mouseX, mouseY)) {
            ctx.drawTexture(TEXTURE, rerollButton.x(), rerollButton.y(), 204, 0, 25, 35);
        }
    }
    
    private void drawAnimationRerollButtonFrame(DrawContext ctx, int progress, boolean hovered) {
        if (progress > 20) {
            progress = 20;
        }
        if (progress < 0) {
            progress = 0;
        }
        int x = 232, width = 4, height = 34;
        if (hovered && drawRerollButton) {
            x = 204;
            width = 5;
            height = 35;
        }
        TexturePos part = TexturePos.of(x + 20 - progress, 0);
        ctx.drawTexture(TEXTURE, rerollButton.x(), rerollButton.y(), part.x(), part.y(), width + progress, height);
    }

    protected void renderTooltip(DrawContext ctx, Choice hovered, EnchantmentSlots slots, int x, int y) {
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
        ctx.drawTooltip(textRenderer, tooltipLines, x, y);
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
        return isInBounds(touchButton.x(), touchButton.y(), mouseX, mouseY, 0, 13, 0, 15);
    }

    private boolean isTouchscreen() {
        return client.options.getTouchscreen().getValue();
    }
}
