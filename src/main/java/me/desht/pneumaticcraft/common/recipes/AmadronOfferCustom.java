package me.desht.pneumaticcraft.common.recipes;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.DroneRegistry;
import me.desht.pneumaticcraft.common.config.AmadronOfferSettings;
import me.desht.pneumaticcraft.common.inventory.ContainerAmadron;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketAmadronTradeNotifyDeal;
import me.desht.pneumaticcraft.common.network.PacketSyncAmadronOffers;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

public class AmadronOfferCustom extends AmadronOffer {
    private final String offeringPlayerName;
    private String offeringPlayerId;
    private int providingDimensionId, returningDimensionId;
    private BlockPos providingPosition, returningPosition;
    private int inStock;
    private int maxTrades = -1;
    private int pendingPayments;
    private TileEntity cachedInput, cachedOutput;

    public AmadronOfferCustom(Object input, Object output, EntityPlayer offeringPlayer) {
        this(input, output, offeringPlayer.getGameProfile().getName(), offeringPlayer.getGameProfile().getId().toString());
    }

    public AmadronOfferCustom(Object input, Object output, String playerName, String playerId) {
        super(input, output);
        offeringPlayerName = playerName;
        offeringPlayerId = playerId;
    }

    public AmadronOfferCustom setProvidingPosition(BlockPos pos, int dimensionId) {
        providingPosition = pos;
        providingDimensionId = dimensionId;
        cachedInput = null;
        return this;
    }

    public AmadronOfferCustom setReturningPosition(BlockPos pos, int dimensionId) {
        returningPosition = pos;
        returningDimensionId = dimensionId;
        cachedOutput = null;
        return this;
    }

    public AmadronOfferCustom invert() {
        Object temp = input;
        input = output;
        output = temp;
        return this;
    }

