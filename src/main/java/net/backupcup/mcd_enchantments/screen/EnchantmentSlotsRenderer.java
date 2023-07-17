package net.backupcup.mcd_enchantments.screen;

import java.util.Map;
import java.util.function.Predicate;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcd_enchantments.screen.EnchantmentTextureMapper.TexturePos;
import net.backupcup.mcd_enchantments.util.EnchantmentClassifier;
import net.backupcup.mcd_enchantments.util.EnchantmentSlot.Choice;
import net.backupcup.mcd_enchantments.util.EnchantmentSlot.ChoiceWithLevel;
import net.backupcup.mcd_enchantments.util.Slots;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class EnchantmentSlotsRenderer {
    private TexturePos slotTexturePos;
    private TexturePos outlinePos;

    private TexturePos powerfulOutlinePos;
    private TexturePos choicePosOffset;
    private TexturePos choiceTexturePos;
    private TexturePos hoverOutlinePos;
    private Map<Slots, TexturePos> slotPos;
    private Map<Slots, TexturePos> choiceOffsets;
    private DrawableHelper helper;
    private Predicate<Choice> dimPredicate;

    private EnchantmentSlotsRenderer(TexturePos outlinePos, TexturePos slotTexturePos, TexturePos powerfulOutlinePos, TexturePos hoverOutlinePos,
            TexturePos choicePosOffset, TexturePos choiceTexturePos, Map<Slots, TexturePos> slotPos, Map<Slots, TexturePos> choiceOffsets,
            DrawableHelper helper, Predicate<Choice> dimPredicate) {
        this.outlinePos = outlinePos;
        this.slotTexturePos = slotTexturePos;
        this.powerfulOutlinePos = powerfulOutlinePos;
        this.hoverOutlinePos = hoverOutlinePos;
        this.choicePosOffset = choicePosOffset;
        this.choiceTexturePos = choiceTexturePos;
        this.slotPos = slotPos;
        this.choiceOffsets = choiceOffsets;
        this.helper = helper;
        this.dimPredicate = dimPredicate;
    }

    public void drawSlot(MatrixStack matrices, Slots slot) {
        var pos = slotPos.get(slot);
        helper.drawTexture(matrices, pos.x(), pos.y(), slotTexturePos.x(), slotTexturePos.y(), 31, 31);
    }

    public void drawChoices(MatrixStack matrices, Slots slot) {
        var pos = slotPos.get(slot).add(choicePosOffset);
        helper.drawTexture(matrices, pos.x(), pos.y(), choiceTexturePos.x(), choiceTexturePos.y(), 67, 51);
    }

    public void drawHoverOutline(MatrixStack matrices, Slots slot) {
        var pos = slotPos.get(slot);
        helper.drawTexture(matrices, pos.x() - 1, pos.y() - 1, hoverOutlinePos.x(), hoverOutlinePos.y(), 33, 33);
    }

    public void drawEnchantmentIconInSlot(MatrixStack matrices, Slots slot, ChoiceWithLevel choice) {
        var texPos = EnchantmentClassifier.isEnchantmentPowerful(choice.getEnchantment()) ?
            powerfulOutlinePos : outlinePos;
        var pos = slotPos.get(slot);
        helper.drawTexture(matrices, pos.x(), pos.y(), texPos.x(), texPos.y(), 31, 31);
        drawEnchantmentIcon(matrices, pos.add(4, 4), slot, choice);
    }

    public void drawEnchantmentIconOutline(MatrixStack matrices, Slots slot, Choice choice, int mouseX, int mouseY) {
        var drawPos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice.getSlot()));
        RenderSystem.setShaderTexture(0, EnchantmentTextureMapper.getTexture(choice.getEnchantment()));
        if (isInChoiceBounds(slot, choice.getSlot(), mouseX, mouseY)) {
            helper.drawTexture(matrices, drawPos.x() - 1, drawPos.y() - 1, 226, 225, 25, 25);
            return;
        }
        if (EnchantmentClassifier.isEnchantmentPowerful(choice.getEnchantment())) {
            helper.drawTexture(matrices, drawPos.x() - 1, drawPos.y() - 1, 199, 225, 25, 25);
            return;
        }
        helper.drawTexture(matrices, drawPos.x() - 1, drawPos.y() - 1, 172, 225, 25, 25);
    }

    public void drawEnchantmentIconInChoice(MatrixStack matrices, Slots slot, Choice choice) {
        var drawPos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice.getSlot()));
        drawEnchantmentIcon(matrices, drawPos, slot, choice);
    }

    public boolean isInSlotBounds(Slots slot, int mouseX, int mouseY) {
        var pos = slotPos.get(slot).add(TexturePos.of(-1, -1));
        boolean ButtonBox1 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 13, 18, 0, 31);
        boolean ButtonBox2 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 0, 31, 13, 18);
        boolean ButtonBox3 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 6, 25, 6, 25);
        boolean ButtonBox4 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 9, 22, 2, 29);
        boolean ButtonBox5 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 2, 29, 9, 22);

        return ButtonBox1 || ButtonBox2 || ButtonBox3 || ButtonBox4 || ButtonBox5;
    }


    public boolean isInChoiceBounds(Slots slot, Slots choice, int mouseX, int mouseY) {
        var pos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice)).add(-1, -1);
        boolean ButtonBox1 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 10, 13, 0, 23);
        boolean ButtonBox2 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 0, 23, 10, 13);
        boolean ButtonBox3 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 5, 18, 5, 18);
        boolean ButtonBox4 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 7, 16, 2, 21);
        boolean ButtonBox5 = isInBounds(pos.x(), pos.y(), mouseX, mouseY, 2, 21, 7, 16);

        return ButtonBox1 || ButtonBox2 || ButtonBox3 || ButtonBox4 || ButtonBox5;
    }

    private static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX &&
               mouseX <= posX + endX &&
               mouseY >= posY + startY &&
               mouseY <= posY + endY;
    }

    private void drawEnchantmentIcon(MatrixStack matrices, TexturePos drawPos, Slots slot, Choice choice) {
        Identifier enchantmentID = choice.getEnchantment();
        RenderSystem.setShaderTexture(0, EnchantmentTextureMapper.getTexture(enchantmentID));
        var pos = EnchantmentTextureMapper.getPos(enchantmentID);
        if (dimPredicate.test(choice)) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
            
        }
        helper.drawTexture(matrices, drawPos.x(), drawPos.y(), pos.x(), pos.y(), 23, 23);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<Slots, TexturePos> slotMap;
        private Map<Slots, TexturePos> choiceOffstets;
        private TexturePos slotTexturePos;
        private TexturePos outlinePos;
        private TexturePos hoverOutlinePos;
        private TexturePos powerfulOutlinePos;
        private TexturePos choicePosOffset;
        private TexturePos choiceTexturePos;
        private DrawableHelper helper;
        private Predicate<Choice> dimPredicate;

        public Builder withSlotPositions(TexturePos first, TexturePos second, TexturePos third) {
            slotMap = Map.of(Slots.FIRST, first, Slots.SECOND, second, Slots.THIRD, third);
            return this;
        }

        public Builder withSlotPositions(Map<Slots, TexturePos> slotMap) {
            this.slotMap = slotMap;
            return this;
        }

        public Builder withChoiceOffsets(TexturePos first, TexturePos second, TexturePos third) {
            choiceOffstets = Map.of(Slots.FIRST, first, Slots.SECOND, second, Slots.THIRD, third);
            return this;
        }

        public Builder withChoiceOffsets(Map<Slots, TexturePos> choiceMap) {
            choiceOffstets = choiceMap;
            return this;
        }

        public Builder withHoverOutlinePos(int x, int y) {
            hoverOutlinePos = TexturePos.of(x, y);
            return this;
        }

        public Builder withSlotTexturePos(int x, int y) {
            slotTexturePos = TexturePos.of(x, y);
            return this;
        }
        public Builder withOutlinePos(int x, int y) {
            outlinePos = TexturePos.of(x, y);
            return this;
        }

        public Builder withPowerfulOutlinePos(int x, int y) {
            powerfulOutlinePos = TexturePos.of(x, y);
            return this;
        }
        public Builder withChoicePosOffset(int x, int y) {
            choicePosOffset = TexturePos.of(x, y);
            return this;
        }

        public Builder withChoiceTexturePos(int x, int y) {
            choiceTexturePos = TexturePos.of(x, y);
            return this;
        }

        public Builder withDimPredicate(Predicate<Choice> predicate) {
            dimPredicate = predicate;
            return this;
        }

        public Builder withHelper(DrawableHelper helper) {
            this.helper = helper;
            return this;
        }

        public EnchantmentSlotsRenderer build() {
            return new EnchantmentSlotsRenderer(outlinePos, slotTexturePos, powerfulOutlinePos, hoverOutlinePos, choicePosOffset, choiceTexturePos, slotMap, choiceOffstets, helper, dimPredicate);
        }
    }
}
