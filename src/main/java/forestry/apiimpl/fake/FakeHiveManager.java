package forestry.apiimpl.fake;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeListener;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.apiculture.hives.IHive;
import forestry.api.apiculture.hives.IHiveDrop;
import forestry.api.apiculture.hives.IHiveManager;
import forestry.api.apiculture.hives.VillageHive;

/**
 * The hive manager used when the apiculture module is absent. Every registry is empty, so
 * ForestryBiomeModifier adds no hive feature to any biome.
 */
public enum FakeHiveManager implements IHiveManager {
	INSTANCE;

	// Every method on both interfaces is defaulted, so an empty implementation is the null object
	private static final IBeeModifier MODIFIER = new IBeeModifier() {
	};
	private static final IBeeListener LISTENER = new IBeeListener() {
	};

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public List<IHive> getHives() {
		return List.of();
	}

	@Override
	public ImmutableList<VillageHive> getCommonVillageHives() {
		return ImmutableList.of();
	}

	@Override
	public ImmutableList<VillageHive> getRareVillageHives() {
		return ImmutableList.of();
	}

	@Override
	public List<IHiveDrop> getDrops(ResourceLocation id) {
		return List.of();
	}

	@Override
	public float getSwarmingMaterialChance(Item swarmItem) {
		return 0.0f;
	}

	@Override
	public IBeekeepingLogic createBeekeepingLogic(IBeeHousing housing) {
		return FakeBeekeepingLogic.INSTANCE;
	}

	@Override
	public IBeeModifier createBeeHousingModifier(IBeeHousing housing) {
		return MODIFIER;
	}

	@Override
	public IBeeListener createBeeHousingListener(IBeeHousing housing) {
		return LISTENER;
	}
}
