package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.programmer.GuiProgWidgetForEach;
import me.desht.pneumaticcraft.common.ai.DroneAIManager;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.item.ItemPlastic;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProgWidgetForEachItem extends ProgWidget implements IJumpBackWidget, IJump, IVariableSetWidget {
    private String elementVariable = "";
    private int curIndex; //iterator index
    private DroneAIManager aiManager;

    @Override
    public String getWidgetString() {
        return "forEachItem";
    }

    @Override
    public int getCraftingColorIndex() {
        return ItemPlastic.YELLOW;
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_FOR_EACH_ITEM;
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters() {
        return new Class[]{ProgWidgetItemFilter.class, ProgWidgetString.class};
    }

    @Override
    public void addVariables(Set<String> variables) {
        variables.add(elementVariable);
    }

    @Override
    public String getVariable() {
        return elementVariable;
    }

    @Override
    public void setVariable(String variable) {
        elementVariable = variable;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("variable", elementVariable);
        super.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        elementVariable = tag.getString("variable");
        super.readFromNBT(tag);
    }

    @Override
    public WidgetDifficulty getDifficulty() {
        return WidgetDifficulty.ADVANCED;
    }

    @Override
    public IProgWidget getOutputWidget(IDroneBase drone, List<IProgWidget> allWidgets) {
        List<String> locations = getPossibleJumpLocations();
        ItemStack filter = getFilterForIndex(curIndex++);
        if (locations.size() > 0 && filter != null && (curIndex == 1 || !aiManager.getStack(elementVariable).isEmpty())) {
            aiManager.setItem(elementVariable, filter);
            return ProgWidgetJump.jumpToLabel(drone, allWidgets, locations.get(0));
        }
        curIndex = 0;
        return super.getOutputWidget(drone, allWidgets);
    }

    private ItemStack getFilterForIndex(int index) {
        ProgWidgetItemFilter widget = (ProgWidgetItemFilter) getConnectedParameters()[0];
        for (int i = 0; i < index; i++) {
            if (widget == null) return null;
            widget = (ProgWidgetItemFilter) widget.getConnectedParameters()[0];
        }
        return widget != null ? widget.getFilter() : null;
    }

    @Override
    public List<String> getPossibleJumpLocations() {
        IProgWidget widget = getConnectedParameters()[getParameters().length - 1];
        ProgWidgetString textWidget = widget != null ? (ProgWidgetString) widget : null;
        List<String> locations = new ArrayList<>();
        if (textWidget != null) locations.add(textWidget.string);
        return locations;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer) {
        return new GuiProgWidgetForEach(this, guiProgrammer);
    }

    @Override
    public String getExtraStringInfo() {
        return "\"" + elementVariable + "\"";
    }

    @Override
    public void setAIManager(DroneAIManager aiManager) {
        this.aiManager = aiManager;
    }

    @Override
    public boolean hasStepInput() {
        return true;
    }

    @Override
    public Class<? extends IProgWidget> returnType() {
        return null;
    }

    @Override
    protected boolean hasBlacklist() {
        return false;
    }
}
