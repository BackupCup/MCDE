package net.backupcup.mcd_enchantments.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.util.*;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RerollStationScreen extends HandledScreen<RerollStationScreenHandler> {
    private Inventory inventory;

    private static Pattern wrap = Pattern.compile("(\\b.{1,40})(?:\\s+|$)");

    private Optional<Slots> opened = Optional.empty();

    private static final Identifier TEXTURE =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/reroll_station.png");

    private EnchantmentSlotsRenderer slotsRenderer;

    public RerollStationScreen(RerollStationScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.inventory = handler.getInventory();
    }

    @Override
    protected void init() {
        super.init();
        titleX = 127;
        titleY = 10;

        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        var slotPos = Arrays.stream(Slots.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        s -> new EnchantmentTextureMapper.TexturePos(posX + 18 + 35 * s.ordinal(), posY + 38)
                ));
        int[] enchantOffsetX = {6, 38, 22};
        int[] enchantOffsetY = {22, 22, 6};
        var choiceOffsets = Arrays.stream(Slots.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        s -> new EnchantmentTextureMapper.TexturePos(enchantOffsetX[s.ordinal()], enchantOffsetY[s.ordinal()])
                ));
        slotsRenderer = EnchantmentSlotsRenderer.builder()
                .withHelper(this)
                .withDimPredicate(choice -> {
                    short level = 1;
                    boolean isMaxedOut = false;
                    if (choice instanceof EnchantmentSlot.ChoiceWithLevel withLevel) {
                        level = (short)(withLevel.getLevel() + 1);
                        isMaxedOut = withLevel.isMaxedOut();
                    }
                    return isMaxedOut || !handler.canReroll(client.player, choice.getEnchantment(), level);
                })
                .withSlotTexturePos(187, 105)
                .withOutlinePos(187, 138)
                .withPowerfulOutlinePos(221, 138)
                .withChoiceTexturePos(186, 0)
                .withChoicePosOffset(-17, -38)
                .withHoverOutlinePos(220, 104)
                .withSlotPositions(slotPos)
                .withChoiceOffsets(choiceOffsets)
                .build();
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /*Reroll Station UI*/
        RenderSystem.setShaderTexture(0, TEXTURE);
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        drawTexture(matrices, posX, posY, 0, 0, backgroundWidth + 10, backgroundHeight);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack stack = inventory.getStack(0);

        if (stack.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);
        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(stack);

        for (var slot : slots) {
            if (slotsRenderer.isInSlotBounds(slot.getSlot(), (int)mouseX, (int)mouseY)) {
                if (slot.getChosen().isPresent()) {
                    client.interactionManager.clickButton(handler.syncId, Slots.values().length * slot.ordinal());
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                if (opened.isEmpty()) {
                    opened = Optional.of(slot.getSlot());
                }
                else {
                    opened = opened.get() == slot.getSlot() ?
                            Optional.empty() : Optional.of(slot.getSlot());
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            if (opened.isPresent() && opened.get() == slot.getSlot()) {
                for (var choice : slot.choices()) {
                    if (slotsRenderer.isInChoiceBounds(slot.getSlot(), choice.getSlot(), (int) mouseX, (int) mouseY)) {
                        client.interactionManager.clickButton(handler.syncId, Slots.values().length * slot.ordinal() + choice.ordinal());
                        opened = Optional.empty();
                        return super.mouseClicked(mouseX, mouseY, button);
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

        if (itemStack.isEmpty()) {
            drawMouseoverTooltip(matrices, mouseX, mouseY);
            opened = Optional.empty();
            return;
        }

        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(itemStack);
        if (slots == null) {
            return;
        }
        Optional<EnchantmentSlot.Choice> hoveredChoice = Optional.empty();

        for (var slot : slots) {
            slotsRenderer.drawSlot(matrices, slot.getSlot());
            if (slot.getChosen().isPresent()) {
                var chosen = slot.getChosen().get();
                slotsRenderer.drawEnchantmentIconInSlot(matrices, slot.getSlot(), chosen);
                if (slotsRenderer.isInSlotBounds(slot.getSlot(), mouseX, mouseY))
                    hoveredChoice = Optional.of(chosen);
                RenderSystem.setShaderTexture(0, TEXTURE);
            }
            if (slotsRenderer.isInSlotBounds(slot.getSlot(), mouseX, mouseY))
                slotsRenderer.drawHoverOutline(matrices, slot.getSlot());

            if (opened.isPresent() && opened.get() == slot.getSlot()) {
                slotsRenderer.drawChoices(matrices, slot.getSlot());

                for (var choice : slot.choices()) {
                    slotsRenderer.drawEnchantmentIconOutline(matrices, slot.getSlot(), choice, mouseX, mouseY);
                    slotsRenderer.drawEnchantmentIconInChoice(matrices, slot.getSlot(), choice);
                }
                RenderSystem.setShaderTexture(0, TEXTURE);
            }
        }

        if (hoveredChoice.isEmpty()) {
            for (var slot : slots) {
                if (opened.isEmpty() || opened.get() != slot.getSlot()) {
                    continue;
                }
                for (var choice : slot.choices()) {
                    if (slotsRenderer.isInChoiceBounds(slot.getSlot(), choice.getSlot(), mouseX, mouseY)) {
                        hoveredChoice = Optional.of(choice);
                    }
                }
            }
        }

        if (hoveredChoice.isEmpty()) {
            drawMouseoverTooltip(matrices, mouseX, mouseY);
            return;
        }

        Identifier enchantment = hoveredChoice.get().getEnchantment();
        String translationKey = enchantment.toTranslationKey("enchantment");
        List<Text> tooltipLines = new ArrayList<>();
        short level = 1;
        boolean enoughLevels = handler.canReroll(client.player, enchantment, level);
        MutableText enchantmentName = Text.translatable(translationKey)
                .formatted(EnchantmentClassifier.isEnchantmentPowerful(enchantment) ? Formatting.RED : Formatting.LIGHT_PURPLE);
        if (hoveredChoice.get() instanceof EnchantmentSlot.ChoiceWithLevel withLevel) {
            enchantmentName.append(" ");
            if (withLevel.isMaxedOut()) {
                enchantmentName.append(Text.translatable("message.mcde.max_level"));
                enoughLevels = true;
            }
            else {
                enchantmentName
                        .append(Text.translatable("enchantment.level." + withLevel.getLevel()))
                        .append(" → ")
                        .append(Text.translatable("enchantment.level." + (withLevel.getLevel() + 1)));
                level = (short)(withLevel.getLevel() + 1);
                enoughLevels = handler.canReroll(client.player, enchantment, level);

            }
        }
        tooltipLines.add(enchantmentName);

        Text enchantmentDescription = Text.translatable(translationKey + ".desc");
        List<MutableText> desc = wrap.matcher(enchantmentDescription.getString())
                .results().map(res -> Text.literal(res.group(1)).formatted(Formatting.GRAY))
                .toList();
        tooltipLines.addAll(desc);
        if (!enoughLevels) {
            tooltipLines.add(Text.translatable("message.mcde.not_enough_levels").formatted(Formatting.DARK_RED, Formatting.ITALIC));
            tooltipLines.add(Text.translatable(
                    "message.mcde.levels_required",
                    EnchantmentUtils.getCost(enchantment, level)
            ).formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
        }
        renderTooltip(matrices, tooltipLines, mouseX, mouseY);

        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }
}
