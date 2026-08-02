package forestry.apiculture.bees;
import forestry.apiculture.bees.GuiBeeHousing;
import forestry.apiculture.bees.IGuiBeeHousingDelegate;

public interface IContainerBeeHousing {
	IGuiBeeHousingDelegate getDelegate();

	GuiBeeHousing.Icon getIcon();
}
