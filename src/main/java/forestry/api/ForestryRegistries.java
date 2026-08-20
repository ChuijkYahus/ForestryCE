package forestry.api;

import com.mojang.serialization.MapCodec;

import forestry.api.apiculture.FlowerTypeType;
import forestry.api.apiculture.IFlowerType;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.core.FluidProductType;
import forestry.api.core.IFluidProduct;
import forestry.api.core.IProduct;
import forestry.api.core.ProductType;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.genetics.IMutationCondition;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.MutationConditionType;
import forestry.api.mail.IPostalCarrier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class ForestryRegistries {
	public static final Registry<ICircuit> CIRCUIT = new RegistryBuilder<>(Keys.CIRCUIT_TYPE)
		.sync(true)
		.create();

	public static final Registry<IPostalCarrier> POSTAL_CARRIER = new RegistryBuilder<>(Keys.POSTAL_CARRIER)
		.sync(true)
		.create();

	public static final Registry<ISpeciesType<?, ?>> SPECIES_TYPE = new RegistryBuilder<>(Keys.SPECIES_TYPE)
		.sync(true)
		.create();

	/**
	 * Serializer registry for {@link IBeeEffect} primitive types, used to dispatch {@link IBeeEffect#CODEC}
	 * in datapack effect definitions. Not synced: the datapack-loaded {@link IBeeEffect} instances themselves
	 * are delivered to clients by {@code BeeEffectSyncPacket} (mirroring {@code BeeSpeciesSyncPacket}); this
	 * registry only needs to be populated on both sides at mod init to resolve the {@code "type"} dispatch key.
	 */
	public static final Registry<MapCodec<? extends IBeeEffect>> BEE_EFFECT_TYPE = new RegistryBuilder<>(Keys.BEE_EFFECT_TYPE)
		.create();

	/**
	 * Serializer registry for {@link IMutationCondition} types, used to dispatch {@link IMutationCondition#CODEC}
	 * in datapack mutation recipes. Not synced: the entries are code-registered on both sides at mod init, and the
	 * mutation recipes that reference them travel over the vanilla recipe sync keyed by name.
	 */
	public static final Registry<MutationConditionType<?>> MUTATION_CONDITION_TYPE = new RegistryBuilder<>(Keys.MUTATION_CONDITION_TYPE)
		.create();

	/**
	 * Serializer registry for {@link IProduct} types, used to dispatch {@link IProduct#CODEC} in datapack species
	 * definitions and machine recipes. Not synced: the entries are code-registered on both sides at mod init, and
	 * the definitions that reference them are delivered by their own sync packets keyed by name.
	 */
	public static final Registry<ProductType<?>> PRODUCT_TYPE = new RegistryBuilder<>(Keys.PRODUCT_TYPE)
		.create();

	/**
	 * Serializer registry for {@link IFluidProduct} types, used to dispatch {@link IFluidProduct#CODEC} in machine
	 * recipes. Fluid analog of {@link #PRODUCT_TYPE}, and not synced for the same reason.
	 */
	public static final Registry<FluidProductType<?>> FLUID_PRODUCT_TYPE = new RegistryBuilder<>(Keys.FLUID_PRODUCT_TYPE)
		.create();

	/**
	 * Serializer registry for {@link IFlowerType} types, used to dispatch {@link IFlowerType#CODEC} in datapack
	 * flower type definitions. Not synced: the definitions themselves are delivered by {@code FlowerTypeSyncPacket}.
	 */
	public static final Registry<FlowerTypeType<?>> FLOWER_TYPE_SERIALIZER = new RegistryBuilder<>(Keys.FLOWER_TYPE_SERIALIZER)
		.create();

	public static class Keys {
		public static final ResourceKey<Registry<ICircuit>> CIRCUIT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("circuit"));
		public static final ResourceKey<Registry<IPostalCarrier>> POSTAL_CARRIER = ResourceKey.createRegistryKey(ForestryConstants.forestry("postal_carrier"));
		public static final ResourceKey<Registry<ISpeciesType<?, ?>>> SPECIES_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("species_type"));
		public static final ResourceKey<Registry<MapCodec<? extends IBeeEffect>>> BEE_EFFECT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("bee_effect_type"));
		public static final ResourceKey<Registry<MutationConditionType<?>>> MUTATION_CONDITION_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("mutation_condition_type"));
		public static final ResourceKey<Registry<ProductType<?>>> PRODUCT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("product_type"));
		public static final ResourceKey<Registry<FluidProductType<?>>> FLUID_PRODUCT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("fluid_product_type"));
		public static final ResourceKey<Registry<FlowerTypeType<?>>> FLOWER_TYPE_SERIALIZER = ResourceKey.createRegistryKey(ForestryConstants.forestry("flower_type_serializer"));
	}
}
