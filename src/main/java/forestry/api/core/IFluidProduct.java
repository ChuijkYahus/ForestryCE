package forestry.api.core;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import forestry.api.ForestryRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A fluid analog of {@link IProduct}. Represents a fluid output, for example, from the Squeezer.
 *
 * @see FluidProduct The default fixed-fluid implementation used by Forestry's own recipes.
 */
public interface IFluidProduct {
	String TYPE_KEY = "type";

	/**
	 * The dispatch codec, resolving the {@code "type"} key against {@link ForestryRegistries#FLUID_PRODUCT_TYPE}.
	 * Unlike a stock {@link Codec#dispatch}, the key is optional and defaults to {@link FluidProduct#TYPE}; encoding
	 * a {@link FluidProduct} omits the key entirely. This keeps the common case (a fixed fluid) as clean,
	 * backwards-compatible JSON, while dynamic products (addon-provided tag/random/chance outputs) round-trip
	 * through their own type by declaring {@code "type"}.
	 */
	MapCodec<IFluidProduct> MAP_CODEC = new MapCodec<>() {
		@Override
		public <T> DataResult<IFluidProduct> decode(DynamicOps<T> ops, MapLike<T> input) {
			T typeValue = input.get(TYPE_KEY);
			DataResult<FluidProductType<?>> type = typeValue == null
				? DataResult.success(FluidProduct.TYPE)
				: ResourceLocation.CODEC.parse(ops, typeValue).flatMap(IFluidProduct::byId);
			return type.flatMap(t -> t.codec().decode(ops, input).map(product -> (IFluidProduct) product));
		}

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T> RecordBuilder<T> encode(IFluidProduct input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			FluidProductType<?> type = input.type();
			if (type != FluidProduct.TYPE) {
				ResourceLocation id = ForestryRegistries.FLUID_PRODUCT_TYPE.getKey(type);
				if (id == null) {
					return prefix.withErrorsFrom(DataResult.error(() -> "Unregistered fluid product type: " + type));
				}
				prefix.add(TYPE_KEY, ResourceLocation.CODEC.encodeStart(ops, id));
			}
			return ((MapCodec) type.codec()).encode(input, ops, prefix);
		}

		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops) {
			return Stream.of(ops.createString(TYPE_KEY));
		}
	};

	Codec<IFluidProduct> CODEC = MAP_CODEC.codec();

	/**
	 * Network counterpart of {@link #CODEC}. Always writes the registry name of the product's type, since the
	 * "omit the default" trick only buys readability in JSON.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	StreamCodec<RegistryFriendlyByteBuf, IFluidProduct> STREAM_CODEC = StreamCodec.of(
		(buf, product) -> {
			ResourceLocation.STREAM_CODEC.encode(buf, idOf(product.type()));
			((StreamCodec) product.type().streamCodec()).encode(buf, product);
		},
		buf -> {
			ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
			return byId(id).getOrThrow().streamCodec().decode(buf);
		});

	private static DataResult<FluidProductType<?>> byId(ResourceLocation id) {
		FluidProductType<?> type = ForestryRegistries.FLUID_PRODUCT_TYPE.get(id);
		if (type == null) {
			return DataResult.error(() -> "Unknown fluid product type: " + id);
		}
		return DataResult.success(type);
	}

	private static ResourceLocation idOf(FluidProductType<?> type) {
		ResourceLocation id = ForestryRegistries.FLUID_PRODUCT_TYPE.getKey(type);
		if (id == null) {
			throw new IllegalArgumentException("Unregistered fluid product type: " + type);
		}
		return id;
	}

	/**
	 * Creates a new, non-random stack to represent this product in recipe viewers. <p>
	 *
	 * The fluid amount should be the MAXIMUM amount of fluid a stack returned by this product's
	 * {@link #createRandomFluidStack(RandomSource)} can have. For example, if your recipe produces 100-300 mB of some
	 * fluid, this method should return a stack with 300 mB of fluid. This ensures machines like the Squeezer know how much
	 * space must be available to fit this fluid product into the output tank. <p>
	 *
	 * This can return empty, like in the case where the product is a modded fluid not present in the current instance.
	 * However, in these cases, your recipe should use the proper conditions to disable itself rather than relying on this behavior.
	 *
	 * @return A new stack representing this product. If your product is dynamic, return a "default" stack.
	 */
	FluidStack createFluidStack();

	/**
	 * Used to produce a fluid stack from this product, taking randomness into account. This is the final result produced
	 * by machines. <p>
	 *
	 * @param random The random source. If no randomness is desired, this defaults to {@link #createFluidStack()}.
	 * @return A new stack for this cycle, possibly empty or smaller than the representative stack.
	 */
	default FluidStack createRandomFluidStack(RandomSource random) {
		return createFluidStack();
	}

	/**
	 * The type of this product, used to (de)serialize it via {@link #CODEC}. Plain {@link FluidProduct} instances
	 * return {@link FluidProduct#TYPE}, which the dispatch codec treats as the default: it serializes without a
	 * {@code "type"} key.
	 *
	 * @return The type of this product.
	 */
	FluidProductType<?> type();
}