    public AmadronOfferCustom copy() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return loadFromNBT(tag);
    }

    public void updatePlayerId() {
        EntityPlayer player = PneumaticCraftUtils.getPlayerFromName(offeringPlayerName);
        if (player != null) offeringPlayerId = player.getGameProfile().getId().toString();
    }

    public void addStock(int stock) {
        inStock += stock;
    }

    @Override
    public int getStock() {
        return inStock;
    }

    public void addPayment(int payment) {
        pendingPayments += payment;
    }

    public void setMaxTrades(int maxTrades) {
        this.maxTrades = maxTrades;
    }

    @Override
    public String getVendor() {
        return offeringPlayerName;
    }

    public String getPlayerId() {
        return offeringPlayerId;
    }

    @Override
    public void onTrade(int tradingAmount, String buyingPlayer) {
        EntityPlayer player = PneumaticCraftUtils.getPlayerFromId(offeringPlayerId);
        if (player != null && AmadronOfferSettings.notifyOfDealMade) {
            NetworkHandler.sendTo(new PacketAmadronTradeNotifyDeal(this, tradingAmount, buyingPlayer), (EntityPlayerMP) player);
        }
    }

    void payout() {
        TileEntity returning = getReturningTileEntity();
        TileEntity provider = getProvidingTileEntity();
        if (pendingPayments > 0) {
            int paying = Math.min(pendingPayments, 50);
            paying = ContainerAmadron.capShoppingAmount(this, paying,
                    AmadronOfferManager.getItemHandler(provider), AmadronOfferManager.getItemHandler(returning),
                    AmadronOfferManager.getFluidHandler(provider), AmadronOfferManager.getFluidHandler(returning),
                    null);
            if (paying > 0) {
                pendingPayments -= paying;
                if (getInput() instanceof ItemStack) {
                    ItemStack deliveringItems = (ItemStack) getInput();
                    int amount = deliveringItems.getCount() * paying;
                    List<ItemStack> stacks = new ArrayList<>();
                    while (amount > 0) {
                        ItemStack stack = deliveringItems.copy();
                        stack.setCount(Math.min(amount, stack.getMaxStackSize()));
                        stacks.add(stack);
                        amount -= stack.getCount();
                    }
                    DroneRegistry.getInstance().deliverItemsAmazonStyle(returning.getWorld(), returning.getPos(), stacks.toArray(new ItemStack[0]));
                } else {
                    FluidStack deliveringFluid = ((FluidStack) getInput()).copy();
                    deliveringFluid.amount *= paying;
                    DroneRegistry.getInstance().deliverFluidAmazonStyle(returning.getWorld(), returning.getPos(), deliveringFluid);
                }
            }
        }
    }

    public void returnStock() {
        TileEntity provider = getProvidingTileEntity();
        TileEntity returning = getReturningTileEntity();
        invert();
        while (inStock > 0) {
            int stock = Math.min(inStock, 50);
            stock = ContainerAmadron.capShoppingAmount(this, stock,
                    AmadronOfferManager.getItemHandler(returning), AmadronOfferManager.getItemHandler(provider),
                    AmadronOfferManager.getFluidHandler(returning), AmadronOfferManager.getFluidHandler(provider),
                    null);
            if (stock > 0) {
                inStock -= stock;
                if (getInput() instanceof ItemStack) {
                    ItemStack deliveringItems = (ItemStack) getInput();
                    int amount = deliveringItems.getCount() * stock;
                    List<ItemStack> stacks = new ArrayList<>();
                    while (amount > 0) {
                        ItemStack stack = deliveringItems.copy();
                        stack.setCount(Math.min(amount, stack.getMaxStackSize()));
                        stacks.add(stack);
                        amount -= stack.getCount();
                    }
                    DroneRegistry.getInstance().deliverItemsAmazonStyle(provider.getWorld(), provider.getPos(), stacks.toArray(new ItemStack[0]));
                } else {
                    FluidStack deliveringFluid = ((FluidStack) getInput()).copy();
                    deliveringFluid.amount *= stock;
                    DroneRegistry.getInstance().deliverFluidAmazonStyle(provider.getWorld(), provider.getPos(), deliveringFluid);
                }
            } else {
                break;
            }
        }
    }

    public TileEntity getProvidingTileEntity() {
        if (cachedInput == null || cachedInput.isInvalid()) {
            if (providingPosition != null) {
                cachedInput = PneumaticCraftUtils.getTileEntity(providingPosition, providingDimensionId);
            }
        }
        return cachedInput;
    }

    public TileEntity getReturningTileEntity() {
        if (cachedOutput == null || cachedOutput.isInvalid()) {
            if (returningPosition != null) {
                cachedOutput = PneumaticCraftUtils.getTileEntity(returningPosition, returningDimensionId);
            }
        }
        return cachedOutput;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("offeringPlayerId", offeringPlayerId);
        tag.setString("offeringPlayerName", offeringPlayerName);
        tag.setInteger("inStock", inStock);
        tag.setInteger("maxTrades", maxTrades);
        tag.setInteger("pendingPayments", pendingPayments);
        if (providingPosition != null) {
            tag.setInteger("providingDimensionId", providingDimensionId);
            tag.setInteger("providingX", providingPosition.getX());
            tag.setInteger("providingY", providingPosition.getY());
            tag.setInteger("providingZ", providingPosition.getZ());
        }
        if (returningPosition != null) {
            tag.setInteger("returningDimensionId", returningDimensionId);
            tag.setInteger("returningX", returningPosition.getX());
            tag.setInteger("returningY", returningPosition.getY());
            tag.setInteger("returningZ", returningPosition.getZ());
        }
    }

    public static AmadronOfferCustom loadFromNBT(NBTTagCompound tag) {
        AmadronOffer offer = AmadronOffer.loadFromNBT(tag);
        AmadronOfferCustom custom = new AmadronOfferCustom(offer.getInput(), offer.getOutput(), tag.getString("offeringPlayerName"), tag.getString("offeringPlayerId"));
        custom.inStock = tag.getInteger("inStock");
        custom.maxTrades = tag.getInteger("maxTrades");
        custom.pendingPayments = tag.getInteger("pendingPayments");
        if (tag.hasKey("providingDimensionId")) {
            custom.setProvidingPosition(new BlockPos(tag.getInteger("providingX"), tag.getInteger("providingY"), tag.getInteger("providingZ")), tag.getInteger("providingDimensionId"));
        }
        if (tag.hasKey("returningDimensionId")) {
            custom.setReturningPosition(new BlockPos(tag.getInteger("returningX"), tag.getInteger("returningY"), tag.getInteger("returningZ")), tag.getInteger("returningDimensionId"));
        }
        return custom;
    }

    public void writeToBuf(ByteBuf buf) {
        super.writeToBuf(buf);
        ByteBufUtils.writeUTF8String(buf, offeringPlayerName);
        ByteBufUtils.writeUTF8String(buf, offeringPlayerId);
        if (providingPosition != null) {
            buf.writeBoolean(true);
            buf.writeInt(providingPosition.getX());
            buf.writeInt(providingPosition.getY());
            buf.writeInt(providingPosition.getZ());
            buf.writeInt(providingDimensionId);
        } else {
            buf.writeBoolean(false);
        }
        if (returningPosition != null) {
            buf.writeBoolean(true);
            buf.writeInt(returningPosition.getX());
            buf.writeInt(returningPosition.getY());
            buf.writeInt(returningPosition.getZ());
            buf.writeInt(returningDimensionId);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(inStock);
        buf.writeInt(maxTrades);
        buf.writeInt(pendingPayments);
    }

    public static AmadronOfferCustom loadFromBuf(ByteBuf buf) {
        AmadronOfferCustom offer = new AmadronOfferCustom(PacketSyncAmadronOffers.readFluidOrItemStack(buf), PacketSyncAmadronOffers.readFluidOrItemStack(buf), ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
        if (buf.readBoolean()) {
            offer.setProvidingPosition(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()), buf.readInt());
        }
        if (buf.readBoolean()) {
            offer.setReturningPosition(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()), buf.readInt());
        }
        offer.inStock = buf.readInt();
        offer.maxTrades = buf.readInt();
        offer.pendingPayments = buf.readInt();
        return offer;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("offeringPlayerName", offeringPlayerName);
        json.addProperty("offeringPlayerId", offeringPlayerId);
        json.addProperty("inStock", inStock);
        json.addProperty("maxTrades", maxTrades);
        json.addProperty("pendingPayments", pendingPayments);
        if (providingPosition != null) {
            json.addProperty("providingDimensionId", providingDimensionId);
            json.addProperty("providingX", providingPosition.getX());
            json.addProperty("providingY", providingPosition.getY());
            json.addProperty("providingZ", providingPosition.getZ());
        }
        if (returningPosition != null) {
            json.addProperty("returningDimensionId", returningDimensionId);
            json.addProperty("returningX", returningPosition.getX());
            json.addProperty("returningY", returningPosition.getY());
            json.addProperty("returningZ", returningPosition.getZ());
        }
        return json;
    }

    public static AmadronOfferCustom fromJson(JsonObject json) {
        AmadronOffer offer = AmadronOffer.fromJson(json);
        if (offer != null) {
            AmadronOfferCustom custom = new AmadronOfferCustom(offer.input, offer.output, json.get("offeringPlayerName").getAsString(), json.get("offeringPlayerId").getAsString());
            custom.inStock = json.get("inStock").getAsInt();
            custom.maxTrades = json.get("maxTrades").getAsInt();
            custom.pendingPayments = json.get("pendingPayments").getAsInt();
            if (json.has("providingDimensionId")) {
                custom.providingDimensionId = json.get("providingDimensionId").getAsInt();
                custom.providingPosition = new BlockPos(json.get("providingX").getAsInt(), json.get("providingY").getAsInt(), json.get("providingZ").getAsInt());
            }
            if (json.has("returningDimensionId")) {
                custom.returningDimensionId = json.get("returningDimensionId").getAsInt();
                custom.returningPosition = new BlockPos(json.get("returningX").getAsInt(), json.get("returningY").getAsInt(), json.get("returningZ").getAsInt());
            }
            return custom;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return super.toString() + " - " + offeringPlayerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AmadronOfferCustom) {
            AmadronOfferCustom offer = (AmadronOfferCustom) o;
            return super.equals(o) && offer.offeringPlayerId.equals(offeringPlayerId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + offeringPlayerId.hashCode();
    }
}
