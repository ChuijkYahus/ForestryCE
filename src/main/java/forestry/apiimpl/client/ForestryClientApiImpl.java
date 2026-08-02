package forestry.apiimpl.client;

import forestry.api.client.IForestryClientApi;
import forestry.api.client.ITextureManager;
import forestry.api.client.apiculture.IBeeClientManager;
import forestry.api.client.arboriculture.ITreeClientManager;
import forestry.api.client.genetics.IGeneticClientManager;
import forestry.api.client.lepidopterology.IButterflyClientManager;
import forestry.api.client.plugin.IClientHelper;
import forestry.apiimpl.client.fake.FakeBeeClientManager;
import forestry.apiimpl.client.fake.FakeButterflyClientManager;
import forestry.apiimpl.client.fake.FakeClientHelper;
import forestry.apiimpl.client.fake.FakeTreeClientManager;
import forestry.core.platform.render.ForestryTextureManager;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;

public class ForestryClientApiImpl implements IForestryClientApi {
	// Resolved by service rather than constructed: every IClientHelper method returns an arboriculture
	// type, and ForestryLeafSprites resolves the helper from a static initializer, too early for any
	// lifecycle hook to have installed one. Falls back to the no-op when the arboriculture module is
	// absent.
	private final IClientHelper helper = ServiceLoader.load(IClientHelper.class).findFirst().orElse(FakeClientHelper.INSTANCE);

	@Nullable
	private ITextureManager textureManager;
	@Nullable
	private IGeneticClientManager geneticManager;
	// The three managers whose module can be absent start at their no-op, and the owning module
	// overwrites them. See IForestryModule.installClientManagers
	private IBeeClientManager beeManager = FakeBeeClientManager.INSTANCE;
	private ITreeClientManager treeManager = FakeTreeClientManager.INSTANCE;
	private IButterflyClientManager butterflyManager = FakeButterflyClientManager.INSTANCE;

	@Override
	public ITextureManager getTextureManager() {
		if (this.textureManager == null) {
			throw new IllegalStateException("ITextureManager not initialized yet. Please wait until Minecraft constructor has been called");
		}
		return this.textureManager;
	}

	@Override
	public IGeneticClientManager getGeneticManager() {
		IGeneticClientManager manager = this.geneticManager;
		if (manager == null) {
			throw new IllegalStateException("IGeneticClientManager not initialized yet");
		}
		return manager;
	}

	public IBeeClientManager getBeeManager() {
		return this.beeManager;
	}

	@Override
	public ITreeClientManager getTreeManager() {
		return this.treeManager;
	}

	@Override
	public IButterflyClientManager getButterflyManager() {
		return this.butterflyManager;
	}

	@Override
	public IClientHelper getHelper() {
		return this.helper;
	}

	// Must be called after textureManager is initialized in Minecraft's constructor.
	public void initializeTextureManager(RegisterClientReloadListenersEvent event) {
		this.textureManager = new ForestryTextureManager();
	}

	@ApiStatus.Internal
	public void setGeneticsManager(IGeneticClientManager treeManager) {
		this.geneticManager = treeManager;
	}

	@ApiStatus.Internal
	public void setTreeManager(ITreeClientManager treeManager) {
		this.treeManager = treeManager;
	}

	@ApiStatus.Internal
	public void setButterflyManager(IButterflyClientManager butterflyManager) {
		this.butterflyManager = butterflyManager;
	}

	@ApiStatus.Internal
	public void setBeeManager(IBeeClientManager beeManager) {
		this.beeManager = beeManager;
	}
}
