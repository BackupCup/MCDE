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
import net.backupcup.mcde.util.EnchantmentSlot.Chosen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.Slots;
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

@Environment(EnvType.CLIENT)
public class RunicTableScreen extends HandledScreen<RunicTableScreenHandler> implements ScreenWithSlots {
    private Inventory inventory;

    private static Pattern wrap = Pattern.compile("(\\b.{1,40})(?:\\s+|$)");

    private Optional<Slots> opened = Optional.empty();

    private static final Identifier TEXTURE =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/runic_table.png");

    private EnchantmentSlotsRenderer slotsRenderer;

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
                if (choice instanceof Chosen withLevel) {
                    level = (int)(withLevel.getLevel() + 1);
                    isMaxedOut = withLevel.isMaxedOut();
                }
                return isMaxedOut || !RunicTableScreenHandler.canEnchant(client.player, choice.getEnchantmentId(), level) ||
                    (EnchantmentHelper.get(inventory.getStack(0)).keySet().stream().anyMatch(e -> !e.canCombine(choice.getEnchantment())) && 
                         !(choice instanceof Chosen));
            })
        .build();
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        /*Runic Table UI*/
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        ctx.drawTexture(TEXTURE, posX, posY, 0, 0, backgroundWidth + 10, backgroundHeight);
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
                    if (slotsRenderer.isInChoiceBounds(slot.getSlot(), choice.getChoiceSlot(), (int) mouseX, (int) mouseY)) {
                        if (!slotsRenderer.getDimPredicate().test(choice)) {
                            client.interactionManager.clickButton(handler.syncId, Slots.values().length * slot.ordinal() + choice.ordinal());
                            opened = Optional.empty();
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
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        int width = textRenderer.getWidth(title);
        int height = textRenderer.fontHeight;
        int outlineX = titleX - 4;
        int outlineY = titleY - 4;
        RenderSystem.setShaderTexture(0, TEXTURE);

        ctx.drawTexture(TEXTURE, outlineX, outlineY, 0, 172, 3, height + 6);
        IntStream.range(-1, width + 1).forEach(i ->
                ctx.drawTexture(TEXTURE, titleX + i, outlineY, 2, 172, 1, height + 6));
        ctx.drawTexture(TEXTURE, titleX + width + 1, outlineY, 3, 172, 2, height + 6);
        super.drawForeground(ctx, mouseX, mouseY);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        RenderSystem.setShaderTexture(0, TEXTURE);
        ItemStack itemStack = inventory.getStack(0);

        if (itemStack.isEmpty()) {
            opened = Optional.empty();
            drawMouseoverTooltip(ctx, mouseX, mouseY);
            return;
        }

        var hoveredChoice = slotsRenderer.render(ctx, itemStack, mouseX, mouseY);

        if (hoveredChoice.isEmpty()) {
            drawMouseoverTooltip(ctx, mouseX, mouseY);
            return;
        }
        var hovered = hoveredChoice.get();
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

        Text enchantmentDescription = Text.translatable(enchantment.getTranslationKey() + ".desc");
        List<MutableText> desc = wrap.matcher(enchantmentDescription.getString())
            .results().map(res -> Text.literal(res.group(1)).formatted(Formatting.GRAY))
            .toList();
        tooltipLines.addAll(desc);
        if (!enoughLevels) {
            tooltipLines.add(Text.translatable("message.mcde.not_enough_levels").formatted(Formatting.DARK_RED, Formatting.ITALIC));
            tooltipLines.add(Text.translatable(
                        "message.mcde.levels_required",
                        MCDEnchantments.getConfig().getEnchantCost(enchantmentId, level)
                        ).formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
        }

        if (!hovered.isChosen()) {
            if (EnchantmentHelper.getLevel(hoveredChoice.get().getEnchantment(), itemStack) > 0) {
                tooltipLines.add(Text.translatable("message.mcde.already_exists").formatted(Formatting.DARK_RED, Formatting.ITALIC));
            } else if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
                var conflict = EnchantmentHelper.get(itemStack).keySet().stream()
                    .filter(e -> !e.canCombine(hoveredChoice.get().getEnchantment())).findFirst();
                if (conflict.isPresent()) {
                    var conflicting = conflict.get();
                    tooltipLines.add(Text.translatable(
                        "message.mcde.cant_combine",
                        Text.translatable(conflicting.getTranslationKey())
                    ).formatted(Formatting.DARK_RED, Formatting.ITALIC));
                }
            }
        }
        ctx.drawTooltip(textRenderer, tooltipLines, mouseX, mouseY);
    }

    @Override
    public Optional<Slots> getOpened() {
        return opened;
    }

    @Override
    public void setOpened(Optional<Slots> opened) {
        this.opened = opened;
    }
}
