package gregicadditions.machines.multi.override;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.capabilities.impl.GAMultiblockRecipeLogic;
import gregicadditions.item.GAMetaBlocks;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.unification.material.Materials;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static gregtech.api.unification.material.Materials.StainlessSteel;

public class MetaTileEntityDistillationTower extends gregtech.common.metatileentities.multi.electric.MetaTileEntityDistillationTower {
    public MetaTileEntityDistillationTower(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.recipeMapWorkable = new GAMultiblockRecipeLogic(this);
    }

    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityDistillationTower(this.metaTileEntityId);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return GAMetaBlocks.METAL_CASING.get(Materials.StainlessSteel);
    }

    @Override
    public IBlockState getCasingState() {
        return GAMetaBlocks.getMetalCasingBlockState(StainlessSteel);
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            IEnergyContainer energyContainer = recipeMapWorkable.getEnergyContainer();
            if (energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
                long maxVoltage = energyContainer.getInputVoltage();
                String voltageName = GAValues.VN[GAUtility.getTierByVoltage(maxVoltage)];
                textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, voltageName));
            }

            if (!recipeMapWorkable.isWorkingEnabled()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.work_paused"));

            } else if (recipeMapWorkable.isActive()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.running"));
                int currentProgress = (int) (recipeMapWorkable.getProgressPercent() * 100);
                textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
            } else {
                textList.add(new TextComponentTranslation("gregtech.multiblock.idling"));
            }

            if (recipeMapWorkable.isHasNotEnoughEnergy()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.not_enough_energy").setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }
    }

    @NotNull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.DISTILLERY_OVERLAY;
    }
}
