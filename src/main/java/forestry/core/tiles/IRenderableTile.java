package forestry.core.tiles;

import forestry.core.platform.render.TankRenderInfo;

public interface IRenderableTile {
	TankRenderInfo getResourceTankInfo();

	TankRenderInfo getProductTankInfo();
}
