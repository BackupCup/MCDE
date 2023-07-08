package net.backupcup.mcd_enchantments.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.util.EnchantmentClassifier;
import net.backupcup.mcd_enchantments.util.EnchantmentSlot.Choice;
import net.backupcup.mcd_enchantments.util.EnchantmentSlot.ChoiceWithLevel;
import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
import net.backupcup.mcd_enchantments.util.EnchantmentUtils;
import net.backupcup.mcd_enchantments.util.Slots;
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

public class RunicTableScreen extends HandledScreen<RunicTableScreenHandler> {
    private Inventory inventory;

    private static Pattern wrap = Pattern.compile("(\\b.{1,40})(?:\\s+|$)");

    private Optional<Slots> opened = Optional.empty();

    private static final Identifier TEXTURE =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/runic_table.png");

    public RunicTableScreen(RunicTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.inventory = handler.getInventory();
    }

    @Override
    protected void init() {
        super.init();
        titleX = 125;
        titleY = 10;
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

        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        int[] slotOffsetsX = {posX + 17, posX + 52, posX + 87};

        int[] enchantOffsetX = {6, 38, 22};
        int[] enchantOffsetY = {22, 22, 6};

        if (stack.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);
        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(stack);

        for (var slot : slots) {
            if (isInSBounds(slotOffsetsX[slot.ordinal()], posY + 37, (int)mouseX, (int)mouseY)) {
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
                    Identifier enchantmentId = choice.getEnchantment();

                    if (isInEBounds(posX + (slot.ordinal() * 35) + enchantOffsetX[choice.ordinal()] - 1, posY + enchantOffsetY[choice.ordinal()] - 1, (int) mouseX, (int) mouseY)) {
                        MCDEnchantments.LOGGER.info("Slot " + slot.ordinal() + ": " + enchantmentId + " | Is Powerful: " + EnchantmentClassifier.isEnchantmentPowerful(enchantmentId));
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
        int posX = ((width - backgroundWidth) / 2) - 2;
        int posY = (height - backgroundHeight) / 2;
        int[] slotOffsetsX = {posX + 17, posX + 52, posX + 87};

        int[] enchantOffsetX = {6, 38, 22};
        int[] enchantOffsetY = {22, 22, 6};
        
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
        Optional<Choice> tooltipEnchantmentID = Optional.empty();

        for (var slot : slots) {
            drawTexture(matrices, slotOffsetsX[slot.ordinal()] + 1, posY + 38, 187, 105, 31, 31);
            if (slot.getChosen().isPresent()) {
                var chosen = slot.getChosen().get();
                int outlineTextureX = EnchantmentClassifier.isEnchantmentPowerful(chosen.getEnchantment()) ?
                    221 : 187;
                drawTexture(matrices, slotOffsetsX[slot.ordinal()] + 1, posY + 38, outlineTextureX, 138, 31, 31);
                drawEnchantmentIcon(matrices, chosen, posX + (slot.ordinal() * 35) + 21, posY + 41, mouseX, mouseY, false);
                if (isInSBounds(slotOffsetsX[slot.ordinal()], posY + 37, mouseX, mouseY))
                    tooltipEnchantmentID = Optional.of(chosen);
                RenderSystem.setShaderTexture(0, TEXTURE);
            }
            if (isInSBounds(slotOffsetsX[slot.ordinal()], posY + 37, mouseX, mouseY))
                drawTexture(matrices, slotOffsetsX[slot.ordinal()], posY + 37, 220, 104, 33, 33);

            if (opened.isPresent() && opened.get() == slot.getSlot()) {
                drawTexture(matrices, posX + (slot.ordinal() * 35), posY, 186, 0, 67, 51);

                for (var choice : slot.choices()) {
                    Identifier enchantmentID = choice.getEnchantment();
                    RenderSystem.setShaderTexture(0, EnchantmentTextureMapper.getTexture(enchantmentID));
                    var hovered = drawEnchantmentIcon(
                        matrices,
                        choice,
                        posX + (slot.ordinal() * 35) + enchantOffsetX[choice.ordinal()] - 1,
                        posY + enchantOffsetY[choice.ordinal()] - 1,
                        mouseX,
                        mouseY,
                        true
                    );
                    if (hovered.isPresent()) {
                        tooltipEnchantmentID = Optional.of(hovered.get());
                    }
                }
                RenderSystem.setShaderTexture(0, TEXTURE);
            }
        }

        if (tooltipEnchantmentID.isPresent()) {
            Identifier enchantment = tooltipEnchantmentID.get().getEnchantment();
            String translationKey = enchantment.toTranslationKey("enchantment");
            List<Text> tooltipLines = new ArrayList<>();
            short level = 1;
            boolean enoughLevels = handler.canEnchant(client.player, enchantment, level);
            MutableText enchantmentName = Text.translatable(translationKey)
                .formatted(EnchantmentClassifier.isEnchantmentPowerful(enchantment) ? Formatting.RED : Formatting.LIGHT_PURPLE);
            if (tooltipEnchantmentID.get() instanceof ChoiceWithLevel withLevel) {
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
                    enoughLevels = handler.canEnchant(client.player, enchantment, level);

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
        }

        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    private Optional<Choice> drawEnchantmentIcon(MatrixStack matrices, Choice choice, int posX, int posY, int mouseX, int mouseY, boolean outline) {
        Optional<Choice> tooltipEnchantmentID = Optional.empty();
        Identifier enchantmentID = choice.getEnchantment();
        RenderSystem.setShaderTexture(0, EnchantmentTextureMapper.getTexture(enchantmentID));
        var pos = EnchantmentTextureMapper.getPos(enchantmentID);

        if (outline) {
            if (isInEBounds(posX, posY, mouseX, mouseY)) {
                drawTexture(matrices, posX, posY, 226, 225, 25, 25);
                tooltipEnchantmentID = Optional.of(choice);
            }
            else {
                if (EnchantmentClassifier.isEnchantmentPowerful(enchantmentID))
                    drawTexture(matrices, posX, posY, 199, 225, 25, 25);
                else
                    drawTexture(matrices, posX, posY, 172, 225, 25, 25);
            }
        }
        short level = 1;
        boolean isMaxedOut = false;
        if (choice instanceof ChoiceWithLevel withLevel) {
            level = (short)(withLevel.getLevel() + 1);
            isMaxedOut = withLevel.isMaxedOut();
        }
        if (isMaxedOut || !handler.canEnchant(client.player, choice.getEnchantment(), level)) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
            
        }
        drawTexture(matrices, posX + 1, posY + 1, pos.x(), pos.y(), 23, 23);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        return tooltipEnchantmentID;
    }

    private static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX && mouseX <= posX + endX && mouseY >= posY + startY && mouseY <= posY + endY;
    }

    //SLOT BUTTONS

    private boolean isInSBounds(int posX, int posY, int mouseX, int mouseY) {
        boolean ButtonBox1 = isInBounds(posX, posY, mouseX, mouseY, 13, 18, 0, 31);
        boolean ButtonBox2 = isInBounds(posX, posY, mouseX, mouseY, 0, 31, 13, 18);
        boolean ButtonBox3 = isInBounds(posX, posY, mouseX, mouseY, 6, 25, 6, 25);
        boolean ButtonBox4 = isInBounds(posX, posY, mouseX, mouseY, 9, 22, 2, 29);
        boolean ButtonBox5 = isInBounds(posX, posY, mouseX, mouseY, 2, 29, 9, 22);

        ItemStack stack = inventory.getStack(0);
        if (stack.isEmpty())
            return false;

        return ButtonBox1 || ButtonBox2 || ButtonBox3 || ButtonBox4 || ButtonBox5;
    }

    //ENCHANTMENT BUTTONS
    private boolean isInEBounds(int posX, int posY, int mouseX, int mouseY) {
        boolean ButtonBox1 = isInBounds(posX, posY, mouseX, mouseY, 10, 13, 0, 23);
        boolean ButtonBox2 = isInBounds(posX, posY, mouseX, mouseY, 0, 23, 10, 13);
        boolean ButtonBox3 = isInBounds(posX, posY, mouseX, mouseY, 5, 18, 5, 18);
        boolean ButtonBox4 = isInBounds(posX, posY, mouseX, mouseY, 7, 16, 2, 21);
        boolean ButtonBox5 = isInBounds(posX, posY, mouseX, mouseY, 2, 21, 7, 16);

        ItemStack stack = inventory.getStack(0);
        if (stack.isEmpty())
            return false;

        return ButtonBox1 || ButtonBox2 || ButtonBox3 || ButtonBox4 || ButtonBox5;
    }
}
