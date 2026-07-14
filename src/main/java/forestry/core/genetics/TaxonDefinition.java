package forestry.core.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import forestry.api.genetics.TaxonomicRank;

/**
 * A datapack-loadable definition of a taxon (a node in the classification tree), loaded through the reloadable
 * {@link forestry.apiculture.genetics.TaxonManager} (a {@code SimpleJsonResourceReloadListener} over the {@code taxon}
 * folder, synced by {@code TaxonSyncPacket}). Each entry defines one taxon by {@link #name} under an existing
 * {@link #parent} taxon; its {@link TaxonomicRank rank} is derived from the parent's rank ({@code parent.rank().next()}).
 * <p>
 * This exists because a species genome references its genus by name, and that genus must resolve to a registered taxon
 * when the species is built ({@code Species}'s constructor). Code plugins define their taxa through
 * {@code IGeneticRegistration#defineTaxon}; this is the datapack analogue, so a pack (e.g. an add-on's genera) can
 * add genera as pure JSON. The definitions are merged onto the code-registered taxa by
 * {@code GeneticManager#applyDatapackTaxa} on every (re)load, before species are projected.
 * <p>
 * Kept intentionally minimal ({@code parent} + {@code name}, no default chromosomes): branch/genus allele templates
 * are flattened into each species' genome by the generator rather than inherited from the taxon, so a datapack taxon
 * only needs to exist for genus resolution to succeed.
 */
public record TaxonDefinition(String parent, String name) {
	public static final Codec<TaxonDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("parent").forGetter(TaxonDefinition::parent),
		Codec.STRING.fieldOf("name").forGetter(TaxonDefinition::name)
	).apply(instance, TaxonDefinition::new));

	/** Used by {@code TaxonSyncPacket} to deliver datapack taxa to remote clients (a species' genus resolves there too). */
	public static final StreamCodec<RegistryFriendlyByteBuf, TaxonDefinition> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, TaxonDefinition::parent,
		ByteBufCodecs.STRING_UTF8, TaxonDefinition::name,
		TaxonDefinition::new
	);
}
