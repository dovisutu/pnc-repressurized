package me.desht.pneumaticcraft.client.gui.pneumatic_armor;

import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IOptionPage;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.client.gui.widget.GuiKeybindCheckBox;
import me.desht.pneumaticcraft.client.gui.widget.IGuiWidget;
import me.desht.pneumaticcraft.client.gui.widget.IWidgetListener;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.common.config.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdateArmorExtraData;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;

public class GuiJetBootsOptions extends IOptionPage.SimpleToggleableOptions implements IWidgetListener {

    private GuiKeybindCheckBox checkBox;

    public GuiJetBootsOptions(IUpgradeRenderHandler handler) {
        super(handler);
    }

    @Override
    public void initGui(IGuiScreen gui) {
        super.initGui(gui);

        checkBox = new GuiKeybindCheckBox(0, 5, 45, 0xFFFFFFFF, "jetboots.module.builderMode");
        ((GuiHelmetMainScreen) gui).addWidget(checkBox);
        checkBox.setListener(this);

        gui.getButtonList().add(new GuiButton(10, 30, 128, 150, 20, "Move Stat Screen..."));
    }

    @Override
    public void updateScreen() {
        CommonArmorHandler handler = CommonArmorHandler.getHandlerForPlayer();
        checkBox.enabled = handler.getUpgradeCount(EntityEquipmentSlot.FEET, IItemRegistry.EnumUpgrade.JET_BOOTS) >= 8;
    }

    @Override
    public void actionPerformed(IGuiWidget widget) {
        if (widget == GuiKeybindCheckBox.fromKeyBindingName("jetboots.module.builderMode")) {
            CommonArmorHandler commonArmorHandler = CommonArmorHandler.getHandlerForPlayer();
            if (commonArmorHandler.getUpgradeCount(EntityEquipmentSlot.FEET, IItemRegistry.EnumUpgrade.JET_BOOTS) >= 8) {
                boolean checked = ((GuiKeybindCheckBox) widget).checked;
                NBTTagCompound tag = new NBTTagCompound();
                tag.setBoolean(ItemPneumaticArmor.NBT_BUILDER_MODE, checked);
                NetworkHandler.sendToServer(new PacketUpdateArmorExtraData(EntityEquipmentSlot.FEET, tag));
                CommonArmorHandler.getHandlerForPlayer().onDataFieldUpdated(EntityEquipmentSlot.FEET, ItemPneumaticArmor.NBT_BUILDER_MODE, tag.getTag(ItemPneumaticArmor.NBT_BUILDER_MODE));
                HUDHandler.instance().addFeatureToggleMessage(getRenderHandler(), checkBox.text, checked);
            }
        }
    }

    @Override
    public void onKeyTyped(IGuiWidget widget) {
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id == 10) {
            Minecraft.getMinecraft().player.closeScreen();
            Minecraft.getMinecraft().displayGuiScreen(new GuiMoveStat(getRenderHandler(), ArmorHUDLayout.LayoutTypes.JET_BOOTS));
        }
    }
}
