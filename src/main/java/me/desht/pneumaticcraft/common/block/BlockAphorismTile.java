package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.common.tileentity.TileEntityAphorismTile;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.lib.BBConstants;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.DyeUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.OptionalInt;

public class BlockAphorismTile extends BlockPneumaticCraft {

    private static final String NBT_BORDER_COLOR = "borderColor";
    private static final String NBT_BACKGROUND_COLOR = "backgroundColor";

    BlockAphorismTile() {
        super(Material.ROCK, "aphorism_tile");
        setHardness(1.5f);
        setResistance(4.0f);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        EnumFacing dir = state.getValue(ROTATION);
        return new AxisAlignedBB(
                dir.getXOffset() <= 0 ? 0 : 1F - BBConstants.APHORISM_TILE_THICKNESS,
                dir.getYOffset() <= 0 ? 0 : 1F - BBConstants.APHORISM_TILE_THICKNESS,
                dir.getZOffset() <= 0 ? 0 : 1F - BBConstants.APHORISM_TILE_THICKNESS,
                dir.getXOffset() >= 0 ? 1 : BBConstants.APHORISM_TILE_THICKNESS,
                dir.getYOffset() >= 0 ? 1 : BBConstants.APHORISM_TILE_THICKNESS,
                dir.getZOffset() >= 0 ? 1 : BBConstants.APHORISM_TILE_THICKNESS);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return getBoundingBox(blockState, worldIn, pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityAphorismTile.class;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> curInfo, ITooltipFlag flag) {
        super.addInformation(stack, world, curInfo, flag);
        if (NBTUtil.hasTag(stack, NBT_BORDER_COLOR) || NBTUtil.hasTag(stack, NBT_BACKGROUND_COLOR)) {
            curInfo.add(TextFormatting.DARK_GREEN.toString() + TextFormatting.ITALIC + I18n.format("gui.tab.info.tile.aphorism_tile.color"));
        }
    }

    /**
     * Called when the block is placed in the world.
     */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entityLiving, ItemStack iStack) {
        super.onBlockPlacedBy(world, pos, state, entityLiving, iStack);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityAphorismTile) {
            TileEntityAphorismTile teAT = (TileEntityAphorismTile) te;

            teAT.setBackgroundColor(getBackgroundColor(iStack));
            teAT.setBorderColor(getBorderColor(iStack));

            EnumFacing rotation = getRotation(world, pos);
            if (rotation.getAxis() == Axis.Y) {
                float yaw = entityLiving.rotationYaw;
                if (yaw < 0) yaw += 360;
                teAT.textRotation = (((int) yaw + 45) / 90 + 2) % 4;
                if (rotation.getYOffset() > 0 && (teAT.textRotation == 1 || teAT.textRotation == 3)) {
                    // fudge - reverse rotation if placing above, and player is facing on east/west axis
                    teAT.textRotation = 4 - teAT.textRotation;
                }
            }

            if (world.isRemote && entityLiving instanceof EntityPlayer) {
                ((EntityPlayer) entityLiving).openGui(PneumaticCraftRepressurized.instance, EnumGuiId.APHORISM_TILE.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
                sendEditorMessage((EntityPlayer) entityLiving);
            }
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        super.getDrops(drops, world, pos, state, fortune);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityAphorismTile && drops.size() > 0) {
            TileEntityAphorismTile teAT = (TileEntityAphorismTile) te;
            ItemStack teStack = drops.get(0);
            int bgColor = teAT.getBackgroundColor();
            int borderColor = teAT.getBorderColor();
            if (bgColor != EnumDyeColor.WHITE.getDyeDamage() || borderColor != EnumDyeColor.BLUE.getDyeDamage()) {
                NBTUtil.setInteger(teStack, NBT_BACKGROUND_COLOR, bgColor);
                NBTUtil.setInteger(teStack, NBT_BORDER_COLOR, borderColor);
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote && hand != EnumHand.OFF_HAND && player.getHeldItem(hand).isEmpty() && !player.isSneaking()) {
            player.openGui(PneumaticCraftRepressurized.instance, EnumGuiId.APHORISM_TILE.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
            sendEditorMessage(player);
        } else if (!world.isRemote && DyeUtils.isDye(player.getHeldItem(hand))) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityAphorismTile) {
                TileEntityAphorismTile teAT = (TileEntityAphorismTile) te;
                OptionalInt color = DyeUtils.dyeDamageFromStack(player.getHeldItem(hand));
                if (color.isPresent()) {
                    if (clickedBorder(state, hitX, hitY, hitZ)) {
                        if (teAT.getBorderColor() != color.getAsInt()) {
                            teAT.setBorderColor(color.getAsInt());
                            player.getHeldItem(hand).shrink(1);
                        }
                    } else {
                        if (teAT.getBackgroundColor() != color.getAsInt()) {
                            teAT.setBackgroundColor(color.getAsInt());
                            player.getHeldItem(hand).shrink(1);
                        }
                    }

                }
            }
        }
        return true;
    }

    private boolean clickedBorder(IBlockState state, float hitX, float hitY, float hitZ) {
        switch (getRotation(state)) {
            case EAST: case WEST: return hitY < 0.1 || hitY > 0.9 || hitZ < 0.1 || hitZ > 0.9;
            case NORTH: case SOUTH: return hitY < 0.1 || hitY > 0.9 || hitX < 0.1 || hitX > 0.9;
            case UP: case DOWN: return hitX < 0.1 || hitX > 0.9 || hitZ < 0.1 || hitZ > 0.9;
        }
        return false;
    }

    private void sendEditorMessage(EntityPlayer player) {
        ITextComponent msg = new TextComponentString(TextFormatting.WHITE.toString())
                .appendSibling(new TextComponentTranslation("gui.aphorismTileEditor"))
                .appendSibling(new TextComponentString(": "))
                .appendSibling(new TextComponentTranslation("gui.holdF1forHelp"));
        player.sendStatusMessage(msg, true);
    }

    @Override
    public boolean isRotatable() {
        return true;
    }

    @Override
    protected boolean canRotateToTopOrBottom() {
        return true;
    }

    @Override
    public boolean rotateBlock(World world, EntityPlayer player, BlockPos pos, EnumFacing face, EnumHand hand) {
        if (player != null && player.isSneaking()) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityAphorismTile) {
                TileEntityAphorismTile teAt = (TileEntityAphorismTile) tile;
                if (++teAt.textRotation > 3) teAt.textRotation = 0;
                teAt.sendDescriptionPacket();
                return true;
            } else {
                return false;
            }
        } else {
            return super.rotateBlock(world, player, pos, face, hand);
        }
    }

    @Override
    protected boolean rotateForgeWay() {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    public static int getBackgroundColor(ItemStack stack) {
        return NBTUtil.hasTag(stack, NBT_BACKGROUND_COLOR) ? NBTUtil.getInteger(stack, NBT_BACKGROUND_COLOR) : EnumDyeColor.WHITE.getDyeDamage();
    }

    public static int getBorderColor(ItemStack stack) {
        return NBTUtil.hasTag(stack, NBT_BORDER_COLOR) ? NBTUtil.getInteger(stack, NBT_BORDER_COLOR) : EnumDyeColor.BLUE.getDyeDamage();
    }
}
