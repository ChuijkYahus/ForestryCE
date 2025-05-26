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

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public abstract class AIButterflyMovement extends AIButterflyBase {
	@Nullable
	protected Vec3 flightTarget;

	protected AIButterflyMovement(EntityButterfly entity) {
		super(entity);
	}

	@Override
	public boolean canContinueToUse() {
		if (this.entity.getState() != EnumButterflyState.FLYING) {
			return false;
		}
		if (this.flightTarget == null) {
			return false;
		}
		// Abort if the flight target changed on us.
		if (this.entity.getDestination() == null || !this.entity.getDestination().equals(this.flightTarget)) {
			return false;
		}

		// Continue if we have not yet reached the destination.
		if (this.entity.getDestination().distanceToSqr(this.entity.position()) > 2.0f) {
			return true;
		}

        this.entity.setDestination(null);
		return false;
	}

	@Override
	public void tick() {
		// Reset destination if we did collide.
		if (this.entity.isInWater()) {
            this.flightTarget = getRandomDestinationUpwards();
		} else if (this.entity.horizontalCollision || this.entity.verticalCollision) {
            this.flightTarget = this.entity.getRandom().nextBoolean() ? getRandomDestination() : null;
		} else if (this.entity.level().random.nextInt(300) == 0) {
            this.flightTarget = getRandomDestination();
		}
        this.entity.setDestination(this.flightTarget);
        this.entity.changeExhaustion(1);
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
        this.flightTarget = null;
	}
}
