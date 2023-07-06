package net.backupcup.mcd_enchantments.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.util.EnchantmentClassifier;
import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
import net.backupcup.mcd_enchantments.util.Slot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RunicTableScreen extends HandledScreen<RunicTableScreenHandler> {
    private Inventory inventory;
    private EnchantmentClassifier classifier = new EnchantmentClassifier();

    private boolean[] slotOpened = new boolean[3];
    private boolean TooltipActive;

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
        EnchantmentSlots slots = EnchantmentSlots.fromNbt(stack.getNbt().getCompound("Slots"));

        for (Slot slot : Slot.values()) {
            if (slots.getSlot(slot).isEmpty()) continue;

            if (slotOpened[slot.ordinal()]) {
                for (Slot innerSlot : Slot.values()) {
                    Optional<Identifier> optionalIdentifier = slots.getSlot(slot).get().getChoice(innerSlot);

                    if (optionalIdentifier.isPresent() &&
                        isInEBounds(posX + (slot.ordinal() * 35) + enchantOffsetX[innerSlot.ordinal()] - 1, posY + enchantOffsetY[innerSlot.ordinal()] - 1, (int) mouseX, (int) mouseY)) {
                        Identifier enchantmentID = optionalIdentifier.get();

                        //stack.addEnchantment(Registry.ENCHANTMENT.get(slots.getSlot(Slot.FIRST).get().getChoice(Slot.FIRST).get()), 1);

                        MCDEnchantments.LOGGER.info("Slot " + slot.ordinal() + ": " + enchantmentID + " | Is Powerful: " + classifier.isEnchantmentPowerful(String.valueOf(enchantmentID)));
                    }
                }
            }

            if (isInSBounds(slotOffsetsX[slot.ordinal()], posY + 37, (int) mouseX, (int) mouseY)) {
                for (int j = 0; j < 3; j++) {
                    slotOpened[j] = slot.ordinal() == j && !slotOpened[j];
                }
                break;
            }
        }

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
            for (int i = 0; i < 3; i++) slotOpened[i] = false;
            return;
        }

        EnchantmentSlots slots = EnchantmentSlots.fromNbt(itemStack.getNbt().getCompound("Slots"));
        Identifier tooltipEnchantmentID = null;

        for (Slot slot : Slot.values()) {
            if (slots.getSlot(slot).isEmpty()) continue;

            drawTexture(matrices, slotOffsetsX[slot.ordinal()] + 1, posY + 38, 187, 105, 31, 31);
            if (isInSBounds(slotOffsetsX[slot.ordinal()], posY + 37, mouseX, mouseY))
                drawTexture(matrices, slotOffsetsX[slot.ordinal()], posY + 37, 220, 104, 33, 33);

            if (slotOpened[slot.ordinal()]) {
                drawTexture(matrices, posX + (slot.ordinal() * 35), posY, 186, 0, 67, 51);

                for (Slot innerSlot : Slot.values()) {
                    Optional<Identifier> optionalIdentifier = slots.getSlot(slot).get().getChoice(innerSlot);
                    if (optionalIdentifier.isPresent()) {
                        Identifier enchantmentID = optionalIdentifier.get();

                        RenderSystem.setShaderTexture(0, EnchantmentTextureMapper.getTexture(enchantmentID));
                        EnchantmentTextureMapper.TexturePos pos = EnchantmentTextureMapper.getPos(enchantmentID);

                        if (!isInEBounds(posX + (slot.ordinal() * 35) + enchantOffsetX[innerSlot.ordinal()] - 1, posY + enchantOffsetY[innerSlot.ordinal()] - 1, mouseX, mouseY))
                            if (classifier.isEnchantmentPowerful(String.valueOf(enchantmentID))) drawTexture(matrices, posX + (slot.ordinal() * 35) + enchantOffsetX[innerSlot.ordinal()] - 1, posY + enchantOffsetY[innerSlot.ordinal()] - 1, 199, 225, 25, 25);
                            else drawTexture(matrices, posX + (slot.ordinal() * 35) + enchantOffsetX[innerSlot.ordinal()] - 1, posY + enchantOffsetY[innerSlot.ordinal()] - 1, 172, 225, 25, 25);
                        else {
                            drawTexture(matrices, posX + (slot.ordinal() * 35) + enchantOffsetX[innerSlot.ordinal()] - 1, posY + enchantOffsetY[innerSlot.ordinal()] - 1, 226, 225, 25, 25);
                            tooltipEnchantmentID = enchantmentID;
                        }

                        drawTexture(matrices, posX + (slot.ordinal() * 35) + enchantOffsetX[innerSlot.ordinal()], posY + enchantOffsetY[innerSlot.ordinal()], pos.x(), pos.y(), 23, 23);
                    } else {
                        RenderSystem.setShaderTexture(0, TEXTURE);
                        drawTexture(matrices, posX + (slot.ordinal() * 35) + enchantOffsetX[innerSlot.ordinal()] - 1, posY + enchantOffsetY[innerSlot.ordinal()] - 1, 190, 52, 25, 25);
                    }
                }
                RenderSystem.setShaderTexture(0, TEXTURE);
            }
        }

        if (tooltipEnchantmentID != null) {
            String[] parts = tooltipEnchantmentID.toString().split(":");
            String namespace = parts[0];
            String id = parts[1];
            int maxLineWords = 5;
            int wordCount = 0;
            List<Text> tooltipLines = new ArrayList<>();

            if (classifier.isEnchantmentPowerful(String.valueOf(tooltipEnchantmentID))) {
                Text enchantmentName = Text.translatable("enchantment." + namespace + "." + id).formatted(Formatting.LIGHT_PURPLE);
                tooltipLines.add(enchantmentName);
            }
            else {
                Text enchantmentName = Text.translatable("enchantment." + namespace + "." + id).formatted(Formatting.AQUA);
                tooltipLines.add(enchantmentName);
            }

            Text enchantmentDescription = Text.translatable("enchantment." + namespace + "." + id + ".desc");

            List<String> words = Arrays.asList(enchantmentDescription.getString().split(" "));
            StringBuilder wrappedDescription = new StringBuilder();

            for (String word : words) {
                if (wrappedDescription.length() > 0) {
                    wrappedDescription.append(" ");
                }

                wrappedDescription.append(word);
                wordCount++;

                if (wordCount >= maxLineWords) {
                    tooltipLines.add(Text.literal(wrappedDescription.toString()).formatted(Formatting.GRAY));
                    wrappedDescription.setLength(0);
                    wordCount = 0;
                }
            }

            if (wrappedDescription.length() > 0) {
                tooltipLines.add(Text.literal(wrappedDescription.toString()).formatted(Formatting.GRAY));
            }
            renderTooltip(matrices, tooltipLines, mouseX, mouseY);
        }

        drawMouseoverTooltip(matrices, mouseX, mouseY);
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