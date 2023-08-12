package net.backupcup.mcde.screen.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.util.EnchantmentSlot.Choice;
import net.backupcup.mcde.util.EnchantmentSlot.ChoiceWithLevel;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.Slots;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class EnchantmentSlotsRenderer {
    private static final Identifier missingEnchantTexture = new Identifier(MCDEnchantments.MOD_ID, "textures/gui/icons/missing_no.png");

    private TexturePos slotTexturePos;
    private TexturePos outlinePos;

    private TexturePos powerfulOutlinePos;
    private TexturePos choicePosOffset;
    private TexturePos choiceTexturePos;
    private TexturePos hoverOutlinePos;
    private TexturePos iconOutlinePos;
    private TexturePos iconPowerfulOutlinePos;
    private TexturePos hoverIconOutlinePos;
    private Map<Slots, TexturePos> slotPos;
    private Map<Slots, TexturePos> choiceOffsets;
    private ScreenWithSlots screen;
    private Predicate<Choice> dimPredicate;
    private Identifier defaultGuiTexture;
    private float dimColorMultiplier;

    private EnchantmentSlotsRenderer(
            TexturePos outlinePos,
            TexturePos slotTexturePos,
            TexturePos powerfulOutlinePos,
            TexturePos hoverOutlinePos,
            TexturePos choicePosOffset,
            TexturePos choiceTexturePos,
            TexturePos iconOutlinePos,
            TexturePos iconPowerfulOutlinePos,
            TexturePos hoverIconOutlinePos,
            Map<Slots, TexturePos> slotPos,
            Map<Slots, TexturePos> choiceOffsets,
            ScreenWithSlots screen,
            Predicate<Choice> dimPredicate,
            Identifier defaultGuiTexture,
            float dimColorMultiplier
            ) {
        this.outlinePos = outlinePos;
        this.slotTexturePos = slotTexturePos;
        this.powerfulOutlinePos = powerfulOutlinePos;
        this.hoverOutlinePos = hoverOutlinePos;
        this.choicePosOffset = choicePosOffset;
        this.choiceTexturePos = choiceTexturePos;
        this.iconOutlinePos = iconOutlinePos;
        this.iconPowerfulOutlinePos = iconPowerfulOutlinePos;
        this.hoverIconOutlinePos = hoverIconOutlinePos;
        this.slotPos = slotPos;
        this.choiceOffsets = choiceOffsets;
        this.screen = screen;
        this.dimPredicate = dimPredicate;
        this.defaultGuiTexture = defaultGuiTexture;
        this.dimColorMultiplier = dimColorMultiplier;
    }

    public void drawSlot(DrawContext ctx, Slots slot) {
        var pos = slotPos.get(slot);
        ctx.drawTexture(defaultGuiTexture, pos.x(), pos.y(), slotTexturePos.x(), slotTexturePos.y(), 31, 31);
    }

    public void drawChoices(DrawContext ctx, Slots slot) {
        var pos = slotPos.get(slot).add(choicePosOffset);
        ctx.drawTexture(defaultGuiTexture, pos.x(), pos.y(), choiceTexturePos.x(), choiceTexturePos.y(), 67, 51);
    }

    public void drawHoverOutline(DrawContext ctx, Slots slot) {
        var pos = slotPos.get(slot);
        RenderSystem.setShaderTexture(0, defaultGuiTexture);
        ctx.drawTexture(defaultGuiTexture, pos.x() - 1, pos.y() - 1, hoverOutlinePos.x(), hoverOutlinePos.y(), 33, 33);
    }

    public void drawIconInSlot(DrawContext ctx, Slots slot, ChoiceWithLevel choice) {
        var texPos = MCDEnchantments.getConfig().isEnchantmentPowerful(choice.getEnchantmentId()) ?
            powerfulOutlinePos : outlinePos;
        var pos = slotPos.get(slot);
        ctx.drawTexture(defaultGuiTexture, pos.x(), pos.y(), texPos.x(), texPos.y(), 31, 31);
        drawIcon(ctx, pos.add(4, 4), slot, choice);
    }

    public void drawIconHoverOutline(DrawContext ctx, Slots slot, Choice choice) {
        var drawPos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice.getSlot()));
        ctx.drawTexture(defaultGuiTexture, drawPos.x() - 1, drawPos.y() - 1, hoverIconOutlinePos.x(), hoverIconOutlinePos.y(), 25, 25);
    }

    public void drawIconOutline(DrawContext ctx, Slots slot, Choice choice) {
        var drawPos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice.getSlot()));
        var texPos = MCDEnchantments.getConfig().isEnchantmentPowerful(choice.getEnchantmentId()) ?
            iconPowerfulOutlinePos : iconOutlinePos;
        ctx.drawTexture(defaultGuiTexture, drawPos.x() - 1, drawPos.y() - 1, texPos.x(), texPos.y(), 25, 25);
    }

    public void drawIconInChoice(DrawContext ctx, Slots slot, Choice choice) {
        var drawPos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice.getSlot()));
        drawIcon(ctx, drawPos, slot, choice);
    }

    public Predicate<Choice> getDimPredicate() {
        return dimPredicate;
    }

    public Optional<Choice> render(DrawContext ctx, ItemStack itemStack, int mouseX, int mouseY) {
        Optional<Choice> hovered = Optional.empty();
        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(itemStack);
        if (slots == null) {
            return hovered;
        }

        for (var slot : slots) {
            drawSlot(ctx, slot.getSlot());
            if (slot.getChosen().isPresent()) {
                var chosen = slot.getChosen().get();
                drawIconInSlot(ctx, slot.getSlot(), chosen);
                if (isInSlotBounds(slot.getSlot(), mouseX, mouseY))
                    hovered = Optional.of(chosen);
            }
            if (isInSlotBounds(slot.getSlot(), mouseX, mouseY))
                drawHoverOutline(ctx, slot.getSlot());

            if (screen.getOpened().isPresent() && screen.getOpened().get() == slot.getSlot()) {
                drawChoices(ctx, slot.getSlot());

                for (var choice : slot.choices()) {
                    if (isInChoiceBounds(slot.getSlot(), choice.getSlot(), mouseX, mouseY)) {
                        drawIconHoverOutline(ctx, slot.getSlot(), choice);
                        hovered = Optional.of(choice);
                    } else {
                        drawIconOutline(ctx, slot.getSlot(), choice);
                    }
                    drawIconInChoice(ctx, slot.getSlot(), choice);
                }
            }
        }
        return hovered;
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

    private void drawIcon(DrawContext ctx, TexturePos drawPos, Slots slot, Choice choice) {
        Identifier enchantmentID = choice.getEnchantmentId();
        Identifier textureID = getTextureId(enchantmentID);

        if (dimPredicate.test(choice)) {
            RenderSystem.setShaderColor(dimColorMultiplier, dimColorMultiplier, dimColorMultiplier, 1.0f);
        }
        ctx.drawTexture(MinecraftClient.getInstance().getResourceManager().getResource(textureID).isPresent() ?
                textureID : missingEnchantTexture, drawPos.x(), drawPos.y(), 0f, 0f, 23, 23, 32, 32);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Identifier getTextureId(Identifier enchantmentID) {
        return Identifier.of(
            MCDEnchantments.MOD_ID,
            String.format(
                "textures/gui/icons/%s/%s.png",
                enchantmentID.getNamespace(),
                enchantmentID.getPath()
            )
        );
    }

    public static class Builder {
        private Map<Slots, TexturePos> slotMap;
        private Map<Slots, TexturePos> choiceOffsets = Map.of(
                Slots.FIRST, TexturePos.of(6, 22),
                Slots.SECOND, TexturePos.of(38, 22),
                Slots.THIRD, TexturePos.of(22, 6));
        private TexturePos slotTexturePos = TexturePos.of(187, 105);
        private TexturePos outlinePos = TexturePos.of(187, 138);
        private TexturePos hoverOutlinePos = TexturePos.of(220, 104);
        private TexturePos powerfulOutlinePos = TexturePos.of(221, 138);
        private TexturePos iconOutlinePos = TexturePos.of(170, 171);
        private TexturePos iconPowerfulOutlinePos = TexturePos.of(197, 171);
        private TexturePos hoverIconOutlinePos = TexturePos.of(224, 171);
        private TexturePos choicePosOffset = TexturePos.of(-17, -38);
        private TexturePos choiceTexturePos = TexturePos.of(186, 0);
        private ScreenWithSlots screen;
        private Predicate<Choice> dimPredicate;
        private Identifier defaultGuiTexture;
        private float dimColorMultiplier = 0.5f;

        public Builder withSlotPositions(TexturePos first, TexturePos second, TexturePos third) {
            slotMap = Map.of(Slots.FIRST, first, Slots.SECOND, second, Slots.THIRD, third);
            return this;
        }

        public Builder withSlotPositions(Map<Slots, TexturePos> slotMap) {
            this.slotMap = slotMap;
            return this;
        }

        public Builder withDefaultSlotPositions(int backgroundPosX, int backgroundPosY) {
            slotMap = Arrays.stream(Slots.values())
                .collect(Collectors.toMap(
                            Function.identity(),
                            s -> TexturePos.of(
                                backgroundPosX + 18 + 35 * s.ordinal(),
                                backgroundPosY + 38
                                )));
            return this;
        }

        public Builder withChoiceOffsets(TexturePos first, TexturePos second, TexturePos third) {
            choiceOffsets = Map.of(Slots.FIRST, first, Slots.SECOND, second, Slots.THIRD, third);
            return this;
        }

        public Builder withChoiceOffsets(Map<Slots, TexturePos> choiceMap) {
            choiceOffsets = choiceMap;
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

        public Builder withIconOutlinePos(int x, int y) {
            iconOutlinePos = TexturePos.of(x, y);
            return this;
        }

        public Builder withIconPowerfulOutlinePos(int x, int y) {
            powerfulOutlinePos = TexturePos.of(x, y);
            return this;
        }

        public Builder withHoverIconOutlinePos(int x, int y) {
            hoverOutlinePos = TexturePos.of(x, y);
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

        public Builder withScreen(ScreenWithSlots screen) {
            this.screen = screen;
            return this;
        }

        public Builder withDefaultGuiTexture(Identifier textureId) {
            this.defaultGuiTexture = textureId;
            return this;
        }

        public Builder withDimColorMultiplier(float dimColorMultiplier) {
            this.dimColorMultiplier = dimColorMultiplier;
            return this;
        }

        public EnchantmentSlotsRenderer build() {
            return new EnchantmentSlotsRenderer(
                outlinePos,
                slotTexturePos,
                powerfulOutlinePos,
                hoverOutlinePos,
                choicePosOffset,
                choiceTexturePos,
                iconOutlinePos,
                iconPowerfulOutlinePos,
                hoverIconOutlinePos,
                slotMap,
                choiceOffsets,
                screen,
                dimPredicate,
                defaultGuiTexture,
                dimColorMultiplier
            );
        }
    }
}
