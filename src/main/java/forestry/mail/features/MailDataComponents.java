package forestry.mail.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.mail.Letter;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mail-side {@link DataComponentType} registrations.
 */
@FeatureProvider
public class MailDataComponents {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);
	private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = REGISTRY.getRegistry(Registries.DATA_COMPONENT_TYPE);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Letter>> LETTER_DATA =
		DATA_COMPONENT_TYPES.register(
			"letter_data",
			() -> DataComponentType.<Letter>builder()
				.persistent(Letter.CODEC)
				.networkSynchronized(ByteBufCodecs.fromCodec(Letter.CODEC))
				.build());
}
