package net.backupcup.mcde.screen.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.util.Choice;
import net.backupcup.mcde.util.EnchantmentSlot;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.SlotPosition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class EnchantmentSlotsRenderer {
    private static final Identifier missingEnchantTexture = MCDE.id("textures/gui/icons/missing_no.png");

    private TexturePos slotTexturePos;
    private TexturePos outlinePos;

    private TexturePos powerfulOutlinePos;
    private TexturePos choicePosOffset;
    private TexturePos choiceTexturePos;
    private TexturePos hoverOutlinePos;
    private TexturePos iconOutlinePos;
    private TexturePos iconPowerfulOutlinePos;
    private TexturePos hoverIconOutlinePos;
    private Map<SlotPosition, TexturePos> slotPos;
    private Map<SlotPosition, TexturePos> choiceOffsets;
    private ScreenWithSlots screen;
    private Predicate<Choice> dimPredicate;
    private Identifier defaultGuiTexture;
    private float dimColorMultiplier;
    private ResourceManager resourceManager;

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
            Map<SlotPosition, TexturePos> slotPos,
            Map<SlotPosition, TexturePos> choiceOffsets,
            ScreenWithSlots screen,
            Predicate<Choice> dimPredicate,
            Identifier defaultGuiTexture,
            float dimColorMultiplier,
            ResourceManager resourceManager
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
        this.resourceManager = resourceManager;
    }

    public void drawSlot(DrawContext ctx, SlotPosition slot) {
        var pos = slotPos.get(slot);
        ctx.drawTexture(defaultGuiTexture, pos.x(), pos.y(), slotTexturePos.x(), slotTexturePos.y(), 33, 39);
    }

    public void drawChoices(DrawContext ctx, SlotPosition slot) {
        var pos = slotPos.get(slot).add(choicePosOffset);
        ctx.drawTexture(defaultGuiTexture, pos.x(), pos.y(), choiceTexturePos.x(), choiceTexturePos.y(), 69, 56);
    }

    public void drawHoverOutline(DrawContext ctx, SlotPosition slot) {
        var pos = slotPos.get(slot);
        RenderSystem.setShaderTexture(0, defaultGuiTexture);
        ctx.drawTexture(defaultGuiTexture, pos.x() - 1, pos.y() - 1, hoverOutlinePos.x(), hoverOutlinePos.y(), 35, 41);
    }

    public void drawIconInSlot(DrawContext ctx, SlotPosition slot, Choice choice) {
        var texPos = MCDE.getConfig().isEnchantmentPowerful(choice.getEnchantmentId()) ?
            powerfulOutlinePos : outlinePos;
        var pos = slotPos.get(slot);
        ctx.drawTexture(defaultGuiTexture, pos.x(), pos.y(), texPos.x(), texPos.y(), 33, 39);
        drawIcon(ctx, pos.add(5, 6), slot, choice);
    }

    public void drawIconHoverOutline(DrawContext ctx, SlotPosition slot, Choice choice) {
        var drawPos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice.getChoicePosition()));
        ctx.drawTexture(defaultGuiTexture, drawPos.x() - 1, drawPos.y() - 1, hoverIconOutlinePos.x(), hoverIconOutlinePos.y(), 31, 33);
    }

    public void drawIconOutline(DrawContext ctx, SlotPosition slot, Choice choice) {
        var drawPos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice.getChoicePosition()));
        var texPos = MCDE.getConfig().isEnchantmentPowerful(choice.getEnchantmentId()) ?
            iconPowerfulOutlinePos : iconOutlinePos;
        ctx.drawTexture(defaultGuiTexture, drawPos.x(), drawPos.y(), texPos.x(), texPos.y(), 29, 31);
    }

    public void drawIconInChoice(DrawContext ctx, SlotPosition slot, Choice choice) {
        var drawPos = slotPos.get(slot)
            .add(choicePosOffset)
            .add(choiceOffsets.get(choice.getChoicePosition()))
            .add(3, 3);
        drawIcon(ctx, drawPos, slot, choice);
    }

    public Predicate<Choice> getDimPredicate() {
        return dimPredicate;
    }

    public Optional<Choice> render(DrawContext ctx, ItemStack itemStack, int mouseX, int mouseY) {
        Optional<Choice> hovered = Optional.empty();
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (slotsOptional.isEmpty()) {
            return hovered;
        }
        var slots = slotsOptional.get();
        for (var slot : slots) {
            drawSlot(ctx, slot.getSlotPosition());
            if (isInSlotBounds(slot.getSlotPosition(), mouseX, mouseY)) {
                drawHoverOutline(ctx, slot.getSlotPosition());
            }

            if (slot.getChosen().isPresent()) {
                var chosen = slot.getChosen().get();
                drawIconInSlot(ctx, slot.getSlotPosition(), chosen);
                if (isInSlotBounds(slot.getSlotPosition(), mouseX, mouseY)) {
                    drawHoverOutline(ctx, slot.getSlotPosition());
                    hovered = Optional.of(chosen);
                }
                continue;
            }

            if (screen.getOpened().isPresent() && screen.getOpened().get() == slot.getSlotPosition()) {
                drawChoices(ctx, slot.getSlotPosition());

                for (var choice : slot.choices()) {
                    if (isInChoiceBounds(slot.getSlotPosition(), choice.getChoicePosition(), mouseX, mouseY)) {
                        drawIconHoverOutline(ctx, slot.getSlotPosition(), choice);
                        hovered = Optional.of(choice);
                    }
                    drawIconOutline(ctx, slot.getSlotPosition(), choice);
                    drawIconInChoice(ctx, slot.getSlotPosition(), choice);
                }
            }
        }
        return hovered;
    }

    public boolean isInSlotBounds(SlotPosition slot, int mouseX, int mouseY) {
        var pos = slotPos.get(slot);
        return IntStream.rangeClosed(0, 13).anyMatch(i -> isInBounds(pos.x(), pos.y(), mouseX, mouseY, 14 - i, 19 + i, i, i)) ||
            isInBounds(pos.x(), pos.y(), mouseX, mouseY, 0, 31, 14, 25) ||
            IntStream.rangeClosed(0, 13).anyMatch(i -> isInBounds(pos.x(), pos.y(), mouseX, mouseY, i, 31 - i, i + 26, i + 26));
    }

    public boolean isInChoiceBounds(SlotPosition slot, SlotPosition choice, int mouseX, int mouseY) {
        var pos = slotPos.get(slot).add(choicePosOffset).add(choiceOffsets.get(choice));
        return IntStream.rangeClosed(0, 15).anyMatch(i -> isInBounds(pos.x(), pos.y(), mouseX, mouseY, 15 - i, 15 + i, i, i)) ||
            isInBounds(pos.x(), pos.y(), mouseX, mouseY, 0, 25, 15, 15) ||
            IntStream.rangeClosed(0, 15).anyMatch(i -> isInBounds(pos.x(), pos.y(), mouseX, mouseY, i + 1, 25 - i, i + 16, i + 16));
    }

    public boolean isInChoicesBounds(SlotPosition slot, int mouseX, int mouseY) {
        var pos = slotPos.get(slot).add(choicePosOffset);
        return IntStream.rangeClosed(0, 32).anyMatch(i -> isInBounds(pos.x(), pos.y(), mouseX, mouseY, 32 - i, 35 + i, i, i)) ||
            isInBounds(pos.x(), pos.y(), mouseX, mouseY, 0, 66, 33, 33) ||
            IntStream.rangeClosed(0, 16).anyMatch(i -> isInBounds(pos.x(), pos.y(), mouseX, mouseY, i, 66 - i, i + 34, i + 34));
    }

    private static boolean isInBounds(int posX, int posY, int mouseX, int mouseY, int startX, int endX, int startY, int endY) {
        return mouseX >= posX + startX &&
               mouseX <= posX + endX &&
               mouseY >= posY + startY &&
               mouseY <= posY + endY;
    }

    private void drawIcon(DrawContext ctx, TexturePos drawPos, SlotPosition slot, Choice choice) {
        Identifier enchantmentID = choice.getEnchantmentId();
        Identifier textureID = getTextureId(enchantmentID);

        RenderSystem.setShaderTexture(0, resourceManager.getResource(textureID).isPresent() ?
                textureID : missingEnchantTexture);
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
            MCDE.MOD_ID,
            String.format(
                "textures/gui/icons/%s/%s.png",
                enchantmentID.getNamespace(),
                enchantmentID.getPath()
            )
        );
    }

    public static Optional<Identifier> getLevelTexture(int level) {
        return Optional.of(level)
            .filter(lvl -> lvl >= 2)
            .map(lvl -> Math.min(lvl, 7))
            .map(lvl -> Identifier.of(
                MCDE.MOD_ID,
                String.format("textures/gui/slot_levels/%d.png", lvl)
            ));
    }

    public static class Builder {
        private Map<SlotPosition, TexturePos> slotMap;
        private Map<SlotPosition, TexturePos> choiceOffsets = Map.of(
                SlotPosition.FIRST, TexturePos.of(3, 19),
                SlotPosition.SECOND, TexturePos.of(36, 19),
                SlotPosition.THIRD, TexturePos.of(20, 3));
        private TexturePos slotTexturePos = TexturePos.of(186, 215);
        private TexturePos outlinePos = TexturePos.of(187, 102);
        private TexturePos hoverOutlinePos = TexturePos.of(220, 214);
        private TexturePos powerfulOutlinePos = TexturePos.of(221, 102);
        private TexturePos iconOutlinePos = TexturePos.of(189, 142);
        private TexturePos iconPowerfulOutlinePos = TexturePos.of(223, 142);
        private TexturePos hoverIconOutlinePos = TexturePos.of(222, 174);
        private TexturePos choicePosOffset = TexturePos.of(-18, -39);
        private TexturePos choiceTexturePos = TexturePos.of(186, 45);
        private ScreenWithSlots screen;
        private Predicate<Choice> dimPredicate;
        private Identifier defaultGuiTexture;
        private float dimColorMultiplier = 0.5f;
        private ResourceManager resourceManager;

        public Builder withSlotPositions(TexturePos first, TexturePos second, TexturePos third) {
            slotMap = Map.of(SlotPosition.FIRST, first, SlotPosition.SECOND, second, SlotPosition.THIRD, third);
            return this;
        }

        public Builder withSlotPositions(Map<SlotPosition, TexturePos> slotMap) {
            this.slotMap = slotMap;
            return this;
        }

        public Builder withDefaultSlotPositions(TexturePos backroundPos) {
            return withDefaultSlotPositions(backroundPos.x(), backroundPos.y());
        }

        public Builder withDefaultSlotPositions(int backgroundPosX, int backgroundPosY) {
            slotMap = Arrays.stream(SlotPosition.values())
                .collect(Collectors.toMap(
                            Function.identity(),
                            s -> TexturePos.of(
                                backgroundPosX + 18 + 37 * s.ordinal(),
                                backgroundPosY + 3
                                )));
            return this;
        }

        public Builder withChoiceOffsets(TexturePos first, TexturePos second, TexturePos third) {
            choiceOffsets = Map.of(SlotPosition.FIRST, first, SlotPosition.SECOND, second, SlotPosition.THIRD, third);
            return this;
        }

        public Builder withChoiceOffsets(Map<SlotPosition, TexturePos> choiceMap) {
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

        public Builder withClient(MinecraftClient client) {
            this.resourceManager = client.getResourceManager();
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
                dimColorMultiplier,
                resourceManager
            );
        }
    }
}
