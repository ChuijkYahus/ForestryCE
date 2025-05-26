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
package forestry.apiculture.items;

import forestry.core.items.ItemOverlay;

import java.awt.*;
import java.util.Locale;

public enum EnumHoneyDrop implements ItemOverlay.IOverlayInfo {
	HONEY(new Color(0xecb42d), new Color(0xe8c814));

	private final String name;
	private final int primaryColor;
	private final int secondaryColor;

	EnumHoneyDrop(Color primary, Color secondary) {
		this.name = toString().toLowerCase(Locale.ENGLISH);
		this.primaryColor = primary.getRGB();
		this.secondaryColor = secondary.getRGB();
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public int getPrimaryColor() {
		return this.primaryColor;
	}

	@Override
	public int getSecondaryColor() {
		return this.secondaryColor;
	}
}
