package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.desht.pneumaticcraft.common.PneumaticCraftAPIHandler;
import me.desht.pneumaticcraft.common.block.Blockss;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.fluid.Fluids;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileEntityKeroseneLamp extends TileEntityTickableBase implements IRedstoneControlled, ISerializableTanks, ISmartFluidSync {

    private static final List<String> REDSTONE_LABELS = ImmutableList.of(
            "gui.tab.redstoneBehaviour.button.anySignal",
            "gui.tab.redstoneBehaviour.button.highSignal",
            "gui.tab.redstoneBehaviour.button.lowSignal",
            "gui.tab.redstoneBehaviour.keroseneLamp.button.interpolate"
    );

    public static final int INVENTORY_SIZE = 2;

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private final Set<BlockPos> managingLights = new HashSet<>();
    @DescSynced
    private boolean isOn;
    @GuiSynced
    private int range;
    @GuiSynced
    private int targetRange = 10;
    @GuiSynced
    private int redstoneMode;
    @GuiSynced
    private int fuel;
    private static final int LIGHT_SPACING = 3;
    public static final int MAX_RANGE = 30;
    private int checkingX, checkingY, checkingZ;
    @DescSynced
    private EnumFacing sideConnected = EnumFacing.DOWN;
    @LazySynced
    @DescSynced
    @GuiSynced
    private final SmartSyncTank tank = new SmartSyncTank(this, 2000) {
        private FluidStack prevFluid;
        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            if (prevFluid == null && fluid != null || prevFluid != null && fluid == null) {
                recalculateFuelQuality();
            }
            prevFluid = fluid;
        }
    };
    @SuppressWarnings("unused")
    @DescSynced
    private int fluidAmountScaled;  // sync the lazy tank in a network-friendly way
    @DescSynced
    private float fuelQuality = -1f; // the quality of the liquid currently in the tank; basically, its burn time

    private final ItemStackHandler inventory = new BaseItemStackHandler(this, INVENTORY_SIZE) {
        @Override
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty() || FluidUtil.getFluidHandler(itemStack) != null;
        }
    };

    @Override
    public IItemHandlerModifiable getPrimaryInventory() {
        return inventory;
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote) {
            if (fuelQuality < 0) recalculateFuelQuality();
            processFluidItem(INPUT_SLOT, OUTPUT_SLOT);
            if (getWorld().getTotalWorldTime() % 5 == 0) {
                int realTargetRange = redstoneAllows() && fuel > 0 ? targetRange : 0;
                if (redstoneMode == 3) realTargetRange = (int) (poweredRedstone / 15D * targetRange);
                updateRange(Math.min(realTargetRange, tank.getFluidAmount())); //Fade out the lamp when almost empty.
                updateLights();
                useFuel();
            }
        } else {
            if (isOn && getWorld().getTotalWorldTime() % 5 == 0) {
                getWorld().spawnParticle(EnumParticleTypes.FLAME, getPos().getX() + 0.4 + 0.2 * getWorld().rand.nextDouble(), getPos().getY() + 0.2 + tank.getFluidAmount() / 1000D * 3 / 16D, getPos().getZ() + 0.4 + 0.2 * getWorld().rand.nextDouble(), 0, 0, 0);
            }
        }
    }

    private void recalculateFuelQuality() {
        if (tank.getFluid() != null && tank.getFluid().amount > 0) {
            if (ConfigHandler.machineProperties.keroseneLampCanUseAnyFuel) {
                Fluid f = tank.getFluid().getFluid();
                // 110 comes from kerosene's fuel value of 1,100,000 divided by the old FUEL_PER_MB value (10000)
                fuelQuality = PneumaticCraftAPIHandler.getInstance().liquidFuels.getOrDefault(f.getName(), 0) / 110f;
            } else {
                fuelQuality = Fluids.areFluidsEqual(tank.getFluid().getFluid(), Fluids.KEROSENE) ? 10000f : 0f;
            }
            fuelQuality *= ConfigHandler.machineProperties.keroseneLampFuelEfficiency;
        }
    }

    private void useFuel() {
        if (fuelQuality == 0) return; // tank is empty or a non-burnable liquid in the tank
        fuel -= range * range * range;
        while (fuel <= 0 && tank.drain(1, true) != null) {
            fuel += fuelQuality;
        }
        if (fuel < 0) fuel = 0;
    }

    @Override
    public void validate() {
        super.validate();
        checkingX = getPos().getX();
        checkingY = getPos().getY();
        checkingZ = getPos().getZ();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        for (BlockPos pos : managingLights) {
            if (isLampLight(pos)) {
                getWorld().setBlockToAir(pos);
            }
        }
    }

    private boolean isLampLight(BlockPos pos) {
        return getWorld().getBlockState(pos).getBlock() == Blockss.KEROSENE_LAMP_LIGHT;
    }

    private void updateLights() {
        int roundedRange = range / LIGHT_SPACING * LIGHT_SPACING;
        checkingX += LIGHT_SPACING;
        if (checkingX > getPos().getX() + roundedRange) {
            checkingX = getPos().getX() - roundedRange;
            checkingY += LIGHT_SPACING;
            if (checkingY > getPos().getY() + roundedRange) {
                checkingY = getPos().getY() - roundedRange;
                checkingZ += LIGHT_SPACING;
                if (checkingZ > getPos().getZ() + roundedRange) checkingZ = getPos().getZ() - roundedRange;
            }
        }
        BlockPos pos = new BlockPos(checkingX, checkingY, checkingZ);
        BlockPos lampPos = new BlockPos(getPos().getX(), getPos().getY(), getPos().getZ());
        if (managingLights.contains(pos)) {
            if (isLampLight(pos)) {
                if (!passesRaytraceTest(pos, lampPos)) {
                    getWorld().setBlockToAir(pos);
                    managingLights.remove(pos);
                }
            } else {
                managingLights.remove(pos);
            }
        } else {
            tryAddLight(pos, lampPos);
        }
    }

    private void updateRange(int targetRange) {
        if (targetRange > range) {
            range++;
            BlockPos lampPos = new BlockPos(getPos().getX(), getPos().getY(), getPos().getZ());
            int roundedRange = range / LIGHT_SPACING * LIGHT_SPACING;
            for (int x = -roundedRange; x <= roundedRange; x += LIGHT_SPACING) {
                for (int y = -roundedRange; y <= roundedRange; y += LIGHT_SPACING) {
                    for (int z = -roundedRange; z <= roundedRange; z += LIGHT_SPACING) {
                        BlockPos pos = new BlockPos(x + getPos().getX(), y + getPos().getY(), z + getPos().getZ());
                        if (!managingLights.contains(pos)) {
                            tryAddLight(pos, lampPos);
                        }
                    }
                }
            }
        } else if (targetRange < range) {
            range--;
            Iterator<BlockPos> iterator = managingLights.iterator();
            BlockPos lampPos = new BlockPos(getPos().getX(), getPos().getY(), getPos().getZ());
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                if (!isLampLight(pos)) {
                    iterator.remove();
                } else if (PneumaticCraftUtils.distBetween(pos, lampPos) > range) {
                    getWorld().setBlockToAir(pos);
                    iterator.remove();
                }
            }
        }
        boolean oldIsOn = isOn;
        isOn = range > 0;
        if (isOn != oldIsOn) {
            getWorld().checkLightFor(EnumSkyBlock.BLOCK, getPos());
            sendDescriptionPacket();
        }
    }

    public boolean isOn() {
        return isOn;
    }

    private boolean passesRaytraceTest(BlockPos pos, BlockPos lampPos) {
        RayTraceResult mop = getWorld().rayTraceBlocks(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), new Vec3d(lampPos.getX() + 0.5, lampPos.getY() + 0.5, lampPos.getZ() + 0.5));
        return mop != null && lampPos.equals(mop.getBlockPos());
    }

    private void tryAddLight(BlockPos pos, BlockPos lampPos) {
        if (PneumaticCraftUtils.distBetween(pos, lampPos) <= range) {
            if (getWorld().isAirBlock(pos) && !isLampLight(pos)) {
                if (passesRaytraceTest(pos, lampPos)) {
                    getWorld().setBlockState(pos, Blockss.KEROSENE_LAMP_LIGHT.getDefaultState());
                    managingLights.add(pos);
                }
            }
        }
    }

    @Override
    public void onNeighborBlockUpdate() {
        super.onNeighborBlockUpdate();
        EnumFacing oldSideConnected = sideConnected;
        sideConnected = EnumFacing.DOWN;
        for (EnumFacing d : EnumFacing.VALUES) {
            BlockPos neighborPos = getPos().offset(d);
            IBlockState state = getWorld().getBlockState(neighborPos);
            if (state.isSideSolid(getWorld(), neighborPos, d.getOpposite())) {
                sideConnected = d;
                break;
            }
        }
        if (sideConnected != oldSideConnected) {
            sendDescriptionPacket();
        }
    }

    @Override
    public void onDescUpdate() {
        getWorld().checkLightFor(EnumSkyBlock.BLOCK, getPos());
        getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList lights = new NBTTagList();
        for (BlockPos pos : managingLights) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("x", pos.getX());
            t.setInteger("y", pos.getY());
            t.setInteger("z", pos.getZ());
            lights.appendTag(t);
        }
        tag.setTag("lights", lights);

        NBTTagCompound tankTag = new NBTTagCompound();
        tank.writeToNBT(tankTag);
        tag.setTag("tank", tankTag);
        tag.setByte("redstoneMode", (byte) redstoneMode);
        tag.setByte("targetRange", (byte) targetRange);
        tag.setByte("range", (byte) range);
        tag.setByte("sideConnected", (byte) sideConnected.ordinal());
        tag.setTag("Items", inventory.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        managingLights.clear();
        NBTTagList lights = tag.getTagList("lights", 10);
        for (int i = 0; i < lights.tagCount(); i++) {
            NBTTagCompound t = lights.getCompoundTagAt(i);
            managingLights.add(new BlockPos(t.getInteger("x"), t.getInteger("y"), t.getInteger("z")));
        }
        tank.readFromNBT(tag.getCompoundTag("tank"));
        fluidAmountScaled = tank.getScaledFluidAmount();
        recalculateFuelQuality();
        redstoneMode = tag.getByte("redstoneMode");
        targetRange = tag.getByte("targetRange");
        range = tag.getByte("range");
        sideConnected = EnumFacing.byIndex(tag.getByte("sideConnected"));
        inventory.deserializeNBT(tag.getCompoundTag("Items"));
    }

    @Override
    public boolean redstoneAllows() {
        return redstoneMode == 3 || super.redstoneAllows();
    }

    @Override
    public int getRedstoneMode() {
        return redstoneMode;
    }

    @Override
    public void handleGUIButtonPress(int buttonID, EntityPlayer player) {
        if (buttonID == 0) {
            redstoneMode++;
            if (redstoneMode > 3) redstoneMode = 0;
        } else if (buttonID > 0 && buttonID <= MAX_RANGE) {
            targetRange = buttonID;
        }
    }

    public FluidTank getTank() {
        return tank;
    }

    public int getRange() {
        return range;
    }

    public int getTargetRange() {
        return targetRange;
    }

    public int getFuel() {
        return fuel;
    }

    public EnumFacing getSideConnected() {
        return sideConnected;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    /**
     * Returns the name of the inventory.
     */
    @Override
    public String getName() {
        return Blockss.KEROSENE_LAMP.getTranslationKey();
    }

    public float getFuelQuality() {
        return fuelQuality;
    }

    @Override
    protected List<String> getRedstoneButtonLabels() {
        return REDSTONE_LABELS;
    }

    @Nonnull
    @Override
    public Map<String, FluidTank> getSerializableTanks() {
        return ImmutableMap.of("Tank", tank);
    }

    @Override
    public void updateScaledFluidAmount(int tankIndex, int amount) {
        fluidAmountScaled = amount;
    }
}
