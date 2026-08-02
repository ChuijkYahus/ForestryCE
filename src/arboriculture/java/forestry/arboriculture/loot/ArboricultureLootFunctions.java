package forestry.arboriculture.loot;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@FeatureProvider
public class ArboricultureLootFunctions {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);
	private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = REGISTRY.getRegistry(Registries.LOOT_FUNCTION_TYPE);

	// name matches the one generated loot tables reference
	public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<?>> COUNT = LOOT_FUNCTIONS.register("count_from_block", () -> new LootItemFunctionType<>(CountBlockFunction.CODEC));
}
