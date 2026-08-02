package forestry.apiculture.alveary.multiblock;

import forestry.api.core.multiblock.IMultiblockLogicAlveary;
import forestry.core.platform.multiblock.MultiblockController;
import forestry.core.platform.multiblock.MultiblockLogicBase;

public class MultiblockLogicAlveary extends MultiblockLogicBase implements IMultiblockLogicAlveary {
	public MultiblockLogicAlveary() {
	}

	@Override
	public IAlvearyControllerInternal getController() {
		MultiblockController controller = resolveController();
		if (controller instanceof IAlvearyControllerInternal internal && controller.isAssembled()) {
			return internal;
		}
		return FakeAlvearyController.INSTANCE;
	}
}
