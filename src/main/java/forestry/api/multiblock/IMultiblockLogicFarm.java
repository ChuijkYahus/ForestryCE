package forestry.api.multiblock;

public interface IMultiblockLogicFarm extends IMultiblockLogic {
	/**
	 * @return the multiblock controller for this logic
	 */
	@Override
	IFarmController getController();
}
