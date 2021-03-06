package me.desht.pneumaticcraft.common.network;

import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PacketCoordTrackUpdate extends LocationIntPacket<PacketCoordTrackUpdate> {

    private int dimensionID;

    public PacketCoordTrackUpdate() {
    }

    public PacketCoordTrackUpdate(World world, BlockPos pos) {
        super(pos);
        dimensionID = world.provider.getDimension();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeInt(dimensionID);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        dimensionID = buffer.readInt();
    }

    @Override
    public void handleClientSide(PacketCoordTrackUpdate message, EntityPlayer player) {
    }

    @Override
    public void handleServerSide(PacketCoordTrackUpdate message, EntityPlayer player) {
        ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!stack.isEmpty()) {
            ItemPneumaticArmor.setCoordTrackerPos(stack, message.dimensionID, message.pos);
        }
    }

}
