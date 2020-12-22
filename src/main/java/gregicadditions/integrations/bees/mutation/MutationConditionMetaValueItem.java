package gregicadditions.integrations.bees.mutation;

import forestry.api.climate.IClimateProvider;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutationCondition;
import forestry.api.multiblock.IMultiblockComponent;
import forestry.apiculture.multiblock.TileAlveary;
import forestry.core.tiles.TileUtil;
import gregicadditions.integrations.bees.alveary.TileGTAlveary;
import gregtech.api.items.metaitem.MetaItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MutationConditionMetaValueItem implements IMutationCondition {
    private final List<MetaItem<?>.MetaValueItem> conditions;
    private final int chance;
    private final int cost;

    public MutationConditionMetaValueItem(Integer cost, Integer chance, MetaItem<?>.MetaValueItem... items) {
        conditions = new ArrayList<>();
        conditions.addAll(Arrays.asList(items));
        this.chance = chance;
        this.cost = cost;
    }

    public MutationConditionMetaValueItem(Integer chance, MetaItem<?>.MetaValueItem... items) {
        this(0, chance, items);
    }

    public MutationConditionMetaValueItem(MetaItem<?>.MetaValueItem... items) {
        this(100, items);
    }

    @Override
    public float getChance(World world, BlockPos pos, IAllele allele0, IAllele allele1, IGenome genome0, IGenome genome1, IClimateProvider climate) {
        TileEntity tile;
        tile = TileUtil.getTile(world, pos);
        float result = 0;
        if (tile instanceof TileAlveary) {
            for(IMultiblockComponent component :((TileAlveary)tile).getMultiblockLogic().getController().getComponents()) {
                if (component instanceof TileGTAlveary){
                    ItemStackHandler itemStackHandler = ((TileGTAlveary) component).getItemHandler();
                    for (int slot = 0; slot < itemStackHandler.getSlots(); slot++) {
                        ItemStack itemStack = itemStackHandler.getStackInSlot(slot);
                        if (itemStack.getItem() instanceof MetaItem){
                            MetaItem<?> metaItem = (MetaItem<?>) itemStack.getItem();
                            if (cost > 0){
                                result = conditions.stream().anyMatch(condition -> metaItem.getItem(itemStack) == condition &&
                                        itemStack.getCount() >= cost)
                                        ? itemStackHandler.extractItem(slot, cost, false).getCount() == cost
                                        ? world.rand.nextInt(100) < chance ? 1 : 0 : 0 : 0;
                            } else {
                                result = conditions.isEmpty()? 1 : conditions.stream().anyMatch(condition -> metaItem.getItem(itemStack) == condition)
                                        ? world.rand.nextInt(100) < chance ? 1 : 0 : 0;
                            }
                            if (result > 0) return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String getDescription() {
        StringBuilder displayName = new StringBuilder("[");
        for (MetaItem<?>.MetaValueItem item : conditions){
            displayName.append(I18n.format(item.getStackForm().getDisplayName())).append("/");
        }
        displayName.setCharAt(displayName.length() - 1, ']');
        return I18n.format("for.mutation.condition.metavalueitem", displayName.toString(), 2 * cost);
    }
}
