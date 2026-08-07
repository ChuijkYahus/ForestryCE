package forestry.core.platform.loot;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@FeatureProvider
public class CoreLootFunctions {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = REGISTRY.getRegistry(Registries.LOOT_FUNCTION_TYPE);

	public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<?>> ORGANISM = LOOT_FUNCTIONS.register("set_species_nbt", () -> new LootItemFunctionType<>(OrganismFunction.CODEC));
}
