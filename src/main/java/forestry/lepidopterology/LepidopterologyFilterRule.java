package forestry.lepidopterology;

import forestry.api.genetics.ForestrySpeciesTypes;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.alleles.ButterflyChromosomes;
import forestry.api.genetics.filter.FilterData;
import forestry.api.genetics.filter.IFilterRule;
import forestry.api.genetics.filter.IFilterRuleType;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.sorting.DefaultFilterRuleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public enum LepidopterologyFilterRule implements IFilterRule {
	PURE_BREED(DefaultFilterRuleType.PURE_BREED) {
		@Override
		protected boolean isValid(IButterfly butterfly) {
			return butterfly.getGenome().getAllelePair(ButterflyChromosomes.SPECIES).isSameAlleles();
		}
	},
	NOCTURNAL(DefaultFilterRuleType.NOCTURNAL) {
		@Override
		protected boolean isValid(IButterfly butterfly) {
			return butterfly.getGenome().getActiveValue(ButterflyChromosomes.NEVER_SLEEPS);
		}
	},
	PURE_NOCTURNAL(DefaultFilterRuleType.PURE_NOCTURNAL) {
		@Override
		protected boolean isValid(IButterfly butterfly) {
			return butterfly.getGenome().getActiveValue(ButterflyChromosomes.NEVER_SLEEPS) && butterfly.getGenome().getAllelePair(ButterflyChromosomes.NEVER_SLEEPS).isSameAlleles();
		}
	},
	FLYER(DefaultFilterRuleType.FLYER) {
		@Override
		protected boolean isValid(IButterfly butterfly) {
			return butterfly.getGenome().getActiveValue(ButterflyChromosomes.TOLERATES_RAIN);
		}
	},
	PURE_FLYER(DefaultFilterRuleType.PURE_FLYER) {
		@Override
		protected boolean isValid(IButterfly butterfly) {
			return butterfly.getGenome().getActiveValue(ButterflyChromosomes.TOLERATES_RAIN) && butterfly.getGenome().getAllelePair(ButterflyChromosomes.TOLERATES_RAIN).isSameAlleles();
		}
	};

	LepidopterologyFilterRule(IFilterRuleType rule) {
		rule.addLogic(this);
	}

	public static void init() {
		// class load.
	}

	@Override
	public boolean isValid(ItemStack stack, FilterData data) {
		IIndividual individual = data.individual();
		if (!(individual instanceof IButterfly butterfly)) {
			return false;
		}
		return isValid(butterfly);
	}

	protected boolean isValid(IButterfly butterfly) {
		return false;
	}

	@Override
	public ResourceLocation getSpeciesTypeId() {
		return ForestrySpeciesTypes.BUTTERFLY;
	}

}
