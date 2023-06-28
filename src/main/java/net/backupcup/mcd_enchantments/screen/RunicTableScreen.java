package net.backupcup.mcd_enchantments.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.backupcup.mcd_enchantments.MCDEnchantments;
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

    private static final Identifier TEXTURE =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/runic_table.png");
    private static final Identifier ENCHANTMENT_ICONS_DUNGEONS =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/enchantment_icons_dungeons.png");
    private static final Identifier ENCHANTMENT_ICONS_VANILLA =
            new Identifier(MCDEnchantments.MOD_ID, "textures/gui/enchantment_icons_vanilla.png");

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

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        ItemStack stack = inventory.getStack(0);

        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
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