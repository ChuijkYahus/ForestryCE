package forestry.apiculture.alveary.multiblock;

import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.BlockAlveary;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileAlvearyStabiliser extends TileAlveary implements IAlvearyComponent.BeeModifier<MultiblockLogicAlveary> {
	private static final IBeeModifier MODIFIER = new IBeeModifier() {
		@Override
		public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> mutation, float currentChance) {
			return 0.0f;
		}
	};

	public TileAlvearyStabiliser(BlockPos pos, BlockState state) {
		super(BlockAlveary.Type.STABILISER, pos, state);
	}

	@Override
	public IBeeModifier getBeeModifier() {
		return MODIFIER;
	}
}
