package forestry.apiculture.genetics.effects;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.IIndividualLiving;
import forestry.api.genetics.alleles.ForestryAlleles;
import forestry.api.genetics.capability.IIndividualHandlerItem;

import net.minecraft.world.level.Level;

import java.util.Random;

public class AgingBeeEffect extends NonStackingBeeEffect {
	boolean aging;
	Random rand = new Random();

	public AgingBeeEffect(boolean dominant, boolean aging) {
		super(dominant);
		this.aging = aging;
	}

	@Override
	protected void doEffectForHive(Level level, IBeeHousing housing) {
		if (!housing.getErrorLogic().hasErrors()) {
			IIndividualHandlerItem handler = IIndividualHandlerItem.get(housing.getBeeInventory().getQueen());
			if (handler != null && handler.getStage() == BeeLifeStage.QUEEN) {
				IIndividual individual = handler.getIndividual();
				if (individual instanceof IIndividualLiving queen) {
					int life = queen.getMaxHealth() / ForestryAlleles.LIFESPAN_NORMAL.value();
					if (rand.nextInt(ForestryAlleles.LIFESPAN_NORMAL.value()) < queen.getMaxHealth() % ForestryAlleles.LIFESPAN_NORMAL.value()) {
						life++;
					}
					if (aging) {
						queen.setHealth(Math.max(1, queen.getHealth() - life));
					} else {
						queen.setHealth((int) Math.min(queen.getMaxHealth(), Math.min(Integer.MAX_VALUE, queen.getHealth() + (long) life)));
					}
					queen.saveToStack(housing.getBeeInventory().getQueen());
				}
			}
		}
	}
}
