package forestry.core.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import net.minecraftforge.common.data.ExistingFileHelper;

import forestry.api.ForestryConstants;

import top.theillusivec4.curios.api.CuriosDataProvider;

public class ForestryCuriosProvider extends CuriosDataProvider {
	public ForestryCuriosProvider(PackOutput output, ExistingFileHelper fileHelper, CompletableFuture<HolderLookup.Provider> registries) {
		super(ForestryConstants.MOD_ID, output, fileHelper, registries);
	}

	@Override
	public void generate(HolderLookup.Provider provider, ExistingFileHelper existingFileHelper) {
		createSlot("head")
			.addCosmetic(true);

		createEntities("slots")
			.addPlayer()
			.addSlots("head");
	}
}
