package net.backupcup.mcde.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.screen.handler.RollBenchScreenHandler;
import net.backupcup.mcde.screen.util.EnchantmentSlotsRenderer;
import net.backupcup.mcde.screen.util.ScreenWithSlots;
import net.backupcup.mcde.util.EnchantmentSlot.Choice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.SlotPosition;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class RollBenchScreen extends HandledScreen<RollBenchScreenHandler> implements ScreenWithSlots {
    private Inventory inventory;

    private static Pattern wrap = Pattern.compile("(\\b.{1,40})(?:\\s+|$)");

    private Optional<SlotPosition> opened = Optional.empty();

    private static final Identifier TEXTURE = new Identifier(MCDEnchantments.MOD_ID, "textures/gui/roll_bench.png");

    private EnchantmentSlotsRenderer slotsRenderer;

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
                        handler.isSlotLocked(choice.getEnchantmentSlot().getSlotPosition());
                })
                .build();
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

        for (var slot : slots) {
            if (slotsRenderer.isInSlotBounds(slot.getSlotPosition(), (int) mouseX, (int) mouseY)) {
                if (slot.getChosen().isPresent()) {
                    var chosen = slot.getChosen().get();
                    if (!slotsRenderer.getDimPredicate().test(chosen)) {
                        client.interactionManager.clickButton(handler.syncId, SlotPosition.values().length * slot.ordinal());
                        opened = Optional.of(slot.getSlotPosition());
                    }
                    else {
                        opened = Optional.empty();
                    }
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                if (opened.isEmpty()) {
                    opened = Optional.of(slot.getSlotPosition());
                } else {
                    opened = opened.get() == slot.getSlotPosition() ? Optional.empty() : Optional.of(slot.getSlotPosition());
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            if (opened.isPresent() && opened.get() == slot.getSlotPosition()) {
                for (var choice : slot.choices()) {
                    if (slotsRenderer.isInChoiceBounds(slot.getSlotPosition(), choice.getChoicePosition(), (int) mouseX, (int) mouseY)) {
                        if (!slotsRenderer.getDimPredicate().test(choice)) {
                            client.interactionManager.clickButton(handler.syncId, SlotPosition.values().length * slot.ordinal() + choice.ordinal());
                            return super.mouseClicked(mouseX, mouseY, button);
                        } else {
                            return super.mouseClicked(mouseX, mouseY, button);
                        }
                    }
                }
            }
        }
        opened = Optional.empty();

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        RenderSystem.setShaderTexture(0, TEXTURE);
        ItemStack itemStack = inventory.getStack(0);
        var slots = EnchantmentSlots.fromItemStack(itemStack);
        if (itemStack.isEmpty() || slots == null) {
            drawMouseoverTooltip(matrices, mouseX, mouseY);
            opened = Optional.empty();
            return;
        }

        Optional<Choice> hoveredChoice = slotsRenderer.render(matrices, itemStack, mouseX, mouseY);

        if (hoveredChoice.isEmpty()) {
            drawMouseoverTooltip(matrices, mouseX, mouseY);
            return;
        }

        var hovered = hoveredChoice.get();

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

        Text enchantmentDescription = Text.translatable(translationKey + ".desc");
        List<MutableText> desc = wrap.matcher(enchantmentDescription.getString())
            .results().map(res -> Text.literal(res.group(1)).formatted(Formatting.GRAY))
            .toList();
        tooltipLines.addAll(desc);
        if (!canReroll) {
            tooltipLines.add(Text.translatable("message.mcde.not_enough_lapis")
                    .formatted(Formatting.DARK_RED, Formatting.ITALIC));
            tooltipLines.add(Text.translatable(
                    "message.mcde.lapis_required",
                    slots.getNextRerollCost(enchantment))
                .formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
        }
        if (handler.isSlotLocked(hovered.getEnchantmentSlot().getSlotPosition())) {
            tooltipLines.add(Text.translatable("message.mcde.cant_generate").formatted(Formatting.DARK_RED, Formatting.ITALIC));
        }
        renderTooltip(matrices, tooltipLines, mouseX, mouseY);

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
}
