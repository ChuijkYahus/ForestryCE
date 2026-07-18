package forestry.arboriculture.genetics;

import com.mojang.authlib.GameProfile;
import forestry.Forestry;
import forestry.api.arboriculture.IArboristTracker;
import forestry.api.genetics.ForestrySpeciesTypes;
import forestry.api.genetics.ISpecies;
import forestry.core.advancements.ApicultureResearchTrigger;
import forestry.core.advancements.ArboricultureResearchTrigger;
import forestry.core.advancements.ForestryAdvancementTriggers;
import forestry.core.genetics.BreedingTracker;
import net.minecraft.world.level.Level;

public class ArboristTracker extends BreedingTracker implements IArboristTracker {
	public ArboristTracker() {
		super(ForestrySpeciesTypes.TREE);
	}

	@Override
	public void registerPickup(ISpecies<?> species) {
		//discover(species);
	}

	public void registerProgress(Level level, GameProfile profile, ISpecies<?> species){
		double researchPercentage = (double) this.getSpeciesBred() / species.getType().getSpeciesCount();
		//Forestry.LOGGER.info("Player has researched: " + researchPercentage);
		ArboricultureResearchTrigger.TriggerInstance.checkIfResearchIsGreaterThan(researchPercentage);
		ForestryAdvancementTriggers.ARBORICULTURE_RESEARCH_TRIGGER.trigger(level, profile, researchPercentage);
	}
}
