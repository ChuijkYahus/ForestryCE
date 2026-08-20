package forestry.api.core;

import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import forestry.api.ForestryRegistries;
import it.unimi.dsi.fastutil.Hash;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Represents some item that has a set chance of being produced.
 *
 * @see Product The default implementation used in the majority of cases.
 */
public interface IProduct {
	/**
	 * A hashing strategy used for FastUtil custom hash collections.
	 * Currently, Forestry uses this to remove common products between species from the product list of a hybrid bee.
	 */
	Hash.Strategy<IProduct> ITEM_ONLY_STRATEGY = new Hash.Strategy<>() {
		@Override
		public int hashCode(@Nullable IProduct o) {
			return o == null ? 0 : o.item().hashCode();
		}

		@Override
		public boolean equals(@Nullable IProduct a, @Nullable IProduct b) {
			return (a == null || b == null) ? a == b : a.item() == b.item();
		}
	};

	String TYPE_KEY = "type";

	/**
	 * The dispatch codec, resolving the {@code "type"} key against {@link ForestryRegistries#PRODUCT_TYPE}. Unlike a
	 * stock {@link Codec#dispatch}, the key is optional and defaults to {@link Product#TYPE}; encoding a
	 * {@link Product} omits the key entirely. This keeps the overwhelmingly common case (a plain item stack) as
	 * clean, backwards-compatible JSON, while dynamic products (ex. the secret Patriotic bee's randomized firework)
	 * round-trip through their own type by declaring {@code "type"}.
	 */
	MapCodec<IProduct> MAP_CODEC = new MapCodec<>() {
		@Override
		public <T> DataResult<IProduct> decode(DynamicOps<T> ops, MapLike<T> input) {
			T typeValue = input.get(TYPE_KEY);
			DataResult<ProductType<?>> type = typeValue == null
				? DataResult.success(Product.TYPE)
				: ResourceLocation.CODEC.parse(ops, typeValue).flatMap(IProduct::byId);
			return type.flatMap(t -> t.codec().decode(ops, input).map(product -> (IProduct) product));
		}

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T> RecordBuilder<T> encode(IProduct input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			ProductType<?> type = input.type();
			if (type != Product.TYPE) {
				ResourceLocation id = ForestryRegistries.PRODUCT_TYPE.getKey(type);
				if (id == null) {
					return prefix.withErrorsFrom(DataResult.error(() -> "Unregistered product type: " + type));
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

	Codec<IProduct> CODEC = MAP_CODEC.codec();

	/**
	 * Network counterpart of {@link #CODEC}. Always writes the registry name of the product's type, since the
	 * "omit the default" trick only buys readability in JSON.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	StreamCodec<RegistryFriendlyByteBuf, IProduct> STREAM_CODEC = StreamCodec.of(
		(buf, product) -> {
			ResourceLocation.STREAM_CODEC.encode(buf, idOf(product.type()));
			((StreamCodec) product.type().streamCodec()).encode(buf, product);
		},
		buf -> {
			ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
			return byId(id).getOrThrow().streamCodec().decode(buf);
		});

	Codec<List<IProduct>> LIST_CODEC = CODEC.listOf();
	StreamCodec<RegistryFriendlyByteBuf, List<IProduct>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

	private static DataResult<ProductType<?>> byId(ResourceLocation id) {
		ProductType<?> type = ForestryRegistries.PRODUCT_TYPE.get(id);
		if (type == null) {
			return DataResult.error(() -> "Unknown product type: " + id);
		}
		return DataResult.success(type);
	}

	private static ResourceLocation idOf(ProductType<?> type) {
		ResourceLocation id = ForestryRegistries.PRODUCT_TYPE.getKey(type);
		if (id == null) {
			throw new IllegalArgumentException("Unregistered product type: " + type);
		}
		return id;
	}

	// todo should this be replaced with is(ItemStack) and getIconStack() methods instead?

	/**
	 * Gets the item this product contains. In the case of a dynamic product, return an item that might
	 * be used to display it in a screen or for equality purposes in {@link #ITEM_ONLY_STRATEGY}.
	 *
	 * @return The item this product represents.
	 */
	Item item();

	/**
	 * @return The set chance of this product being produced.
	 */
	float chance();

	/**
	 * @return A new stack of this product. If your product is dynamic, return a "default" nonempty stack.
	 */
	ItemStack createStack();

	/**
	 * Used to produce a random variant of this product.
	 *
	 * @param random The random source. If no randomness is desired, call {@link #createStack} instead.
	 * @return A new stack of this product with potentially random properties.
	 */
	default ItemStack createRandomStack(RandomSource random) {
		return createStack();
	}

	/**
	 * The type of this product, used to (de)serialize it via {@link #CODEC}. Most products are plain
	 * {@link Product} instances and return
	 * {@link Product#TYPE}, which the dispatch codec treats as the default: it serializes without a {@code "type"}
	 * key. Dynamic products (e.g. a randomized firework) return their own type so their randomness survives the
	 * round-trip through JSON and network sync.
	 *
	 * @return The type of this product.
	 */
	ProductType<?> type();
}
