package gregicadditions.machines;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
import gregicadditions.machines.overrides.GATieredMetaTileEntity;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.render.Textures;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.IntStream;

public class TileEntityWorldAccelerator extends GATieredMetaTileEntity implements IControllable {

    private final long energyPerTick;
    private boolean tileMode = true;
    private boolean isActive = false;
    private boolean isPaused = false;
    private int lastTick;

    public TileEntityWorldAccelerator(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        //consume 8 amps
        this.energyPerTick = GAValues.V[tier] * getMaxInputOutputAmperage();
        this.lastTick = 0;
        initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new TileEntityWorldAccelerator(metaTileEntityId, getTier());
    }

    void addDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation(tileMode ? "gregtech.machine.world_accelerator.mode.tile" : "gregtech.machine.world_accelerator.mode.entity"));
    }


    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.extendedBuilder();
        builder.image(7, 4, 162, 121, GuiTextures.DISPLAY);
        builder.label(10, 7, getMetaFullName(), 0xFFFFFF);
        builder.widget(new AdvancedTextWidget(10, 17, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(156));
        builder.bindPlayerInventory(entityPlayer.inventory, 134);
        return builder.build(getHolder(), entityPlayer);

    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.universal.tooltip.voltage_in", energyContainer.getInputVoltage(), GAValues.VN[getTier()]));
        tooltip.add(I18n.format("gtadditions.universal.tooltip.amperage_in", getMaxInputOutputAmperage()));
        tooltip.add(I18n.format("gregtech.universal.tooltip.energy_storage_capacity", energyContainer.getEnergyCapacity()));
        tooltip.add(I18n.format("gtadditions.machine.world_accelerator.description"));
        tooltip.add(I18n.format("gtadditions.machine.world_accelerator.area", getArea(), getArea()));
    }

    @Override
    protected long getMaxInputOutputAmperage() {
        return 8L;
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote && lastTick != FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter()) {
            lastTick = FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter();
            if (isPaused) {
                if (isActive)
                    setActive(false);
                return;
            }
            if (energyContainer.getEnergyStored() < energyPerTick) {
                if (isActive)
                    setActive(false);
                return;
            }
            if (!isActive)
                setActive(true);
            energyContainer.removeEnergy(energyPerTick);
            WorldServer world = (WorldServer) this.getWorld();
            BlockPos worldAcceleratorPos = getPos();
            if (tileMode) {
                BlockPos[] neighbours = new BlockPos[]{worldAcceleratorPos.down(), worldAcceleratorPos.up(), worldAcceleratorPos.north(), worldAcceleratorPos.south(), worldAcceleratorPos.east(), worldAcceleratorPos.west()};
                for (BlockPos neighbour : neighbours) {
                    TileEntity targetTE = world.getTileEntity(neighbour);
                    if (targetTE == null || targetTE instanceof TileEntityMaterialPipeBase || targetTE instanceof MetaTileEntityHolder) {
                        continue;
                    }
                    boolean horror = false;
                    if (clazz != null && targetTE instanceof ITickable) {
                        horror = clazz.isInstance(targetTE);
                    }
                    if (targetTE instanceof ITickable && (!horror || !world.isRemote)) {
                        IntStream.range(0, (int) Math.pow(2, getTier())).forEach(value -> {
                            ((ITickable) targetTE).update();
                        });
                    }
                }
            } else {
                BlockPos upperConner = worldAcceleratorPos.north(getTier()).east(getTier());
                for (int x = 0; x < getArea(); x++) {
                    BlockPos row = upperConner.south(x);
                    for (int y = 0; y < getArea(); y++) {
                        BlockPos cell = row.west(y);

                        IBlockState targetBlock = world.getBlockState(cell);
                        IntStream.range(0, (int) Math.pow(2, getTier())).forEach(value -> {
                            if (world.rand.nextInt(100) == 0) {
                                if (targetBlock.getBlock().getTickRandomly()) {
                                    targetBlock.getBlock().randomTick(world, cell, targetBlock, world.rand);
                                }
                            }
                        });
                    }
                }
            }
        }

    }

    public int getArea() {
        return (getTier() * 2) + 1;
    }

    static Class clazz;

    static {
        try {
            clazz = Class.forName("cofh.core.block.TileCore");
        } catch (Exception e) {

        }
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.AMPLIFAB_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }


    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        tileMode = !tileMode;
        if (!getWorld().isRemote) {
            playerIn.sendStatusMessage(new TextComponentTranslation(tileMode ? "gregtech.machine.world_accelerator.mode.tile" : "gregtech.machine.world_accelerator.mode.entity"), false);
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("TileMode", new NBTTagString(Boolean.valueOf(tileMode).toString()));
        data.setTag("isPaused", new NBTTagString(Boolean.valueOf(isPaused).toString()));
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        tileMode = Boolean.parseBoolean(data.getString("TileMode"));
        isPaused = Boolean.parseBoolean(data.getString("isPaused"));
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        markDirty();
        if (!getWorld().isRemote) {
            writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            getHolder().scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public boolean isWorkingEnabled() {
        return !isPaused;
    }

    @Override
    public void setWorkingEnabled(boolean b) {
        isPaused = !b;
        getHolder().notifyBlockUpdate();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }
        return super.getCapability(capability, side);
    }

    public boolean isTileMode() {
        return tileMode;
    }

    public void setTileMode(boolean tileMode) {
        this.tileMode = tileMode;
    }
}
