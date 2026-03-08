package forestry.core.advancements;

import net.minecraft.advancements.CriteriaTriggers;

public class ForestryAdvancementTriggers {
	public static final DiscoverSpeciesTrigger DISCOVER_SPECIES_TRIGGER =
		new DiscoverSpeciesTrigger();

	public static void init() {
		CriteriaTriggers.register(DISCOVER_SPECIES_TRIGGER);
	}
}
