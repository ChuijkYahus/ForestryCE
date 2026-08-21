package forestry.core.platform.models;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class ModelBlockCached<B extends Block, K> extends ModelBlockDefault<B, K> {
	private static final Set<ModelBlockCached<?, ?>> CACHE_PROVIDERS = new HashSet<>();

	// todo test performance of this. from my experience, Cache is slow.
	private final Cache<K, BakedModel> inventoryCache;
	private final Cache<K, BakedModel> worldCache;

	public static void clear() {
		for (ModelBlockCached<?, ?> modelBlockCached : CACHE_PROVIDERS) {
			modelBlockCached.worldCache.invalidateAll();
			modelBlockCached.inventoryCache.invalidateAll();
		}
	}

	protected ModelBlockCached(Class<B> blockClass) {
		super(blockClass);

        this.worldCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();
        this.inventoryCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

		CACHE_PROVIDERS.add(this);
	}

	@Override
	protected BakedModel getModel(BlockState state, ModelData extraData) {
		return getOrBake(this.worldCache, getWorldKey(state, extraData), () -> super.getModel(state, extraData));
	}

	@Override
	protected BakedModel getModel(ItemStack stack, Level world) {
		K key = getInventoryKey(stack);

		return getOrBake(this.inventoryCache, key, () -> bakeModel(stack, world, key));
	}

	/**
	 * Used to bake a model at most once per cache key.
	 *
	 * @param cache The cache to read the model from, and to store a newly baked model in
	 * @param key   The cache key of the model
	 * @param baker The fallback that bakes the model when the cache does not have it
	 * @return The cached model, baked by this call if it was absent
	 */
	private static <K> BakedModel getOrBake(Cache<K, BakedModel> cache, K key, Callable<BakedModel> baker) {
		try {
			// get(key, baker) rather than getIfPresent + put: chunk meshing runs on several worker threads, which
			// all miss the same key at once after a cache clear and bake redundant copies of the same model
			return cache.get(key, baker);
		} catch (ExecutionException e) {
			// the bakers declare no checked exceptions, so this is unreachable
			throw new RuntimeException("Failed to bake a Forestry block model", e.getCause());
		}
	}
}
