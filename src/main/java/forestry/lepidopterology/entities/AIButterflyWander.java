/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.lepidopterology.entities;

import java.util.EnumSet;

public class AIButterflyWander extends AIButterflyMovement {
	public AIButterflyWander(EntityButterfly entity) {
		super(entity);
		setFlags(EnumSet.of(Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (this.entity.getDestination() != null) {
			return false;
		}

        this.flightTarget = getRandomDestination();
		if (this.flightTarget == null) {
			if (this.entity.getState().doesMovement) {
                this.entity.setState(EnumButterflyState.HOVER);
			}
			return false;
		}

        this.entity.setDestination(this.flightTarget);
        this.entity.setState(EnumButterflyState.FLYING);
		return true;
	}
}
