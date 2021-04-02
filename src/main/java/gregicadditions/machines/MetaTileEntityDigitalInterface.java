package gregicadditions.machines;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.client.ClientHandler;
import gregicadditions.covers.CoverDigitalInterface;
import gregtech.api.block.BlockStateTileEntity;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

public class MetaTileEntityDigitalInterface extends MetaTileEntity {
    private final CoverDigitalInterface[] coverBehaviors = new CoverDigitalInterface[6];

    public MetaTileEntityDigitalInterface(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    public CoverDigitalInterface[] getCoverBehaviors() {
        return coverBehaviors;
    }

    @Override
    public void update() {
        super.update();

        if (!this.getWorld().isRemote) {
            for (CoverDigitalInterface cover : coverBehaviors) {
                if(cover != null) {
                    cover.update();
                }
            }
            if (this.getTimer() % 20 == 0) {
                for (EnumFacing facing : EnumFacing.VALUES) {
                    TileEntity tileEntity = this.getWorld().getTileEntity(this.getPos().offset(facing));
                    int index = facing.getIndex();
                    boolean inValid = tileEntity == null || tileEntity instanceof BlockStateTileEntity;
                    if (inValid && this.coverBehaviors[facing.getIndex()] != null) {
                        this.coverBehaviors[index].onRemoved();
                        this.coverBehaviors[index] = null;
                    } else if (!inValid && this.coverBehaviors[index] == null) {
                        this.coverBehaviors[index] = new CoverDigitalInterface(this, facing);
                    } else {
                        continue;
                    }
                    this.writeCustomData(5, (buffer) -> {
                        buffer.writeByte(index);
                        buffer.writeBoolean(this.coverBehaviors[index] != null);
                        if (this.coverBehaviors[index] != null) {
                            this.coverBehaviors[index].writeInitialSyncData(buffer);
                        }
                    });
                    if (this.getHolder() != null) {
                        this.getHolder().notifyBlockUpdate();
                        this.getHolder().markDirty();
                    }
                }
            }
        }

    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        for (EnumFacing coverSide : EnumFacing.VALUES) {
            CoverBehavior coverBehavior = coverBehaviors[coverSide.getIndex()];
            if (coverBehavior != null) {
                buf.writeBoolean(true);
                coverBehavior.writeInitialSyncData(buf);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        for (EnumFacing coverSide : EnumFacing.VALUES) {
            if (buf.readBoolean()) {
                CoverDigitalInterface coverBehavior = new CoverDigitalInterface(this, coverSide);
                coverBehavior.readInitialSyncData(buf);
                this.coverBehaviors[coverSide.getIndex()] = coverBehavior;
            }
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == -7) {
            CoverDigitalInterface coverBehavior = coverBehaviors[buf.readByte()];
            int id = buf.readVarInt();
            if (coverBehavior != null) {
                coverBehavior.readUpdateData(id, buf);
            }
        } else if(dataId == 5) {
            int index = buf.readByte();
            if (buf.readBoolean()) {
                coverBehaviors[index] = new CoverDigitalInterface(this, EnumFacing.VALUES[index]);
                coverBehaviors[index].readInitialSyncData(buf);
            } else {
                coverBehaviors[index] = null;
            }
            this.getHolder().scheduleChunkForRenderUpdate();
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagList coversList = new NBTTagList();

        for (EnumFacing coverSide : EnumFacing.VALUES) {
            CoverDigitalInterface coverBehavior = this.coverBehaviors[coverSide.getIndex()];
            if (coverBehavior != null) {
                NBTTagCompound tagCompound = new NBTTagCompound();
                tagCompound.setByte("Side", (byte) coverSide.getIndex());
                coverBehavior.writeToNBT(tagCompound);
                coversList.appendTag(tagCompound);
            }
        }

        data.setTag("ICovers", coversList);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        NBTTagList coversList = data.getTagList("ICovers", 10);

        for(int index = 0; index < coversList.tagCount(); ++index) {
            data = coversList.getCompoundTagAt(index);
            if (data.hasKey("Side")) {
                EnumFacing coverSide = EnumFacing.VALUES[data.getByte("Side")];
                CoverDigitalInterface coverBehavior = new CoverDigitalInterface(this, coverSide);
                coverBehavior.readFromNBT(data);
                this.coverBehaviors[coverSide.getIndex()] = coverBehavior;
            }
        }
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder metaTileEntityHolder) {
        return new MetaTileEntityDigitalInterface(this.metaTileEntityId);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing == this.getFrontFacing()) {
                //TODO
            } else{
                ClientHandler.COVER_INTERFACE_PROXY.renderSided(facing, renderState, translation, pipeline);
            }

        }
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return null;
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        return false;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        TileEntity tileEntity = side == null? null : this.getWorld() == null? null : this.getWorld().getTileEntity(this.getPos().offset(side));
        return this.getFrontFacing() == side && capability == GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER ?
                (T) CoverDigitalInterface.proxyCapability : tileEntity == null || tileEntity instanceof BlockStateTileEntity ?
                null : tileEntity.getCapability(capability, side.getOpposite());
    }

    @Override
    public boolean canPlaceCoverOnSide(EnumFacing side) {
        return false;
    }

    @Override
    public boolean isValidFrontFacing(EnumFacing facing) {
        return true;
    }
}
