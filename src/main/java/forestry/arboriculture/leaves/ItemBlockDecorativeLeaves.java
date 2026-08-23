package forestry.arboriculture.leaves;

import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.client.IForestryClientApi;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ITintedItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemBlockDecorativeLeaves extends ItemBlockForestry<BlockDecorativeLeaves> implements ITintedItem {
	public ItemBlockDecorativeLeaves(BlockDecorativeLeaves block, Item.Properties properties) {
		super(block, properties);
	}

	@Override
	public Component getName(ItemStack itemStack) {
		BlockDecorativeLeaves block = getBlock();
		ForestryLeafType treeDefinition = block.getType();
		return ItemBlockLeaves.getDisplayName(treeDefinition.getIndividual().getSpecies());
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public int getColorFromItemStack(ItemStack itemStack, int renderPass) {
		BlockDecorativeLeaves block = getBlock();
		ForestryLeafType leafType = block.getType();

		ITree individual = leafType.getIndividual();
		IGenome genome = individual.getGenome();

		if (renderPass == BlockAbstractLeaves.FRUIT_COLOR_INDEX) {
			IFruit fruitProvider = genome.resolveActive(TreeChromosomes.FRUIT);
			return fruitProvider.getDecorativeColor();
		}
		return IForestryClientApi.INSTANCE.getTreeManager().getTint(individual.getSpecies()).get(null, null);
	}
}
