package net.backupcup.mcd_enchantments.screen;

import org.lwjgl.opengl.GREMEDYStringMarker;

import com.mojang.blaze3d.systems.RenderSystem;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
import net.backupcup.mcd_enchantments.util.Slot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RunicTableScreen extends HandledScreen<RunicTableScreenHandler> {
    private Inventory inventory;

    private boolean[] slotOpened = new boolean[3];

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

        if (stack.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);
        EnchantmentSlots slots = EnchantmentSlots.fromNbt(stack.getNbt().getCompound("Slots"));

        for (Slot slot : Slot.values()) {
            if (slots.getSlot(slot).isEmpty()) continue;

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
        
        ItemStack itemStack = inventory.getStack(0);

        if (itemStack.isEmpty()) {
            drawMouseoverTooltip(matrices, mouseX, mouseY);
            return;
        }

        EnchantmentSlots slots = EnchantmentSlots.fromNbt(itemStack.getNbt().getCompound("Slots"));

        for (Slot slot : Slot.values()) {
            if (slots.getSlot(slot).isEmpty()) continue;

            drawTexture(matrices, slotOffsetsX[slot.ordinal()] + 1, posY + 38, 187, 105, 31, 31);
            if (isInSBounds(slotOffsetsX[slot.ordinal()], posY + 37, mouseX, mouseY))
                drawTexture(matrices, slotOffsetsX[slot.ordinal()], posY + 37, 220, 104, 33, 33);

            if (slotOpened[slot.ordinal()]) {
                RenderSystem.setShaderTexture(0, TEXTURE);
                drawTexture(matrices, posX + (slot.ordinal() * 35), posY, 186, 0, 67, 51);
                Identifier test = new Identifier("mcdw", "freezing");
                RenderSystem.setShaderTexture(0, EnchantmentTextureMapper.getTexture(test));
                EnchantmentTextureMapper.TexturePos pos = EnchantmentTextureMapper.getPos(test);
                drawTexture(matrices, posX + (slot.ordinal() * 35), posY, pos.x(), pos.y(), 23, 23);
                RenderSystem.setShaderTexture(0, TEXTURE);
            }
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