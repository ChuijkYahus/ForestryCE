package forestry.core.advancements;

import net.minecraft.advancements.CriteriaTriggers;

public class ForestryAdvancementTriggers {
	public static final DiscoverSpeciesTrigger DISCOVER_SPECIES_TRIGGER =
		new DiscoverSpeciesTrigger();
	public static final ApicultureResearchTrigger APICULTURE_RESEARCH_TRIGGER =
		new ApicultureResearchTrigger();
	public static final ArboricultureResearchTrigger ARBORICULTURE_RESEARCH_TRIGGER =
		new ArboricultureResearchTrigger();

	public static void init() {
		CriteriaTriggers.register(DISCOVER_SPECIES_TRIGGER);
		CriteriaTriggers.register(APICULTURE_RESEARCH_TRIGGER);
		CriteriaTriggers.register(ARBORICULTURE_RESEARCH_TRIGGER);
	}
}
