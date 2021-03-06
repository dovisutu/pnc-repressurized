package me.desht.pneumaticcraft.common.remote;

import me.desht.pneumaticcraft.client.gui.GuiRemoteEditor;
import me.desht.pneumaticcraft.client.gui.remote.GuiRemoteDropdown;
import me.desht.pneumaticcraft.client.gui.widget.WidgetComboBox;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSetGlobalVariable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.text.WordUtils;

public class ActionWidgetDropdown extends ActionWidgetVariable<WidgetComboBox> {

    private int x, y, width, height;
    private String dropDownElements = "";
    private String selectedElement = "";
    private boolean sorted;

    public ActionWidgetDropdown() {
        super();
    }

    public ActionWidgetDropdown(WidgetComboBox widget) {
        super(widget);
        width = widget.width;
        height = widget.height;
        widget.setText(I18n.format("remote.dropdown.name"));
        widget.setTooltip(WordUtils.wrap(I18n.format("remote.dropdown.tooltip"), 50).split(System.getProperty("line.separator")));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag, int guiLeft, int guiTop) {
        super.readFromNBT(tag, guiLeft, guiTop);
        x = tag.getInteger("x") + guiLeft;
        y = tag.getInteger("y") + guiTop;
        width = tag.getInteger("width");
        height = tag.getInteger("height");
        dropDownElements = tag.getString("dropDownElements");
        sorted = tag.getBoolean("sorted");
        updateWidget();
    }

    @Override
    public NBTTagCompound toNBT(int guiLeft, int guiTop) {
        NBTTagCompound tag = super.toNBT(guiLeft, guiTop);
        tag.setInteger("x", x - guiLeft);
        tag.setInteger("y", y - guiTop);
        tag.setInteger("width", width);
        tag.setInteger("height", height);
        tag.setString("dropDownElements", dropDownElements);
        tag.setBoolean("sorted", sorted);

        return tag;
    }

    @Override
    public String getId() {
        return "dropdown";
    }

    @Override
    public void onKeyTyped() {
        String[] elements = getDropdownElements();
        selectedElement = getWidget().getText();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].equals(selectedElement)) {
                NetworkHandler.sendToServer(new PacketSetGlobalVariable(getVariableName(), i));
                break;
            }
        }
    }

    @Override
    public void onVariableChange() {
        updateWidget();
    }

    @Override
    public void setWidgetPos(int x, int y) {
        this.x = x;
        this.y = y;
        updateWidget();
    }

    @Override
    public WidgetComboBox getWidget() {
        if (widget == null) {
            widget = new WidgetComboBox(Minecraft.getMinecraft().fontRenderer, x, y, width, height);
            widget.setElements(getDropdownElements());
            widget.setFixedOptions();
            widget.setShouldSort(sorted);
            updateWidget();
        }
        return widget;
    }

    private String[] getDropdownElements() {
        return dropDownElements.split(",");
    }

    private void updateWidget() {
        String[] elements = getDropdownElements();
        selectedElement = elements[MathHelper.clamp(GlobalVariableManager.getInstance().getInteger(getVariableName()), 0, elements.length - 1)];

        if (widget != null) {
            widget.x = x;
            widget.y = y;
            widget.width = width;
            widget.height = height;
            widget.setElements(getDropdownElements());
            widget.setText(selectedElement);
            widget.setShouldSort(sorted);
        }
    }

    @Override
    public void onActionPerformed() {
    }

    public void setDropDownElements(String dropDownElements) {
        this.dropDownElements = dropDownElements;
        updateWidget();
    }

    public String getDropDownElements() {
        return dropDownElements;
    }

    public boolean getSorted() {
        return sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    public void setWidth(int width) {
        this.width = width;
        updateWidget();
    }

    public int getWidth() {
        return width;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getGui(GuiRemoteEditor guiRemote) {
        return new GuiRemoteDropdown(this, guiRemote);
    }
}
