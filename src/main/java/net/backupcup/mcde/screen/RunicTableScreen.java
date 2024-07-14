package net.backupcup.mcde.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
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

    private TexturePos background;
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

        background = TexturePos.of(((width - backgroundWidth) / 2) - 2, (height - backgroundHeight) / 2 + 25);
        slotsRenderer = EnchantmentSlotsRenderer.builder()
            .withScreen(this)
            .withDefaultGuiTexture(TEXTURE)
            .withDefaultSlotPositions(background)
            .withDimPredicate(choice -> {
                int level = 1;
                boolean isMaxedOut = false;
                if (choice.isChosen()) {
                    level = choice.getLevel() + 1;
                    isMaxedOut = choice.isMaxedOut();
                }
                return isMaxedOut || !RunicTableScreenHandler.canEnchant(client.player, choice.getEnchantmentId(), level) ||
                    (EnchantmentHelper.get(inventory.getStack(0)).keySet().stream().anyMatch(e -> !e.canCombine(choice.getEnchantment())) && 
                         !choice.isChosen() && MCDEnchantments.getConfig().isCompatibilityRequired());
            })
            .withClient(client)
            .build();
        touchButton = background.add(-2, 38);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /*Runic Table UI*/
        ctx.drawTexture(TEXTURE, background.x(), background.y(), 0, 109, 168, 150);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack stack = inventory.getStack(0);
        var slotsOptional = EnchantmentSlots.fromItemStack(stack);
        if (stack.isEmpty() || slotsOptional.isEmpty()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        var slots = slotsOptional.get();

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

    // To not render Inventory and Title text
    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        RenderSystem.setShaderTexture(0, TEXTURE);
        ItemStack itemStack = inventory.getStack(0);
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (isTouchscreen()) {
            ctx.drawTexture(TEXTURE, touchButton.x(), touchButton.y(), 216, 1, 13, 15);
        }
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

        var hoveredChoice = slotsRenderer.render(ctx, itemStack, mouseX, mouseY);

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
                    int buttonX = 229;
                    if (isInTouchButton(mouseX, mouseY)) {
                        buttonX = 242;
                    }
                    ctx.drawTexture(TEXTURE, touchButton.x(), touchButton.y(), buttonX, 1, 13, 13);
                }
            });
        } else {
            hoveredChoice.ifPresent(choice -> renderTooltip(ctx, choice, slots, mouseX, mouseY));
        }

        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    protected void renderTooltip(DrawContext ctx, Choice hovered, EnchantmentSlots slots, int x, int y) {
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
        ctx.drawTooltip(textRenderer, tooltipLines, x, y);
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
        return isInBounds(touchButton.x(), touchButton.y(), mouseX, mouseY, 0, 13, 0, 15);
    }
}
