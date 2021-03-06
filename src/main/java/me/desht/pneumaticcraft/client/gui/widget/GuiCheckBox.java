package me.desht.pneumaticcraft.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiCheckBox extends Gui implements IGuiWidget {
    public boolean checked, enabled = true, visible = true;
    public int x, y, color;
    private final int id;
    public String text;
    private List<String> tooltip = new ArrayList<>();
    private IWidgetListener listener;

    private static final int CHECKBOX_WIDTH = 10;
    private static final int CHECKBOX_HEIGHT = 10;

    public GuiCheckBox(int id, int x, int y, int color, String text) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.color = color;
        this.text = text;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        if (visible) {
            drawRect(x, y, x + CHECKBOX_WIDTH, y + CHECKBOX_HEIGHT, enabled ? 0xFFA0A0A0 : 0xFF999999);
            drawRect(x + 1, y + 1, x + CHECKBOX_WIDTH - 1, y + CHECKBOX_HEIGHT - 1, enabled ? 0xFF202020 : 0xFFAAAAAA);
            if (checked) {
                GlStateManager.disableTexture2D();
                if (enabled) {
                    GlStateManager.color(0.5f, 1, 0.5f, 1);
                } else {
                    GlStateManager.color(0.8f, 0.8f, 0.8f, 1);
                }
                BufferBuilder wr = Tessellator.getInstance().getBuffer();
                GlStateManager.glLineWidth(2);
                wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
                wr.pos(x + 2, y + 5, zLevel).endVertex();
                wr.pos(x + 5, y + 7, zLevel).endVertex();
                wr.pos(x + 8, y + 3, zLevel).endVertex();
                Tessellator.getInstance().draw();
                GlStateManager.enableTexture2D();
                GlStateManager.color(0.25f, 0.25f, 0.25f, 1);
            }
            Minecraft.getMinecraft().fontRenderer.drawString(I18n.format(text), x + 3 + CHECKBOX_WIDTH, y + CHECKBOX_HEIGHT / 2 - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 2, enabled ? color : 0xFF888888);
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, CHECKBOX_WIDTH + Minecraft.getMinecraft().fontRenderer.getStringWidth(I18n.format(text)), CHECKBOX_HEIGHT);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        if (enabled) {
            checked = !checked;
            if (listener != null) listener.actionPerformed(this);
        }
    }

    @Override
    public void onMouseClickedOutsideBounds(int mouseX, int mouseY, int button) {

    }

    public GuiCheckBox setTooltip(String tooltip) {
        this.tooltip.clear();
        if (tooltip != null && !tooltip.equals("")) {
            this.tooltip.add(tooltip);
        }
        return this;
    }

    public GuiCheckBox setTooltip(List<String> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @Override
    public void addTooltip(int mouseX, int mouseY, List<String> curTooltip, boolean shiftPressed) {
        if (visible) curTooltip.addAll(tooltip);
    }

    public String getTooltip() {
        return tooltip.size() > 0 ? tooltip.get(0) : "";
    }

    @Override
    public boolean onKey(char key, int keyCode) {
        return false;
    }

    @Override
    public void setListener(IWidgetListener gui) {
        listener = gui;
    }

    public GuiCheckBox setChecked(boolean checked) {
        this.checked = checked;
        return this;
    }

    @Override
    public void update() {
    }

    @Override
    public void handleMouseInput() {
    }

    @Override
    public void postRender(int mouseX, int mouseY, float partialTick) {

    }
}
