package net.backupcup.mcde.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.screen.handler.RunicTableScreenHandler;
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
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

@Environment(EnvType.CLIENT)
public class RunicTableScreen extends HandledScreen<RunicTableScreenHandler> implements ScreenWithSlots {
    private static final Identifier TEXTURE =
        new Identifier(MCDEnchantments.MOD_ID, "textures/gui/runic_table.png");
    private Inventory inventory;
    private Optional<SlotPosition> opened = Optional.empty();
    private Optional<Pair<SlotPosition, SlotPosition>> selected = Optional.empty();
    private EnchantmentSlotsRenderer slotsRenderer;

    private TexturePos touchButton;

    public RunicTableScreen(RunicTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.inventory = handler.getInventory();
    }

    @Override
    protected void init() {
        super.init();
        titleX = 125;
        titleY = 10;

        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        slotsRenderer = EnchantmentSlotsRenderer.builder()
            .withScreen(this)
            .withDefaultGuiTexture(TEXTURE)
            .withDefaultSlotPositions(posX, posY)
            .withDimPredicate(choice -> {
                int level = 1;
                boolean isMaxedOut = false;
                if (choice.isChosen()) {
                    level = choice.getLevel() + 1;
                    isMaxedOut = choice.isMaxedOut();
                }
                return isMaxedOut || !RunicTableScreenHandler.canEnchant(client.player, choice.getEnchantmentId(), level) ||
                    (EnchantmentHelper.get(inventory.getStack(0)).keySet().stream().anyMatch(e -> !e.canCombine(choice.getEnchantment())) && 
                         !choice.isChosen());
            })
            .withClient(client)
            .build();
        touchButton = TexturePos.of(posX + 8, posY + 57);
    }

    // For some reason without this, it crashes with AbstractMethodException
    // where it says drawTexture is not implemented for ScreenWithSlots interface
    @Override
    public void drawTexture(MatrixStack matrices, int x, int y, int u, int v, int width, int height) {
        super.drawTexture(matrices, x, y, u, v, width, height);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /*Runic Table UI*/
        RenderSystem.setShaderTexture(0, TEXTURE);
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        drawTexture(matrices, posX, posY, 0, 0, backgroundWidth + 10, backgroundHeight);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack stack = inventory.getStack(0);

        if (stack.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);
        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(stack);

        if (isTouchscreen() && selected.isPresent() && isInTouchButton((int)mouseX, (int)mouseY)) {
            var slot = selected.get().getLeft();
            var choice = selected.get().getRight();
            client.interactionManager.clickButton(handler.syncId, SlotPosition.values().length * slot.ordinal() + choice.ordinal());
            return false;
        }

        for (var slot : slots) {
            if (slotsRenderer.isInSlotBounds(slot.getSlotPosition(), (int)mouseX, (int)mouseY)) {
                if (slot.getChosen().isPresent()) {
                    var chosen = slot.getChosen().get();
                    if (isTouchscreen()) {
                        selected = Optional.of(new Pair<>(slot.getSlotPosition(), chosen.getChoicePosition()));
                        opened = Optional.empty();
                    } else {
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

        return super.mouseClicked(mouseX, mouseY, button);
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

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        RenderSystem.setShaderTexture(0, TEXTURE);
        ItemStack itemStack = inventory.getStack(0);
        var slots = EnchantmentSlots.fromItemStack(itemStack);
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        touchButton = TexturePos.of(posX + -4, posY + 54);
        if (isTouchscreen()) {
            drawTexture(matrices, touchButton.x(), touchButton.y(), 0, 187, 13, 13);
        }
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

        var hoveredChoice = slotsRenderer.render(matrices, itemStack, mouseX, mouseY);

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
                    int buttonX = 13;
                    if (isInTouchButton(mouseX, mouseY)) {
                        buttonX = 26;
                    }
                    drawTexture(matrices, touchButton.x(), touchButton.y(), buttonX, 187, 13, 13);
                }
            });
        } else {
            hoveredChoice.ifPresent(choice -> renderTooltip(matrices, choice, slots, mouseX, mouseY));
        }

        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    protected void renderTooltip(MatrixStack matrices, Choice hovered, EnchantmentSlots slots, int x, int y) {
        var itemStack = inventory.getStack(0);
        Enchantment enchantment = hovered.getEnchantment();
        Identifier enchantmentId = hovered.getEnchantmentId();
        List<Text> tooltipLines = new ArrayList<>();
        int level = 1;
        boolean enoughLevels = RunicTableScreenHandler.canEnchant(client.player, enchantmentId, level);
        MutableText enchantmentName = Text.translatable(enchantment.getTranslationKey())
            .formatted(EnchantmentUtils.formatEnchantment(enchantmentId));
        if (hovered.isChosen()) {
            enchantmentName.append(" ")
                .append(Text.translatable("enchantment.level." + hovered.getLevel()))
                .append(" ");
            if (hovered.isMaxedOut()) {
                enchantmentName.append(Text.translatable("message.mcde.max_level"));
                enoughLevels = true;
            }
            else {
                enchantmentName
                    .append("→ ")
                    .append(Text.translatable("enchantment.level." + (hovered.getLevel() + 1)));
                level = hovered.getLevel() + 1;
                enoughLevels = RunicTableScreenHandler.canEnchant(client.player, enchantmentId, level);

            }
        }
        tooltipLines.add(enchantmentName);

        tooltipLines.addAll(TextWrapUtils.wrapText(width, enchantment.getTranslationKey() + ".desc", Formatting.GRAY));
        if (!hovered.isMaxedOut() && !client.player.isCreative()) {
            tooltipLines.addAll(TextWrapUtils.wrapText(width, Text.translatable(
                            "message.mcde.levels_required",
                            MCDEnchantments.getConfig().getEnchantCost(enchantmentId, level)),
                        Formatting.ITALIC, Formatting.DARK_GRAY));
        }
        if (!enoughLevels) {
            tooltipLines.addAll(TextWrapUtils.wrapText(width, "message.mcde.not_enough_levels", Formatting.DARK_RED, Formatting.ITALIC));
        }

        if (!hovered.isChosen()) {
            if (EnchantmentHelper.getLevel(hovered.getEnchantment(), itemStack) > 0) {
                tooltipLines.addAll(TextWrapUtils.wrapText(width, "message.mcde.already_exists", Formatting.DARK_RED, Formatting.ITALIC));
            } else if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
                var conflict = EnchantmentHelper.get(itemStack).keySet().stream()
                    .filter(e -> !e.canCombine(hovered.getEnchantment())).findFirst();
                if (conflict.isPresent()) {
                    var conflicting = conflict.get();
                    tooltipLines.addAll(TextWrapUtils.wrapText(width, Text.translatable(
                                    "message.mcde.cant_combine",
                                    Text.translatable(conflicting.getTranslationKey())),
                                Formatting.DARK_RED, Formatting.ITALIC));
                }
            }
        }
        renderTooltip(matrices, tooltipLines, x, y);
    }

    @Override
    public Optional<SlotPosition> getOpened() {
        return opened;
    }

    @Override
    public void setOpened(Optional<SlotPosition> opened) {
        this.opened = opened;
    }

    private boolean isTouchscreen() {
        return client.options.getTouchscreen().getValue();
    }

    protected static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX &&
               mouseX <= posX + endX &&
               mouseY >= posY + startY &&
               mouseY <= posY + endY;
    }

    protected boolean isInTouchButton(int mouseX, int mouseY) {
        return isInBounds(touchButton.x(), touchButton.y(), mouseX, mouseY, 0, 13, 0, 13);
    }
}
